package com.tetrova_flowledger.di

import android.content.Context
import com.tetrova_flowledger.data.local.FinanceDatabase
import com.tetrova_flowledger.data.local.dao.DetectedTransactionDao
import com.tetrova_flowledger.data.local.dao.ExpenseDao
import com.tetrova_flowledger.data.local.dao.GoalDao
import com.tetrova_flowledger.data.local.dao.IncomeDao
import com.tetrova_flowledger.data.local.dao.SyncQueueDao
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
