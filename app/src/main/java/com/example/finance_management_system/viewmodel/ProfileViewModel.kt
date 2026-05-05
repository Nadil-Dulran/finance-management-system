package com.example.finance_management_system.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.data.AppContainer
import com.example.finance_management_system.ui.state.ProfileUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel : ViewModel() {
    private val auth = AppContainer.firebaseAuth
    private val sessionManager = AppContainer.authSessionManager
    private val preferences = AppContainer.userPreferencesRepository
    private val context = AppContainer.appContext

    val uiState: StateFlow<ProfileUiState> = combine(
        preferences.preferredCurrency,
        sessionManager.currentUser,
    ) { currency, user ->
        ProfileUiState(
            displayName = user?.displayName ?: user?.email ?: "User",
            email = user?.email.orEmpty(),
            preferredCurrency = currency,
            notificationCaptureEnabled = isNotificationListenerEnabled(),
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

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabled.contains(context.packageName, ignoreCase = true)
    }
}
