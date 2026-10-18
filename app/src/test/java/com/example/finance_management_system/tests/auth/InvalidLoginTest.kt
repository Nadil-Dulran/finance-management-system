package com.tetrova_flowledger.tests.auth

import com.tetrova_flowledger.data.session.AuthPostLoginCoordinator
import com.tetrova_flowledger.model.AppDefaults
import com.tetrova_flowledger.repository.AuthRepository
import com.tetrova_flowledger.repository.AuthUser
import com.tetrova_flowledger.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvalidLoginTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_withWrongPassword_showsErrorAndDoesNotCrash() = runTest {
        val fakeRepository = InvalidLoginAuthRepository(
            loginResult = Result.failure(IllegalStateException(AppDefaults.ERROR_AUTH_INVALID_CREDENTIALS)),
        )
        val fakeCoordinator = InvalidLoginAuthPostLoginCoordinator()
        val viewModel = AuthViewModel(
            repository = fakeRepository,
            authPostLoginCoordinator = fakeCoordinator,
        )
        var onSuccessCalled = false

        viewModel.updateEmail("testuser01@example.com")
        viewModel.updatePassword("WrongPassword123")

        viewModel.login {
            onSuccessCalled = true
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("testuser01@example.com", fakeRepository.lastEmail)
        assertEquals("WrongPassword123", fakeRepository.lastPassword)
        assertNull(fakeCoordinator.lastAuthenticatedUid)
        assertFalse(onSuccessCalled)
        assertFalse(state.isLoading)
        assertEquals(AppDefaults.ERROR_AUTH_INVALID_CREDENTIALS, state.errorMessage)
        assertNull(state.successMessage)
    }
}

private class InvalidLoginAuthRepository(
    private val loginResult: Result<AuthUser>,
) : AuthRepository {
    var lastEmail: String? = null
    var lastPassword: String? = null

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        lastEmail = email
        lastPassword = password
        return loginResult
    }

    override suspend fun register(name: String, email: String, password: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthUser> {
        error("Not used in this test")
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        error("Not used in this test")
    }
}

private class InvalidLoginAuthPostLoginCoordinator : AuthPostLoginCoordinator {
    var lastAuthenticatedUid: String? = null

    override suspend fun onAuthenticated(uid: String) {
        lastAuthenticatedUid = uid
    }
}