package com.example.finance_management_system.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finance_management_system.di.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors

class SyncQueueWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
            val completed = entryPoint.syncQueueManager().processPendingOperations()
            if (completed) {
                Result.success()
            } else {
                Result.retry()
            }
        }.getOrElse {
            Result.retry()
        }
    }
}
