package com.example.finance_management_system.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.finance_management_system.data.local.dao.DetectedTransactionDao
import com.example.finance_management_system.data.local.dao.ExpenseDao
import com.example.finance_management_system.data.local.dao.GoalDao
import com.example.finance_management_system.data.local.dao.IncomeDao
import com.example.finance_management_system.data.local.entity.DetectedTransactionEntity
import com.example.finance_management_system.data.local.entity.ExpenseEntity
import com.example.finance_management_system.data.local.entity.GoalEntity
import com.example.finance_management_system.data.local.entity.IncomeEntity

@Database(
    entities = [IncomeEntity::class, ExpenseEntity::class, GoalEntity::class, DetectedTransactionEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun detectedTransactionDao(): DetectedTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tracker.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
