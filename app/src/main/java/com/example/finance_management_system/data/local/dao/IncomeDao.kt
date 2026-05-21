package com.example.finance_management_system.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finance_management_system.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income_entries WHERE userId = :userId ORDER BY receivedAt DESC")
    fun observeAll(userId: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM income_entries WHERE userId = :userId ORDER BY receivedAt DESC")
    suspend fun getAll(userId: String): List<IncomeEntity>

    @Query("SELECT * FROM income_entries WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): IncomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<IncomeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: IncomeEntity)

    @Delete
    suspend fun delete(item: IncomeEntity)

    @Query("DELETE FROM income_entries WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
