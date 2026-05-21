package com.example.finance_management_system.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.ui.state.EntryFormUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EntryFormUiState(
            secondaryField = AppDefaults.DEFAULT_CURRENCY,
            tertiaryField = AppDefaults.DEFAULT_SPENDING_TYPE,
            quaternaryField = AppDefaults.DEFAULT_PAYMENT_METHOD,
            quinaryField = "None",
            note = "",
            helperText = AppDefaults.EXPENSE_HELPER_TEXT,
        ),
    )
    val uiState: StateFlow<EntryFormUiState> = _uiState.asStateFlow()

    fun updateCategory(value: String) {
        _uiState.update { it.copy(primaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amount = value, errorMessage = null, successMessage = null) }
    }

    fun updateCurrency(value: String) {
        _uiState.update { it.copy(secondaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updateType(value: String) {
        _uiState.update { it.copy(tertiaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updatePaymentMethod(value: String) {
        _uiState.update { it.copy(quaternaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updateRecurrence(value: String) {
        _uiState.update { it.copy(quinaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updateNote(value: String) {
        _uiState.update { it.copy(note = value, errorMessage = null, successMessage = null) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = AppDefaults.ERROR_INVALID_EXPENSE) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            financeRepository.addExpense(
                category = state.primaryField,
                amount = amount,
                currency = state.secondaryField,
                spendingType = state.tertiaryField,
                recurrenceType = state.quinaryField,
                paymentMethod = state.quaternaryField,
                accountName = AppDefaults.DEFAULT_ACCOUNT_NAME,
                note = state.note,
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        amount = "",
                        note = "",
                        isSaving = false,
                        successMessage = AppDefaults.SUCCESS_EXPENSE_SAVED,
                    )
                }
                onSaved()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: AppDefaults.ERROR_EXPENSE_SAVE,
                    )
                }
            }
        }
    }
}
