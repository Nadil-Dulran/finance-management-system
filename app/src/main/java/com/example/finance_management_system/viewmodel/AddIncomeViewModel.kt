package com.example.finance_management_system.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.data.AppContainer
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.ui.state.EntryFormUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AddIncomeViewModel(
    private val financeRepository: FinanceRepository = AppContainer.financeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EntryFormUiState(
            primaryField = AppDefaults.DEFAULT_INCOME_SOURCE,
            secondaryField = AppDefaults.DEFAULT_CURRENCY,
            note = "",
            helperText = AppDefaults.INCOME_HELPER_TEXT,
        ),
    )
    val uiState: StateFlow<EntryFormUiState> = _uiState.asStateFlow()

    fun updateSource(value: String) {
        _uiState.update { it.copy(primaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amount = value, errorMessage = null, successMessage = null) }
    }

    fun updateCurrency(value: String) {
        _uiState.update { it.copy(secondaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updateNote(value: String) {
        _uiState.update { it.copy(note = value, errorMessage = null, successMessage = null) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = AppDefaults.ERROR_INVALID_INCOME) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            financeRepository.addIncome(
                sourceType = state.primaryField,
                amount = amount,
                currency = state.secondaryField,
                note = state.note,
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        amount = "",
                        note = "",
                        isSaving = false,
                        successMessage = AppDefaults.SUCCESS_INCOME_SAVED,
                    )
                }
                onSaved()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: AppDefaults.ERROR_INCOME_SAVE,
                    )
                }
            }
        }
    }
}
