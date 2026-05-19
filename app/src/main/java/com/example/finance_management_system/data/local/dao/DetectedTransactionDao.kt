package com.example.finance_management_system.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finance_management_system.data.local.entity.DetectedTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectedTransactionDao {
    @Query("SELECT * FROM detected_transactions WHERE userId = :userId AND status = 'PENDING' ORDER BY occurredAt DESC")
    fun observePending(userId: String): Flow<List<DetectedTransactionEntity>>

    @Query("SELECT * FROM detected_transactions WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): DetectedTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DetectedTransactionEntity)

    @Query("DELETE FROM detected_transactions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
