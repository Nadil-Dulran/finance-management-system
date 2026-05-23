package com.example.finance_management_system.tests.offline

import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.model.ChartDatum
import com.example.finance_management_system.model.DetectedTransactionItem
import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.model.SummaryCard
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.model.TransactionType
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
import com.example.finance_management_system.viewmodel.AddIncomeViewModel
import com.example.finance_management_system.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSyncRecoveryTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tc17_offlineIncomeSyncRecovery_marksPendingEntrySyncedAndClearsQueue() = runTest {
        val fakeFinanceRepository = FakeSyncableFinanceRepository()
        val fakeGoalRepository = FakeGoalRepository()
        val addIncomeViewModel = AddIncomeViewModel(fakeFinanceRepository)
        val dashboardViewModel = DashboardViewModel(fakeFinanceRepository, fakeGoalRepository)

        backgroundScope.launch {
            dashboardViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        fakeFinanceRepository.networkAvailable = false

        addIncomeViewModel.updateSource("Freelance")
        addIncomeViewModel.updateAmount("40000")
        addIncomeViewModel.updateCurrency("LKR")
        addIncomeViewModel.updateNote("Offline web design payment")

        var savedCalled = false
        addIncomeViewModel.save { savedCalled = true }
        advanceUntilIdle()

        assertTrue(savedCalled)
        assertEquals(1, fakeFinanceRepository.localRecords.size)
        assertFalse(fakeFinanceRepository.localRecords.first().isSynced)
        assertEquals(1, fakeFinanceRepository.pendingQueue.value.size)
        assertEquals("Income saved successfully.", addIncomeViewModel.uiState.value.successMessage)
        assertEquals("Freelance", dashboardViewModel.uiState.value.recentTransactions.first().title)

        fakeFinanceRepository.networkAvailable = true
        val syncSucceeded = fakeFinanceRepository.syncPendingOperations()
        advanceUntilIdle()

        assertTrue(syncSucceeded)
        assertTrue(fakeFinanceRepository.syncCalled)
        assertEquals(0, fakeFinanceRepository.pendingQueue.value.size)
        assertTrue(fakeFinanceRepository.localRecords.first().isSynced)
        assertEquals(1, fakeFinanceRepository.localRecords.size)
    }

    private class FakeSyncableFinanceRepository : FinanceRepository {
        val summaryFlow = MutableStateFlow(initialSummary())
        val incomeChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val expenseChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val recentTransactionsFlow = MutableStateFlow<List<TransactionItem>>(emptyList())
        val pendingQueue = MutableStateFlow<List<PendingSyncItem>>(emptyList())
        val localRecords = mutableListOf<LocalIncomeRecord>()

        var networkAvailable = true
        var syncCalled = false

        data class LocalIncomeRecord(
            val id: String,
            val sourceType: String,
            val amount: Double,
            val currency: String,
            val note: String,
            var isSynced: Boolean,
        )

        data class PendingSyncItem(
            val queueKey: String,
            val entityId: String,
            val action: String,
        )

        override fun observeDashboardSummary(): Flow<List<SummaryCard>> = summaryFlow

        override fun observeExpenseChart(): Flow<List<ChartDatum>> = expenseChartFlow

        override fun observeIncomeChart(): Flow<List<ChartDatum>> = incomeChartFlow

        override fun observeRecentTransactions(): Flow<List<TransactionItem>> = recentTransactionsFlow

        override fun observeRecurringExpenses(): Flow<List<TransactionItem>> = flowOf(emptyList())

        override fun observeDetectedTransactions(): Flow<List<DetectedTransactionItem>> = flowOf(emptyList())

        override suspend fun seedDemoDataIfNeeded() {}

        override suspend fun addIncome(
            sourceType: String,
            amount: Double,
            currency: String,
            note: String,
        ): Result<Unit> {
            val record = LocalIncomeRecord(
                id = "income-${localRecords.size + 1}",
                sourceType = sourceType,
                amount = amount,
                currency = currency,
                note = note,
                isSynced = networkAvailable,
            )
            localRecords += record
            pendingQueue.value = if (record.isSynced) {
                pendingQueue.value
            } else {
                pendingQueue.value + PendingSyncItem(
                    queueKey = "income:${record.id}",
                    entityId = record.id,
                    action = "UPSERT",
                )
            }
            rebuildFlows()
            return Result.success(Unit)
        }

        override suspend fun addExpense(
            category: String,
            amount: Double,
            currency: String,
            spendingType: String,
            recurrenceType: String,
            paymentMethod: String,
            accountName: String,
            note: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun refreshRecurringExpensesIfNeeded() {}

        override suspend fun ingestDetectedTransaction(
            packageName: String,
            title: String,
            body: String,
            postedAt: Long,
        ) {}

        override suspend fun confirmDetectedTransaction(
            id: String,
            chosenType: String,
            chosenCategoryOrSource: String,
            chosenSpendingType: String,
            note: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun ignoreDetectedTransaction(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)

        override suspend fun deleteTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)

        override suspend fun checkAndNotifyUpcomingBills() {}

        suspend fun syncPendingOperations(): Boolean {
            syncCalled = true
            if (!networkAvailable) {
                return false
            }

            val pending = pendingQueue.value
            pending.forEach { pendingItem ->
                localRecords.find { it.id == pendingItem.entityId }?.isSynced = true
            }
            pendingQueue.value = emptyList()
            rebuildFlows()
            return true
        }

        private fun rebuildFlows() {
            val totalIncome = localRecords.sumOf { it.amount }
            summaryFlow.value = listOf(
                SummaryCard(
                    title = "Income recorded",
                    amountLabel = formatLkr(totalIncome),
                    description = "All income added to this account",
                ),
                SummaryCard(
                    title = "Expenses recorded",
                    amountLabel = formatLkr(0.0),
                    description = "Spent so far across all expense entries",
                ),
                SummaryCard(
                    title = "Estimated free cash",
                    amountLabel = formatLkr(totalIncome),
                    description = "Remaining amount after expenses and goal reserves",
                ),
            )

            recentTransactionsFlow.value = localRecords.map { record ->
                TransactionItem(
                    id = record.id,
                    type = TransactionType.INCOME,
                    title = record.sourceType,
                    amountLabel = "+ ${formatLkr(record.amount)}",
                    meta = if (record.isSynced) "Income · synced" else "Income · pending sync",
                    originalAmount = record.amount,
                    originalCurrency = record.currency,
                    note = record.note,
                )
            }
        }

        private fun initialSummary(): List<SummaryCard> = listOf(
            SummaryCard(
                title = "Income recorded",
                amountLabel = formatLkr(0.0),
                description = "All income added to this account",
            ),
            SummaryCard(
                title = "Expenses recorded",
                amountLabel = formatLkr(0.0),
                description = "Spent so far across all expense entries",
            ),
            SummaryCard(
                title = "Estimated free cash",
                amountLabel = formatLkr(0.0),
                description = "Remaining amount after expenses and goal reserves",
            ),
        )

        private fun formatLkr(value: Double): String = "LKR ${String.format("%,.2f", value)}"
    }

    private class FakeGoalRepository : GoalRepository {
        private val primaryGoalFlow = MutableStateFlow<GoalOverview?>(null)
        private val goalsFlow = MutableStateFlow<List<GoalOverview>>(emptyList())

        override fun observePrimaryGoal(): Flow<GoalOverview?> = primaryGoalFlow

        override fun observeGoals(): Flow<List<GoalOverview>> = goalsFlow

        override suspend fun addGoal(
            title: String,
            targetAmount: Double,
            currentSaved: Double,
            monthsToDeadline: Int,
            monthlyContribution: Double,
            contributionDayOfMonth: Int,
            allowEmergencyUse: Boolean,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun updateGoal(
            goalId: String,
            title: String,
            targetAmount: Double,
            currentSaved: Double,
            monthsToDeadline: Int,
            monthlyContribution: Double,
            contributionDayOfMonth: Int,
            allowEmergencyUse: Boolean,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun deleteGoal(goalId: String): Result<Unit> = Result.success(Unit)

        override suspend fun applyEmergencyWithdrawal(goalId: String, amount: Double): Result<Unit> = Result.success(Unit)

        override suspend fun refreshGoalContributionsIfNeeded() {}

        override suspend fun seedDemoGoalIfNeeded() {}
    }
}