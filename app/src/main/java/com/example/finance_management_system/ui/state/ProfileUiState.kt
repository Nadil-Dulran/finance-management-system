package com.example.finance_management_system.ui.state

data class ProfileUiState(
    val displayName: String = "User",
    val email: String = "",
    val preferredCurrency: String = "LKR",
    val notificationCaptureEnabled: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val message: String? = null,
)
