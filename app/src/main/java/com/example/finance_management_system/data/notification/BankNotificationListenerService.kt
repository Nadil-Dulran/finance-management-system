package com.example.finance_management_system.data.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.finance_management_system.repository.FinanceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {
    @Inject
    lateinit var financeRepository: FinanceRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        serviceScope.launch {
            financeRepository.ingestDetectedTransaction(
                packageName = sbn.packageName.orEmpty(),
                title = title,
                body = text,
                postedAt = sbn.postTime,
            )
        }
    }
}
