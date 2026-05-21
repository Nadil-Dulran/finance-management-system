package com.example.finance_management_system.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_transactions")
data class DetectedTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val packageName: String,
    val title: String,
    val rawText: String,
    val detectedType: String,
    val merchant: String,
    val suggestedCategoryOrSource: String,
    val currency: String,
    val amountOriginal: Double,
    val amountLkr: Double,
    val occurredAt: Long,
    val status: String,
    val createdAt: Long,
)