package com.tetrova_flowledger.ui.state

data class SettingsUiState(
    val preferredCurrency: String = "LKR",
    val isSaving: Boolean = false,
    val message: String? = null,
)