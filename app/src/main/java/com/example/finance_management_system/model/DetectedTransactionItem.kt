package com.example.finance_management_system.model

data class DetectedTransactionItem(
    val id: String,
    val title: String,
    val amountLabel: String,
    val merchant: String,
    val detectedType: String,
    val suggestedCategoryOrSource: String,
    val rawText: String,
    val currency: String,
    val amountOriginal: Double,
    val occurredAt: Long,
)