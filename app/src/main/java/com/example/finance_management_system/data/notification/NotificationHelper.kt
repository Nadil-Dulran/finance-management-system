package com.example.finance_management_system.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


object NotificationHelper {
    private const val CHANNEL_ID = "bill_reminders"
    private const val CHANNEL_NAME = "Bill Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for upcoming recurring bills"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBillReminder(context: Context, billTitle: String, amountLabel: String, dueDateLabel: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(androidx.core.R.drawable.notification_bg) // Placeholder, should use app icon
            .setContentTitle("Bill Due Soon")
            .setContentText("$billTitle ($amountLabel) is due on $dueDateLabel")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(billTitle.hashCode(), builder.build())
        }
    }
}
