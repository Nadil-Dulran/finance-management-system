package com.example.finance_management_system.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_entries")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val spendingType: String,
    val recurrenceType: String,
    val recurrenceGroupId: String? = null,
    val isRecurringTemplate: Boolean = false,
    val originalCurrency: String,
    val originalAmount: Double,
    val amountLkr: Double,
    val paymentMethod: String,
    val accountName: String,
    val note: String,
    val spentAt: Long,
    val createdAt: Long,
)