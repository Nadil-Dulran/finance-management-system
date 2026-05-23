package com.example.finance_management_system.repository.firebase

import android.content.Context
import android.util.Log
import com.example.finance_management_system.data.preferences.UserPreferencesRepository
import com.example.finance_management_system.data.session.AuthSessionManager
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.repository.AuthRepository
import com.example.finance_management_system.repository.AuthUser
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authSessionManager: AuthSessionManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val appContext: Context,
) : AuthRepository {

    private companion object {
        const val TAG = "FirebaseAuthRepository"
        const val AUTH_TIMEOUT_MS = 15_000L
    }

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        return runCatching {
            val normalizedEmail = email.trim()
            val user = try {
                Log.d(TAG, "Starting Firebase SDK email sign in for $normalizedEmail")
                loginWithFirebase(normalizedEmail, password)
            } catch (throwable: Throwable) {
                if (!shouldUseRestFallback(throwable)) {
                    throw throwable
                }
                Log.w(TAG, "Firebase SDK login stalled; falling back to REST sign in", throwable)
                loginWithRest(normalizedEmail, password)
            }
            authSessionManager.setCurrentUser(user)
            runCatching { userPreferencesRepository.saveAuthSession() }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to persist login session", throwable)
                }
            user
        }.recoverCatching { throwable ->
            throw mapAuthException("login", throwable)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
        return runCatching {
            val normalizedName = name.trim()
            val normalizedEmail = email.trim()
            val user = try {
                Log.d(TAG, "Starting Firebase SDK registration for $normalizedEmail")
                registerWithFirebase(normalizedName, normalizedEmail, password)
            } catch (throwable: Throwable) {
                if (!shouldUseRestFallback(throwable)) {
                    throw throwable
                }
                Log.w(TAG, "Firebase SDK registration stalled; falling back to REST sign up", throwable)
                registerWithRest(normalizedName, normalizedEmail, password)
            }
            authSessionManager.setCurrentUser(user)
            runCatching { userPreferencesRepository.saveAuthSession() }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to persist registration session", throwable)
                }
            user
        }.recoverCatching { throwable ->
            throw mapAuthException("register", throwable)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> {
        return runCatching {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = withTimeout(AUTH_TIMEOUT_MS) {
                firebaseAuth.signInWithCredential(credential).awaitTask("loginWithGoogle")
            }
            val user = requireNotNull(result.user) { "User account not available." }
            val authUser = AuthUser(
                uid = user.uid,
                displayName = user.displayName ?: user.email.orEmpty(),
                email = user.email.orEmpty(),
            )
            authSessionManager.setCurrentUser(authUser)
            runCatching { userPreferencesRepository.saveAuthSession() }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to persist Google login session", throwable)
                }
            authUser
        }.recoverCatching { throwable ->
            throw mapAuthException("loginWithGoogle", throwable)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return runCatching {
            val normalizedEmail = email.trim()
            withTimeout(AUTH_TIMEOUT_MS) {
                firebaseAuth
                    .sendPasswordResetEmail(normalizedEmail)
                    .awaitTask("sendPasswordReset")
            }
            Unit
        }.recoverCatching { throwable ->
            throw mapAuthException("sendPasswordReset", throwable)
        }
    }

    private suspend fun loginWithFirebase(email: String, password: String): AuthUser {
        val result = withTimeout(AUTH_TIMEOUT_MS) {
            firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .awaitTask("login")
        }
        val user = requireNotNull(result.user) { "User account not available." }
        Log.d(TAG, "Firebase SDK sign in succeeded for uid=${user.uid}")
        return AuthUser(
            uid = user.uid,
            displayName = user.displayName ?: user.email.orEmpty(),
            email = user.email.orEmpty(),
        )
    }

    private suspend fun registerWithFirebase(name: String, email: String, password: String): AuthUser {
        val result = withTimeout(AUTH_TIMEOUT_MS) {
            firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .awaitTask("register")
        }
        val user = requireNotNull(result.user) { "User account could not be created." }
        withTimeout(AUTH_TIMEOUT_MS) {
            user.updateDisplayName(name)
        }
        Log.d(TAG, "Firebase SDK registration succeeded for uid=${user.uid}")
        return AuthUser(
            uid = user.uid,
            displayName = name,
            email = user.email.orEmpty(),
        )
    }

    private suspend fun loginWithRest(email: String, password: String): AuthUser {
        val response = postIdentityToolkit(
            endpoint = "accounts:signInWithPassword",
            payload = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("returnSecureToken", true),
        )
        return AuthUser(
            uid = response.getString("localId"),
            displayName = response.optString("displayName", email),
            email = response.optString("email", email),
        )
    }

    private suspend fun registerWithRest(name: String, email: String, password: String): AuthUser {
        val signUpResponse = postIdentityToolkit(
            endpoint = "accounts:signUp",
            payload = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("returnSecureToken", true),
        )
        val idToken = signUpResponse.optString("idToken")
        if (idToken.isNotBlank()) {
            postIdentityToolkit(
                endpoint = "accounts:update",
                payload = JSONObject()
                    .put("idToken", idToken)
                    .put("displayName", name)
                    .put("returnSecureToken", true),
            )
        }
        return AuthUser(
            uid = signUpResponse.getString("localId"),
            displayName = name,
            email = signUpResponse.optString("email", email),
        )
    }

    private suspend fun postIdentityToolkit(
        endpoint: String,
        payload: JSONObject,
    ): JSONObject = withContext(Dispatchers.IO) {
        val apiKey = resolveGoogleApiKey()
        val connection = (URL("https://identitytoolkit.googleapis.com/v1/$endpoint?key=$apiKey").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = AUTH_TIMEOUT_MS.toInt()
            readTimeout = AUTH_TIMEOUT_MS.toInt()
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        try {
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(payload.toString())
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            if (connection.responseCode !in 200..299) {
                throw mapRestError(json)
            }

            json
        } catch (exception: IOException) {
            throw IllegalStateException(AppDefaults.ERROR_AUTH_NETWORK, exception)
        } finally {
            connection.disconnect()
        }
    }

    private fun resolveGoogleApiKey(): String {
        val resourceId = appContext.resources.getIdentifier("google_api_key", "string", appContext.packageName)
        check(resourceId != 0) { "Google API key resource not found." }
        return appContext.getString(resourceId)
    }

    private fun mapRestError(payload: JSONObject): Throwable {
        val message = payload.optJSONObject("error")?.optString("message").orEmpty()
        val userMessage = when (message) {
            "INVALID_LOGIN_CREDENTIALS",
            "EMAIL_NOT_FOUND",
            "INVALID_PASSWORD",
            "INVALID_EMAIL" -> AppDefaults.ERROR_AUTH_INVALID_CREDENTIALS
            "EMAIL_EXISTS" -> "This email is already registered. Try signing in instead."
            "WEAK_PASSWORD : Password should be at least 6 characters" -> "Password should be at least 6 characters long."
            "TOO_MANY_ATTEMPTS_TRY_LATER" -> AppDefaults.ERROR_AUTH_TOO_MANY_REQUESTS
            else -> if (message.isBlank()) AppDefaults.ERROR_SIGN_IN else message.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
        }
        return IllegalStateException(userMessage)
    }

    private fun shouldUseRestFallback(throwable: Throwable): Boolean {
        return throwable is TimeoutCancellationException || throwable is FirebaseNetworkException
    }

    private fun mapAuthException(action: String, throwable: Throwable): Throwable {
        val mapped = when (throwable) {
            is TimeoutCancellationException ->
                IllegalStateException(AppDefaults.ERROR_AUTH_TIMEOUT, throwable)
            is FirebaseNetworkException ->
                IllegalStateException(AppDefaults.ERROR_AUTH_NETWORK, throwable)
            is FirebaseTooManyRequestsException ->
                IllegalStateException(AppDefaults.ERROR_AUTH_TOO_MANY_REQUESTS, throwable)
            is FirebaseAuthInvalidCredentialsException ->
                IllegalStateException(AppDefaults.ERROR_AUTH_INVALID_CREDENTIALS, throwable)
            is FirebaseAuthException ->
                IllegalStateException(throwable.localizedMessage ?: throwable.message ?: AppDefaults.ERROR_SIGN_IN, throwable)
            else -> throwable
        }
        Log.e(TAG, "Firebase auth $action failed", mapped)
        return mapped
    }

    private suspend fun FirebaseUser.updateDisplayName(name: String) {
        updateProfile(
            com.google.firebase.auth.userProfileChangeRequest {
                displayName = name
            },
        ).awaitTask("updateProfile")
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(action: String): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) {
                    return@addOnCompleteListener
                }

                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase task callback succeeded for $action")
                    continuation.resume(task.result)
                } else {
                    val exception = task.exception ?: IllegalStateException("Firebase task failed for $action")
                    Log.e(TAG, "Firebase task callback failed for $action", exception)
                    continuation.resumeWithException(exception)
                }
            }

            addOnCanceledListener {
                if (continuation.isActive) {
                    val exception = IllegalStateException("Firebase task was cancelled during $action")
                    Log.e(TAG, "Firebase task callback cancelled for $action", exception)
                    continuation.resumeWithException(exception)
                }
            }
        }
}
