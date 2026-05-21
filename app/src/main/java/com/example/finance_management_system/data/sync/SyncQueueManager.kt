package com.example.finance_management_system.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finance_management_system.data.local.dao.ExpenseDao
import com.example.finance_management_system.data.local.dao.GoalDao
import com.example.finance_management_system.data.local.dao.IncomeDao
import com.example.finance_management_system.data.local.dao.SyncQueueDao
import com.example.finance_management_system.data.local.entity.SyncQueueEntity
import com.example.finance_management_system.data.remote.FirestoreSyncService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val incomeDao: IncomeDao,
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao,
    private val syncService: FirestoreSyncService,
    @ApplicationContext private val appContext: Context,
) {
    suspend fun enqueueUpsert(
        userId: String,
        entityType: SyncEntityType,
        entityId: String,
    ) {
        val now = System.currentTimeMillis()
        syncQueueDao.upsert(
            SyncQueueEntity(
                queueKey = queueKey(entityType, entityId),
                userId = userId,
                entityType = entityType.name,
                entityId = entityId,
                action = SyncAction.UPSERT.name,
                attemptCount = 0,
                lastAttemptAt = 0L,
                lastError = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        scheduleImmediateSync()
    }

    suspend fun enqueueDelete(
        userId: String,
        entityType: SyncEntityType,
        entityId: String,
    ) {
        val now = System.currentTimeMillis()
        syncQueueDao.upsert(
            SyncQueueEntity(
                queueKey = queueKey(entityType, entityId),
                userId = userId,
                entityType = entityType.name,
                entityId = entityId,
                action = SyncAction.DELETE.name,
                attemptCount = 0,
                lastAttemptAt = 0L,
                lastError = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        scheduleImmediateSync()
    }

    suspend fun processPendingOperations(): Boolean {
        val pending = syncQueueDao.getAllPending()
        pending.forEach { item ->
            runCatching { process(item) }
                .onSuccess {
                    syncQueueDao.delete(item.queueKey)
                }
                .onFailure { throwable ->
                    syncQueueDao.markFailure(
                        queueKey = item.queueKey,
                        lastAttemptAt = System.currentTimeMillis(),
                        lastError = throwable.message,
                    )
                    scheduleImmediateSync()
                    return false
                }
        }
        return true
    }

    fun scheduleRecurringSync() {
        val workManager = WorkManager.getInstance(appContext)
        val request = PeriodicWorkRequestBuilder<SyncQueueWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syncConstraints())
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleImmediateSync() {
        val workManager = WorkManager.getInstance(appContext)
        val request = OneTimeWorkRequestBuilder<SyncQueueWorker>()
            .setConstraints(syncConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private suspend fun process(item: SyncQueueEntity) {
        val entityType = SyncEntityType.valueOf(item.entityType)
        val action = SyncAction.valueOf(item.action)

        when (action) {
            SyncAction.UPSERT -> processUpsert(item.userId, entityType, item.entityId)
            SyncAction.DELETE -> processDelete(item.userId, entityType, item.entityId)
        }
    }

    private suspend fun processUpsert(
        userId: String,
        entityType: SyncEntityType,
        entityId: String,
    ) {
        when (entityType) {
            SyncEntityType.INCOME -> {
                val entity = incomeDao.getById(entityId, userId) ?: return
                syncService.pushIncome(userId, entity)
            }
            SyncEntityType.EXPENSE -> {
                val entity = expenseDao.getById(entityId, userId) ?: return
                syncService.pushExpense(userId, entity)
            }
            SyncEntityType.GOAL -> {
                val entity = goalDao.getGoalById(entityId, userId) ?: return
                syncService.pushGoal(userId, entity)
            }
        }
    }

    private suspend fun processDelete(
        userId: String,
        entityType: SyncEntityType,
        entityId: String,
    ) {
        when (entityType) {
            SyncEntityType.INCOME -> syncService.deleteIncome(userId, entityId)
            SyncEntityType.EXPENSE -> syncService.deleteExpense(userId, entityId)
            SyncEntityType.GOAL -> syncService.deleteGoal(userId, entityId)
        }
    }

    private fun queueKey(entityType: SyncEntityType, entityId: String): String {
        return "${entityType.name}:$entityId"
    }

    private fun syncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    companion object {
        private const val IMMEDIATE_SYNC_WORK_NAME = "finance_sync_queue_immediate"
        private const val PERIODIC_SYNC_WORK_NAME = "finance_sync_queue_periodic"
    }
}

enum class SyncEntityType {
    INCOME,
    EXPENSE,
    GOAL,
}

private enum class SyncAction {
    UPSERT,
    DELETE,
}
