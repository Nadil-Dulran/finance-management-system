package com.example.finance_management_system.ui.state

data class SettingsUiState(
    val preferredCurrency: String = "LKR",
    val isSaving: Boolean = false,
    val message: String? = null,
)