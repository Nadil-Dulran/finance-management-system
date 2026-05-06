package com.example.finance_management_system.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_entries")
data class IncomeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceType: String,
    val amountOriginal: Double,
    val currency: String,
    val exchangeRateToLkr: Double,
    val amountLkr: Double,
    val note: String,
    val receivedAt: Long,
    val createdAt: Long,
)