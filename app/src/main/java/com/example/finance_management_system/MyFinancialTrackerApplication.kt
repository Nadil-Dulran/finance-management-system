package com.example.finance_management_system;

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finance_management_system.data.AppContainer
import com.example.finance_management_system.data.notification.BillReminderWorker
import com.example.finance_management_system.data.notification.NotificationHelper
import java.util.concurrent.TimeUnit

class MyFinancialTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
        AppContainer.ensureAuthSessionInitialized()
        
        NotificationHelper.createNotificationChannel(this)
        scheduleBillReminders()
    }

    private fun scheduleBillReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BillReminderWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
