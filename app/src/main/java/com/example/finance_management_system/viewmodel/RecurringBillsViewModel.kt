package com.example.finance_management_system.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.data.AppContainer
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringBillsViewModel(
    private val financeRepository: FinanceRepository = AppContainer.financeRepository,
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
