package com.example.finance_management_system.tests.income

import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.model.ChartDatum
import com.example.finance_management_system.model.DetectedTransactionItem
import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.model.SummaryCard
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
import com.example.finance_management_system.ui.state.EntryFormUiState
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddIncomeDashboardUpdateTest {
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
    fun tc05_addSalaryIncomeInLkr_incomeIsSavedAndDashboardTotalIncomeUpdates() = runTest {
        val fakeFinanceRepository = FakeFinanceRepository()
        val fakeGoalRepository = FakeGoalRepository()
        val addIncomeViewModel = AddIncomeViewModel(fakeFinanceRepository)
        val dashboardViewModel = DashboardViewModel(fakeFinanceRepository, fakeGoalRepository)

        backgroundScope.launch {
            dashboardViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val initialDashboardState = dashboardViewModel.uiState.value
        assertEquals("LKR 0.00", initialDashboardState.summaryCards.first().amountLabel)

        addIncomeViewModel.updateSource("Salary")
        addIncomeViewModel.updateAmount("75000")
        addIncomeViewModel.updateCurrency("LKR")
        addIncomeViewModel.updateNote("Monthly salary")

        var savedCalled = false
        addIncomeViewModel.save { savedCalled = true }
        advanceUntilIdle()

        val savedIncome = fakeFinanceRepository.lastAddedIncome
        assertTrue(savedCalled)
        assertEquals("Salary", savedIncome?.sourceType)
        assertEquals(75000.0, savedIncome?.amount ?: 0.0, 0.0)
        assertEquals("LKR", savedIncome?.currency)
        assertEquals("Monthly salary", savedIncome?.note)

        val addIncomeState = addIncomeViewModel.uiState.value
        assertFalse(addIncomeState.isSaving)
        assertEquals(AppDefaults.SUCCESS_INCOME_SAVED, addIncomeState.successMessage)
        assertEquals("", addIncomeState.amount)
        assertNull(addIncomeState.errorMessage)

        val dashboardState = dashboardViewModel.uiState.value
        assertEquals("Income recorded", dashboardState.summaryCards.first().title)
        assertEquals("LKR 75,000.00", dashboardState.summaryCards.first().amountLabel)
    }

    private class FakeFinanceRepository : FinanceRepository {
        val summaryFlow = MutableStateFlow(initialSummary(0.0))
        val incomeChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val expenseChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val recentTransactionsFlow = MutableStateFlow<List<TransactionItem>>(emptyList())
        var lastAddedIncome: AddedIncome? = null
        private var totalIncomeLkr = 0.0

        data class AddedIncome(
            val sourceType: String,
            val amount: Double,
            val currency: String,
            val note: String,
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
            lastAddedIncome = AddedIncome(sourceType, amount, currency, note)
            totalIncomeLkr += amount
            summaryFlow.value = initialSummary(totalIncomeLkr)
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

        private fun initialSummary(totalIncome: Double): List<SummaryCard> = listOf(
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

        private fun formatLkr(value: Double): String = "LKR ${String.format("%,.2f", value)}"
    }

    private class FakeGoalRepository : GoalRepository {
        val primaryGoalFlow = MutableStateFlow<GoalOverview?>(null)

        override fun observePrimaryGoal(): Flow<GoalOverview?> = primaryGoalFlow
        override fun observeGoals(): Flow<List<GoalOverview>> = flowOf(emptyList())
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