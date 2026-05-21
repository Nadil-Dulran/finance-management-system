package com.example.finance_management_system.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.data.AppContainer
import com.example.finance_management_system.ui.state.ProfileUiState
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val auth = AppContainer.firebaseAuth
    private val sessionManager = AppContainer.authSessionManager
    private val preferences = AppContainer.userPreferencesRepository
    private val context = AppContainer.appContext
    private val syncService = AppContainer.firestoreSyncService
    private val database = AppContainer.database
    private val statusState = MutableStateFlow(ProfileUiState())

    val uiState: StateFlow<ProfileUiState> = combine(
        preferences.preferredCurrency,
        sessionManager.currentUser,
        statusState,
    ) { currency, user, status ->
        ProfileUiState(
            displayName = user?.displayName ?: user?.email ?: "User",
            email = user?.email.orEmpty(),
            preferredCurrency = currency,
            notificationCaptureEnabled = isNotificationListenerEnabled(),
            isDeletingAccount = status.isDeletingAccount,
            message = status.message,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(),
        )

    fun signOut() {
        auth.signOut()
        sessionManager.clear()
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val firebaseUser = auth.currentUser ?: return
        val userId = firebaseUser.uid

        viewModelScope.launch {
            statusState.update { it.copy(isDeletingAccount = true, message = null) }

            runCatching {
                runCatching { syncService.deleteUserData(userId) }
                runCatching { database.incomeDao().deleteAllForUser(userId) }
                runCatching { database.expenseDao().deleteAllForUser(userId) }
                runCatching { database.goalDao().deleteAllForUser(userId) }
                runCatching { database.detectedTransactionDao().deleteAllForUser(userId) }
                firebaseUser.delete().await()
            }.onSuccess {
                auth.signOut()
                sessionManager.clear()
                statusState.update { it.copy(isDeletingAccount = false, message = null) }
                onSuccess()
            }.onFailure { throwable ->
                val message = if (throwable is FirebaseAuthRecentLoginRequiredException) {
                    "Please sign in again before deleting your account."
                } else {
                    throwable.message ?: "Could not delete account."
                }
                statusState.update { it.copy(isDeletingAccount = false, message = message) }
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabled.contains(context.packageName, ignoreCase = true)
    }
}
