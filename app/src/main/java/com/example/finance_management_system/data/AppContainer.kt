package com.example.finance_management_system.data

import android.app.Application
import android.content.Context
import com.example.finance_management_system.data.local.FinanceDatabase
import com.example.finance_management_system.data.preferences.UserPreferencesRepository
import com.example.finance_management_system.data.currency.ExchangeRateRepository
import com.example.finance_management_system.data.remote.FirestoreSyncService
import com.example.finance_management_system.data.session.AuthSessionManager
import com.example.finance_management_system.data.storage.FileStorageManager
import com.example.finance_management_system.repository.AuthRepository
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
import com.example.finance_management_system.repository.firebase.FirebaseAuthRepository
import com.example.finance_management_system.repository.local.LocalFinanceRepository
import com.example.finance_management_system.repository.local.LocalGoalRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.finance_management_system.repository.AuthUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.atomic.AtomicBoolean

object AppContainer {
    lateinit var appContext: Context
        private set
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deferredStartupScheduled = AtomicBoolean(false)

    fun initialize(application: Application) {
        appContext = application.applicationContext
    }

    // Ensure that if Firebase already has an authenticated user (app restart),
    // we populate the in-app `AuthSessionManager` and trigger an initial sync.
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
            runCatching { firestoreSyncService.syncUserData(current.uid) }
        }
    }

    fun runDeferredStartupWork() {
        if (!deferredStartupScheduled.compareAndSet(false, true)) return

        applicationScope.launch {
            runCatching { financeRepository.refreshRecurringExpensesIfNeeded() }
            runCatching { goalRepository.refreshGoalContributionsIfNeeded() }
            runCatching { exchangeRateRepository.refreshRatesIfNeeded() }
        }
    }

    val authSessionManager: AuthSessionManager by lazy {
        AuthSessionManager()
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(
            firebaseAuth = firebaseAuth,
            authSessionManager = authSessionManager,
            appContext = appContext,
        )
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val database: FinanceDatabase by lazy {
        FinanceDatabase.getInstance(appContext)
    }

    val financeRepository: FinanceRepository by lazy {
        LocalFinanceRepository(
            incomeDao = database.incomeDao(),
            expenseDao = database.expenseDao(),
            goalDao = database.goalDao(),
            detectedTransactionDao = database.detectedTransactionDao(),
            userPreferencesRepository = userPreferencesRepository,
            exchangeRateRepository = exchangeRateRepository,
            authSessionManager = authSessionManager,
            syncService = firestoreSyncService,
        )
    }

    val goalRepository: GoalRepository by lazy {
        LocalGoalRepository(
            goalDao = database.goalDao(),
            userPreferencesRepository = userPreferencesRepository,
            exchangeRateRepository = exchangeRateRepository,
            authSessionManager = authSessionManager,
            syncService = firestoreSyncService,
        )
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val exchangeRateRepository: ExchangeRateRepository by lazy {
        ExchangeRateRepository(userPreferencesRepository)
    }

    val fileStorageManager: FileStorageManager by lazy {
        FileStorageManager(appContext)
    }

    val firestoreSyncService: FirestoreSyncService by lazy {
        FirestoreSyncService(
            firestore = firestore,
            incomeDao = database.incomeDao(),
            expenseDao = database.expenseDao(),
            goalDao = database.goalDao(),
        )
    }
}
