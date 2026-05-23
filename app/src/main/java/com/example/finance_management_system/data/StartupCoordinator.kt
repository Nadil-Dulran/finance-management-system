package com.example.finance_management_system.data

import com.example.finance_management_system.data.currency.ExchangeRateRepository
import com.example.finance_management_system.data.remote.FirestoreSyncService
import com.example.finance_management_system.data.session.AuthSessionManager
import com.example.finance_management_system.data.sync.SyncQueueManager
import com.example.finance_management_system.repository.AuthUser
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class StartupCoordinator @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authSessionManager: AuthSessionManager,
    private val financeRepository: FinanceRepository,
    private val goalRepository: GoalRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val firestoreSyncService: FirestoreSyncService,
    private val syncQueueManager: SyncQueueManager,
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deferredStartupScheduled = AtomicBoolean(false)

    fun ensureAuthSessionInitialized() {
        val current = firebaseAuth.currentUser ?: return
        authSessionManager.setCurrentUser(
            AuthUser(
                uid = current.uid,
                displayName = current.displayName ?: current.email.orEmpty(),
                email = current.email.orEmpty(),
            ),
        )

        applicationScope.launch {
            runCatching { syncQueueManager.processPendingOperations() }
            runCatching { firestoreSyncService.syncUserData(current.uid) }
        }

        syncQueueManager.scheduleImmediateSync()
    }

    fun runDeferredStartupWork() {
        if (!deferredStartupScheduled.compareAndSet(false, true)) return

        applicationScope.launch {
            runCatching { financeRepository.refreshRecurringExpensesIfNeeded() }
            runCatching { goalRepository.refreshGoalContributionsIfNeeded() }
            runCatching { exchangeRateRepository.refreshRatesIfNeeded() }
            runCatching { syncQueueManager.processPendingOperations() }
        }

        syncQueueManager.scheduleRecurringSync()
    }
}
