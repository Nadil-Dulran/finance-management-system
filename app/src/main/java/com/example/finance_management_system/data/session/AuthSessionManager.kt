package com.example.finance_management_system.data.session

import com.example.finance_management_system.repository.AuthUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AuthSessionManager @Inject constructor() {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    val currentUserValue: AuthUser?
        get() = _currentUser.value

    fun setCurrentUser(user: AuthUser) {
        _currentUser.value = user
    }

    fun clear() {
        _currentUser.value = null
    }
}
