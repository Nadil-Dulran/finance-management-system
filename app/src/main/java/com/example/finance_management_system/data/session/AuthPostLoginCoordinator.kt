package com.example.finance_management_system.data.session

import android.util.Log
import com.example.finance_management_system.data.remote.FirestoreSyncService
import com.example.finance_management_system.data.sync.SyncQueueManager
import com.example.finance_management_system.repository.local.LocalFinanceRepository
import javax.inject.Inject
import javax.inject.Singleton

interface AuthPostLoginCoordinator {
    suspend fun onAuthenticated(uid: String)
}

@Singleton
class DefaultAuthPostLoginCoordinator @Inject constructor(
    private val syncService: FirestoreSyncService,
    private val syncQueueManager: SyncQueueManager,
    private val localFinanceRepository: LocalFinanceRepository,
) : AuthPostLoginCoordinator {
    override suspend fun onAuthenticated(uid: String) {
        try {
            syncQueueManager.processPendingOperations()
        } catch (e: Exception) {
            Log.w(TAG, "Queued sync after auth failed", e)
        }
        try {
            syncService.syncUserData(uid)
        } catch (e: Exception) {
            Log.w(TAG, "Initial sync after auth failed", e)
        }
        syncQueueManager.scheduleImmediateSync()
        try {
            localFinanceRepository.cleanupLegacyDemoData()
        } catch (e: Exception) {
            Log.w(TAG, "Legacy data cleanup after auth failed", e)
        }
    }

    private companion object {
        const val TAG = "AuthPostLoginCoordinator"
    }
}
