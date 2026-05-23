package com.example.finance_management_system.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.data.currency.CurrencyConverter
import com.example.finance_management_system.data.currency.ExchangeRateRepository
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.ui.state.EntryFormUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
) : ViewModel() {
    private var availableFreeCashLkr = 0.0
    private var amountTouched = false
    private var currentRatesToLkr: Map<String, Double> = emptyMap()

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

    init {
        viewModelScope.launch {
            financeRepository.observeAvailableFreeCash().collectLatest { availableFreeCash ->
                availableFreeCashLkr = availableFreeCash
                refreshAmountValidation()
            }
        }

        viewModelScope.launch {
            exchangeRateRepository.ratesToLkr.collectLatest { rates ->
                currentRatesToLkr = rates
                refreshAmountValidation()
            }
        }
    }

    fun updateCategory(value: String) {
        _uiState.update { it.copy(primaryField = value, errorMessage = null, successMessage = null) }
    }

    fun updateAmount(value: String) {
        amountTouched = true
        _uiState.update { it.copy(amount = value, errorMessage = null, successMessage = null) }
        refreshAmountValidation()
    }

    fun updateCurrency(value: String) {
        _uiState.update { it.copy(secondaryField = value, errorMessage = null, successMessage = null) }
        refreshAmountValidation()
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
        amountTouched = true
        val state = _uiState.value
        val validation = validateAmount(state.amount, state.secondaryField, showBlankError = true)
        if (!validation.first) {
            _uiState.update {
                it.copy(
                    amountValidationMessage = validation.second,
                    isAmountValid = false,
                    errorMessage = null,
                    successMessage = null,
                )
            }
            return
        }
        val amount = state.amount.trim().toDoubleOrNull() ?: return

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
                        isAmountValid = false,
                        amountValidationMessage = null,
                        successMessage = AppDefaults.SUCCESS_EXPENSE_SAVED,
                    )
                }
                amountTouched = false
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

    private fun refreshAmountValidation() {
        val state = _uiState.value
        val validation = validateAmount(state.amount, state.secondaryField, showBlankError = amountTouched)
        _uiState.update {
            it.copy(
                amountValidationMessage = if (amountTouched) validation.second else null,
                isAmountValid = amountTouched && validation.first,
            )
        }
    }

    private fun validateAmount(
        amountText: String,
        currency: String,
        showBlankError: Boolean,
    ): Pair<Boolean, String?> {
        val trimmedAmount = amountText.trim()
        if (trimmedAmount.isBlank()) {
            return false to if (showBlankError) AppDefaults.ERROR_INVALID_EXPENSE else null
        }

        val amount = trimmedAmount.toDoubleOrNull() ?: return false to AppDefaults.ERROR_INVALID_EXPENSE
        if (amount <= 0.0) {
            return false to AppDefaults.ERROR_INVALID_EXPENSE
        }

        val rateToLkr = currentRatesToLkr[currency.uppercase()] ?: return false to AppDefaults.ERROR_INVALID_EXPENSE
        val amountLkr = CurrencyConverter.toLkr(amount, currency, rateToLkr)
        return if (amountLkr - availableFreeCashLkr > 0.000001) {
            false to AppDefaults.ERROR_EXPENSE_EXCEEDS_FREE_CASH
        } else {
            true to null
        }
    }
}
