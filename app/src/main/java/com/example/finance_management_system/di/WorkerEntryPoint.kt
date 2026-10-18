package com.tetrova_flowledger.di

import com.tetrova_flowledger.data.sync.SyncQueueManager
import com.tetrova_flowledger.repository.FinanceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun financeRepository(): FinanceRepository
    fun syncQueueManager(): SyncQueueManager
}
