package com.example.finance_management_system.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecurringBillsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    val recurringBills: StateFlow<List<TransactionItem>> = financeRepository
        .observeRecurringExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun updateSubscription(bill: TransactionItem) {
        viewModelScope.launch {
            financeRepository.updateTransaction(bill)
        }
    }

    fun deleteSubscription(bill: TransactionItem) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(bill)
        }
    }
}
