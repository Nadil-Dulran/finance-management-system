package com.example.finance_management_system.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.data.AppContainer
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.repository.AuthRepository
import com.example.finance_management_system.repository.local.LocalFinanceRepository
import com.example.finance_management_system.ui.state.AuthUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel(
    private val repository: AuthRepository = AppContainer.authRepository,
) : ViewModel() {
    private companion object {
        const val TAG = "AuthViewModel"
    }

    private val syncService = AppContainer.firestoreSyncService
    private val localFinanceRepository = AppContainer.financeRepository as? LocalFinanceRepository
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null, successMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, successMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, successMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            repository.login(state.email, state.password)
                .onSuccess { user ->
                    handleAuthSuccess(user.uid, onSuccess)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: AppDefaults.ERROR_SIGN_IN,
                            successMessage = null,
                        )
                    }
                }
        }
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            repository.register(state.name, state.email, state.password)
                .onSuccess { user ->
                    handleAuthSuccess(user.uid, onSuccess)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: AppDefaults.ERROR_REGISTER,
                            successMessage = null,
                        )
                    }
                }
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            repository.loginWithGoogle(idToken)
                .onSuccess { user ->
                    handleAuthSuccess(user.uid, onSuccess)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: AppDefaults.ERROR_SIGN_IN,
                            successMessage = null,
                        )
                    }
                }
        }
    }

    private suspend fun handleAuthSuccess(uid: String, onSuccess: () -> Unit) {
        try {
            syncService.syncUserData(uid)
        } catch (e: Exception) {
            Log.w(TAG, "Initial sync after auth failed", e)
        }
        try {
            localFinanceRepository?.cleanupLegacyDemoData()
        } catch (e: Exception) {
            Log.w(TAG, "Legacy data cleanup after auth failed", e)
        }
        _uiState.update { current -> current.copy(isLoading = false) }
        onSuccess()
    }

    fun sendPasswordReset() {
        viewModelScope.launch {
            val email = _uiState.value.email.trim()
            if (email.isBlank()) {
                _uiState.update {
                    it.copy(
                        errorMessage = AppDefaults.ERROR_RESET_PASSWORD_EMAIL_REQUIRED,
                        successMessage = null,
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            repository.sendPasswordReset(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = AppDefaults.SUCCESS_RESET_PASSWORD,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: AppDefaults.ERROR_RESET_PASSWORD,
                            successMessage = null,
                        )
                    }
                }
        }
    }
}
