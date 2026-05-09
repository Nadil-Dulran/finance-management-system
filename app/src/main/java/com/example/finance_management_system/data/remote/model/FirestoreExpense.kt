package com.example.finance_management_system.data.remote.model

data class FirestoreExpense(
    val id: String = "",
    val category: String = "",
    val spendingType: String = "",
    val recurrenceType: String = "None",
    val recurrenceGroupId: String? = null,
    val isRecurringTemplate: Boolean = false,
    val originalCurrency: String = "LKR",
    val originalAmount: Double = 0.0,
    val amountLkr: Double = 0.0,
    val paymentMethod: String = "",
    val accountName: String = "",
    val note: String = "",
    val spentAt: Long = 0L,
    val createdAt: Long = 0L,
)
