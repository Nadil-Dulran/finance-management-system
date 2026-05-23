package com.example.finance_management_system.tests.offline

import com.example.finance_management_system.model.ChartDatum
import com.example.finance_management_system.model.DetectedTransactionItem
import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.model.SummaryCard
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.model.TransactionType
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
import com.example.finance_management_system.viewmodel.DashboardViewModel
import com.example.finance_management_system.viewmodel.GoalViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRestartLocalDataTest {
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
    fun tc18_appRestart_restoresCachedGoalsAfterViewModelRecreation() = runTest {
        val fakeGoalRepository = FakeGoalRepository(
            initialGoals = listOf(cachedGoal()),
        )

        val firstViewModel = GoalViewModel(fakeGoalRepository)
        backgroundScope.launch {
            firstViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val initialState = firstViewModel.uiState.value
        assertEquals(1, initialState.goals.size)
        assertEquals("Emergency Fund", initialState.goals.first().title)

        val restartedViewModel = GoalViewModel(fakeGoalRepository)
        backgroundScope.launch {
            restartedViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val restartedState = restartedViewModel.uiState.value
        assertEquals(1, restartedState.goals.size)
        assertEquals("Emergency Fund", restartedState.goals.first().title)
        assertEquals(50000.0, restartedState.goals.first().targetAmountLkr, 0.0)
        assertEquals(15000.0, restartedState.goals.first().currentSavedLkr, 0.0)
        assertEquals(2500.0, restartedState.goals.first().monthlyContributionLkr, 0.0)
        assertNotNull(restartedState.overview)
        assertEquals(50000.0, restartedState.overview?.targetAmountLkr ?: 0.0, 0.0)
    }

    @Test
    fun tc18_appRestart_restoresCachedTransactionsAndDashboardStateAfterViewModelRecreation() = runTest {
        val fakeFinanceRepository = FakeFinanceRepository(
            summaryCards = cachedSummaryCards(),
            recentTransactions = listOf(cachedIncomeTransaction(), cachedExpenseTransaction()),
        )
        val fakeGoalRepository = FakeGoalRepository(
            initialGoals = listOf(cachedGoal()),
        )

        val firstViewModel = DashboardViewModel(fakeFinanceRepository, fakeGoalRepository)
        backgroundScope.launch {
            firstViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val initialState = firstViewModel.uiState.value
        assertEquals(3, initialState.summaryCards.size)
        assertEquals(2, initialState.recentTransactions.size)
        assertEquals("All Goals", initialState.featuredGoal?.title)

        val restartedViewModel = DashboardViewModel(fakeFinanceRepository, fakeGoalRepository)
        backgroundScope.launch {
            restartedViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val restartedState = restartedViewModel.uiState.value
        assertEquals(3, restartedState.summaryCards.size)
        assertEquals("Income recorded", restartedState.summaryCards.first().title)
        assertEquals("LKR 75,000.00", restartedState.summaryCards.first().amountLabel)
        assertEquals(2, restartedState.recentTransactions.size)
        assertEquals("Salary", restartedState.recentTransactions.first().title)
        assertEquals("All Goals", restartedState.featuredGoal?.title)
        assertEquals(50000.0, restartedState.featuredGoal?.targetAmountLkr ?: 0.0, 0.0)
        assertEquals(15000.0, restartedState.featuredGoal?.currentSavedLkr ?: 0.0, 0.0)
        assertTrue(restartedState.recentTransactions.isNotEmpty())
    }

    private fun cachedSummaryCards(): List<SummaryCard> = listOf(
        SummaryCard(
            title = "Income recorded",
            amountLabel = "LKR 75,000.00",
            description = "All income added to this account",
        ),
        SummaryCard(
            title = "Expenses recorded",
            amountLabel = "LKR 24,000.00",
            description = "Spent so far across all expense entries",
        ),
        SummaryCard(
            title = "Estimated free cash",
            amountLabel = "LKR 51,000.00",
            description = "Remaining amount after expenses and goal reserves",
        ),
    )

    private fun cachedGoal(): GoalOverview = GoalOverview(
        id = "goal-1",
        title = "Emergency Fund",
        targetAmountLabel = "LKR 50,000.00",
        currentSavedLabel = "LKR 15,000.00",
        remainingAmountLabel = "LKR 35,000.00",
        deadlineLabel = "6 months remaining",
        monthlyNeedLabel = "Need about LKR 5,833.33 per month",
        monthlyContributionLabel = "LKR 2,500.00",
        contributionScheduleLabel = "Auto-save on day 5 from Salary",
        emergencyUseLabel = "Emergency use is allowed",
        emergencyUsedLabel = "No emergency withdrawals yet",
        targetAmountLkr = 50000.0,
        currentSavedLkr = 15000.0,
        monthlyContributionLkr = 2500.0,
        contributionDayOfMonth = 5,
        monthsRemaining = 6,
        allowEmergencyUse = true,
        progress = 0.3f,
        isCompleted = false,
    )

    private fun cachedIncomeTransaction(): TransactionItem = TransactionItem(
        id = "txn-income-1",
        type = TransactionType.INCOME,
        title = "Salary",
        amountLabel = "LKR 75,000.00",
        meta = "May 2026",
        originalAmount = 75000.0,
        originalCurrency = "LKR",
        note = "Monthly salary",
    )

    private fun cachedExpenseTransaction(): TransactionItem = TransactionItem(
        id = "txn-expense-1",
        type = TransactionType.EXPENSE,
        title = "Groceries",
        amountLabel = "- LKR 24,000.00",
        meta = "May 2026",
        originalAmount = 24000.0,
        originalCurrency = "LKR",
        spendingType = "Needs",
        paymentMethod = "Debit Card",
        note = "Weekly household shopping",
    )

    private class FakeFinanceRepository(
        summaryCards: List<SummaryCard>,
        recentTransactions: List<TransactionItem>,
    ) : FinanceRepository {
        val summaryFlow = MutableStateFlow(summaryCards)
        val expenseChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val incomeChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val recentTransactionsFlow = MutableStateFlow(recentTransactions)

        override fun observeDashboardSummary(): Flow<List<SummaryCard>> = summaryFlow
        override fun observeExpenseChart(): Flow<List<ChartDatum>> = expenseChartFlow
        override fun observeIncomeChart(): Flow<List<ChartDatum>> = incomeChartFlow
        override fun observeRecentTransactions(): Flow<List<TransactionItem>> = recentTransactionsFlow
        override fun observeRecurringExpenses(): Flow<List<TransactionItem>> = flowOf(emptyList())
        override fun observeDetectedTransactions(): Flow<List<DetectedTransactionItem>> = flowOf(emptyList())
        override suspend fun seedDemoDataIfNeeded() {}
        override suspend fun addIncome(sourceType: String, amount: Double, currency: String, note: String): Result<Unit> = Result.success(Unit)
        override suspend fun addExpense(category: String, amount: Double, currency: String, spendingType: String, recurrenceType: String, paymentMethod: String, accountName: String, note: String): Result<Unit> = Result.success(Unit)
        override suspend fun refreshRecurringExpensesIfNeeded() {}
        override suspend fun ingestDetectedTransaction(packageName: String, title: String, body: String, postedAt: Long) {}
        override suspend fun confirmDetectedTransaction(id: String, chosenType: String, chosenCategoryOrSource: String, chosenSpendingType: String, note: String): Result<Unit> = Result.success(Unit)
        override suspend fun ignoreDetectedTransaction(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
        override suspend fun deleteTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
        override suspend fun checkAndNotifyUpcomingBills() {}
    }

    private class FakeGoalRepository(
        initialGoals: List<GoalOverview>,
    ) : GoalRepository {
        val primaryGoalFlow = MutableStateFlow<GoalOverview?>(initialGoals.firstOrNull())
        val goalsFlow = MutableStateFlow(initialGoals)

        override fun observePrimaryGoal(): Flow<GoalOverview?> = primaryGoalFlow
        override fun observeGoals(): Flow<List<GoalOverview>> = goalsFlow
        override suspend fun addGoal(title: String, targetAmount: Double, currentSaved: Double, monthsToDeadline: Int, monthlyContribution: Double, contributionDayOfMonth: Int, allowEmergencyUse: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun updateGoal(goalId: String, title: String, targetAmount: Double, currentSaved: Double, monthsToDeadline: Int, monthlyContribution: Double, contributionDayOfMonth: Int, allowEmergencyUse: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun deleteGoal(goalId: String): Result<Unit> = Result.success(Unit)
        override suspend fun applyEmergencyWithdrawal(goalId: String, amount: Double): Result<Unit> = Result.success(Unit)
        override suspend fun refreshGoalContributionsIfNeeded() {}
        override suspend fun seedDemoGoalIfNeeded() {}
    }
}