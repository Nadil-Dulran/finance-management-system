package com.example.finance_management_system.di

import com.example.finance_management_system.data.sync.SyncQueueManager
import com.example.finance_management_system.repository.FinanceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun financeRepository(): FinanceRepository
    fun syncQueueManager(): SyncQueueManager
}
