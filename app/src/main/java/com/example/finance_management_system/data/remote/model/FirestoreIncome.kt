package com.example.finance_management_system.data.remote.model

data class FirestoreIncome(
    val id: String = "",
    val sourceType: String = "",
    val amountOriginal: Double = 0.0,
    val currency: String = "LKR",
    val exchangeRateToLkr: Double = 1.0,
    val amountLkr: Double = 0.0,
    val note: String = "",
    val receivedAt: Long = 0L,
    val createdAt: Long = 0L,
)
