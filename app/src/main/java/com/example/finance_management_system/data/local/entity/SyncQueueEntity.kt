package com.example.finance_management_system.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val queueKey: String,
    val userId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val attemptCount: Int,
    val lastAttemptAt: Long,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
