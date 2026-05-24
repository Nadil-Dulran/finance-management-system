package com.example.finance_management_system.data

import com.example.finance_management_system.data.currency.ExchangeRateRepository
import com.example.finance_management_system.data.remote.FirestoreSyncService
import com.example.finance_management_system.data.preferences.UserPreferencesRepository
import com.example.finance_management_system.data.session.AuthSessionManager
import com.example.finance_management_system.data.sync.SyncQueueManager
import com.example.finance_management_system.repository.AuthUser
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.finance_management_system.navigation.AppDestination
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val financeRepository: FinanceRepository,
    private val goalRepository: GoalRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val firestoreSyncService: FirestoreSyncService,
    private val syncQueueManager: SyncQueueManager,
) {
    private companion object {
        const val SESSION_WINDOW_MS = 3 * 24 * 60 * 60 * 1000L
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deferredStartupScheduled = AtomicBoolean(false)

    fun ensureAuthSessionInitialized() {
        applicationScope.launch {
            val user = restoreSessionIfValid() ?: return@launch

            runCatching { syncQueueManager.processPendingOperations() }
            runCatching { firestoreSyncService.syncUserData(user.uid) }

            syncQueueManager.scheduleImmediateSync()
        }
    }

    suspend fun shouldAutoLogin(): Boolean {
        return restoreSessionIfValid() != null
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

    private suspend fun restoreSessionIfValid(): AuthUser? {
        val current = firebaseAuth.currentUser ?: run {
            authSessionManager.clear()
            return null
        }

        val sessionState = userPreferencesRepository.getAuthSessionState()
        val now = System.currentTimeMillis()
        val isSessionValid = sessionState.isLoggedIn &&
            !sessionState.userLoggedOut &&
            sessionState.lastLoginTime > 0L &&
            now - sessionState.lastLoginTime <= SESSION_WINDOW_MS

        if (!isSessionValid) {
            if (!sessionState.userLoggedOut) {
                runCatching { userPreferencesRepository.clearAuthSession() }
            }
            runCatching { firebaseAuth.signOut() }
            authSessionManager.clear()
            return null
        }

        val authUser = AuthUser(
            uid = current.uid,
            displayName = current.displayName ?: current.email.orEmpty(),
            email = current.email.orEmpty(),
        )
        authSessionManager.setCurrentUser(authUser)
        return authUser
    }
}
