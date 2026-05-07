package com.example.finance_management_system.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finance_management_system.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_entries WHERE userId = :userId ORDER BY spentAt DESC")
    fun observeAll(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_entries WHERE userId = :userId ORDER BY spentAt DESC")
    suspend fun getAll(userId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expense_entries WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExpenseEntity)

    @Delete
    suspend fun delete(item: ExpenseEntity)
}