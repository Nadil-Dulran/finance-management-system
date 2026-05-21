package com.example.finance_management_system.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finance_management_system.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY updatedAt ASC")
    suspend fun getAllPending(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueKey = :queueKey")
    suspend fun delete(queueKey: String)

    @Query(
        """
        UPDATE sync_queue
        SET attemptCount = attemptCount + 1,
            lastAttemptAt = :lastAttemptAt,
            lastError = :lastError,
            updatedAt = :lastAttemptAt
        WHERE queueKey = :queueKey
        """,
    )
    suspend fun markFailure(
        queueKey: String,
        lastAttemptAt: Long,
        lastError: String?,
    )
}
