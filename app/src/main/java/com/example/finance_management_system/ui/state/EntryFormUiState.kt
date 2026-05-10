package com.example.finance_management_system.ui.state

data class EntryFormUiState(
    val primaryField: String = "",
    val amount: String = "",
    val secondaryField: String = "",
    val tertiaryField: String = "",
    val quaternaryField: String = "",
    val quinaryField: String = "",
    val note: String = "",
    val helperText: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
