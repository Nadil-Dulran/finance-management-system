package com.example.finance_management_system.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finance_management_system.data.AppContainer

class BillReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            AppContainer.financeRepository.checkAndNotifyUpcomingBills()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
