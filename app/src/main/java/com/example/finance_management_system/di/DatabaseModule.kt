package com.example.finance_management_system.di

import android.content.Context
import com.example.finance_management_system.data.local.FinanceDatabase
import com.example.finance_management_system.data.local.dao.DetectedTransactionDao
import com.example.finance_management_system.data.local.dao.ExpenseDao
import com.example.finance_management_system.data.local.dao.GoalDao
import com.example.finance_management_system.data.local.dao.IncomeDao
import com.example.finance_management_system.data.local.dao.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideFinanceDatabase(
        @ApplicationContext context: Context,
    ): FinanceDatabase = FinanceDatabase.getInstance(context)

    @Provides
    fun provideIncomeDao(database: FinanceDatabase): IncomeDao = database.incomeDao()

    @Provides
    fun provideExpenseDao(database: FinanceDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideGoalDao(database: FinanceDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideDetectedTransactionDao(database: FinanceDatabase): DetectedTransactionDao =
        database.detectedTransactionDao()

    @Provides
    fun provideSyncQueueDao(database: FinanceDatabase): SyncQueueDao = database.syncQueueDao()
}
