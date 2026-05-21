package com.example.finance_management_system.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.finance_management_system.data.local.dao.DetectedTransactionDao
import com.example.finance_management_system.data.local.dao.ExpenseDao
import com.example.finance_management_system.data.local.dao.GoalDao
import com.example.finance_management_system.data.local.dao.IncomeDao
import com.example.finance_management_system.data.local.dao.SyncQueueDao
import com.example.finance_management_system.data.local.entity.DetectedTransactionEntity
import com.example.finance_management_system.data.local.entity.ExpenseEntity
import com.example.finance_management_system.data.local.entity.GoalEntity
import com.example.finance_management_system.data.local.entity.IncomeEntity
import com.example.finance_management_system.data.local.entity.SyncQueueEntity

@Database(
    entities = [IncomeEntity::class, ExpenseEntity::class, GoalEntity::class, DetectedTransactionEntity::class, SyncQueueEntity::class],
    version = 7,
    exportSchema = false,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun detectedTransactionDao(): DetectedTransactionDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tracker.db",
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration(false)
                    .build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        queueKey TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        action TEXT NOT NULL,
                        attemptCount INTEGER NOT NULL,
                        lastAttemptAt INTEGER NOT NULL,
                        lastError TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
