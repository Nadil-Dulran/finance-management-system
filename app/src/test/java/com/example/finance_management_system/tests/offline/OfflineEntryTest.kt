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
import com.example.finance_management_system.viewmodel.AddExpenseViewModel
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
class OfflineEntryTest {
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
    fun tc16_offlineIncomeEntry_savesLocallyAndUpdatesDashboardState() = runTest {
        val fakeFinanceRepository = FakeOfflineFinanceRepository()
        val fakeGoalRepository = FakeGoalRepository()
        val addIncomeViewModel = AddIncomeViewModel(fakeFinanceRepository)
        val dashboardViewModel = DashboardViewModel(fakeFinanceRepository, fakeGoalRepository)

        backgroundScope.launch {
            dashboardViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals("LKR 0.00", dashboardViewModel.uiState.value.summaryCards.first().amountLabel)

        addIncomeViewModel.updateSource("Salary")
        addIncomeViewModel.updateAmount("75000")
        addIncomeViewModel.updateCurrency("LKR")
        addIncomeViewModel.updateNote("Monthly salary")

        var savedCalled = false
        addIncomeViewModel.save { savedCalled = true }
        advanceUntilIdle()

        val addIncomeState = addIncomeViewModel.uiState.value
        val dashboardState = dashboardViewModel.uiState.value

        assertTrue(savedCalled)
        assertEquals(1, fakeFinanceRepository.localIncomeEntries.size)
        assertEquals("Salary", fakeFinanceRepository.localIncomeEntries.first().sourceType)
        assertEquals(75000.0, fakeFinanceRepository.localIncomeEntries.first().amount, 0.0)
        assertEquals("Income saved successfully.", addIncomeState.successMessage)
        assertFalse(addIncomeState.isSaving)
        assertNull(addIncomeState.errorMessage)
        assertEquals("Income recorded", dashboardState.summaryCards.first().title)
        assertEquals("LKR 75,000.00", dashboardState.summaryCards.first().amountLabel)
        assertEquals(1, dashboardState.recentTransactions.size)
        assertEquals("Salary", dashboardState.recentTransactions.first().title)
    }

    @Test
    fun tc16_offlineExpenseEntry_savesLocallyAndUpdatesDashboardState() = runTest {
        val fakeFinanceRepository = FakeOfflineFinanceRepository()
        val fakeGoalRepository = FakeGoalRepository()
        val addExpenseViewModel = AddExpenseViewModel(fakeFinanceRepository)
        val dashboardViewModel = DashboardViewModel(fakeFinanceRepository, fakeGoalRepository)

        backgroundScope.launch {
            dashboardViewModel.uiState.collect { }
        }

        advanceUntilIdle()

        assertEquals("LKR 0.00", dashboardViewModel.uiState.value.summaryCards[1].amountLabel)

        addExpenseViewModel.updateCategory("Dining")
        addExpenseViewModel.updateAmount("1250")
        addExpenseViewModel.updateCurrency("LKR")
        addExpenseViewModel.updateType("Discretionary")
        addExpenseViewModel.updatePaymentMethod("Card")
        addExpenseViewModel.updateRecurrence("None")
        addExpenseViewModel.updateNote("Dinner with friends")

        var savedCalled = false
        addExpenseViewModel.save { savedCalled = true }
        advanceUntilIdle()

        val addExpenseState = addExpenseViewModel.uiState.value
        val dashboardState = dashboardViewModel.uiState.value

        assertTrue(savedCalled)
        assertEquals(1, fakeFinanceRepository.localExpenseEntries.size)
        assertEquals("Dining", fakeFinanceRepository.localExpenseEntries.first().category)
        assertEquals(1250.0, fakeFinanceRepository.localExpenseEntries.first().amount, 0.0)
        assertEquals("Expense saved successfully.", addExpenseState.successMessage)
        assertFalse(addExpenseState.isSaving)
        assertNull(addExpenseState.errorMessage)
        assertEquals("Expenses recorded", dashboardState.summaryCards[1].title)
        assertEquals("LKR 1,250.00", dashboardState.summaryCards[1].amountLabel)
        assertEquals(1, dashboardState.recentTransactions.size)
        assertEquals("Dining", dashboardState.recentTransactions.first().title)
    }

    private class FakeOfflineFinanceRepository : FinanceRepository {
        val summaryFlow = MutableStateFlow(initialSummary())
        val incomeChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val expenseChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val recentTransactionsFlow = MutableStateFlow<List<TransactionItem>>(emptyList())

        val localIncomeEntries = mutableListOf<AddedIncome>()
        val localExpenseEntries = mutableListOf<AddedExpense>()

        data class AddedIncome(
            val sourceType: String,
            val amount: Double,
            val currency: String,
            val note: String,
        )

        data class AddedExpense(
            val category: String,
            val amount: Double,
            val currency: String,
            val spendingType: String,
            val recurrenceType: String,
            val paymentMethod: String,
            val accountName: String,
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
            localIncomeEntries += AddedIncome(sourceType, amount, currency, note)
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
        ): Result<Unit> {
            localExpenseEntries += AddedExpense(
                category = category,
                amount = amount,
                currency = currency,
                spendingType = spendingType,
                recurrenceType = recurrenceType,
                paymentMethod = paymentMethod,
                accountName = accountName,
                note = note,
            )
            rebuildFlows()
            return Result.success(Unit)
        }

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

        private fun rebuildFlows() {
            val incomeTotal = localIncomeEntries.sumOf { it.amount }
            val expenseTotal = localExpenseEntries.sumOf { it.amount }

            summaryFlow.value = listOf(
                SummaryCard(
                    title = "Income recorded",
                    amountLabel = formatLkr(incomeTotal),
                    description = "All income added to this account",
                ),
                SummaryCard(
                    title = "Expenses recorded",
                    amountLabel = formatLkr(expenseTotal),
                    description = "Spent so far across all expense entries",
                ),
                SummaryCard(
                    title = "Estimated free cash",
                    amountLabel = formatLkr((incomeTotal - expenseTotal).coerceAtLeast(0.0)),
                    description = "Remaining amount after expenses and goal reserves",
                ),
            )

            recentTransactionsFlow.value = buildRecentTransactions()
        }

        private fun buildRecentTransactions(): List<TransactionItem> {
            val incomeTransactions = localIncomeEntries.mapIndexed { index, entry ->
                TransactionItem(
                    id = "income-$index",
                    type = TransactionType.INCOME,
                    title = entry.sourceType,
                    amountLabel = "+ ${formatLkr(entry.amount)}",
                    meta = "Income · ${entry.currency}",
                    originalAmount = entry.amount,
                    originalCurrency = entry.currency,
                    note = entry.note,
                )
            }

            val expenseTransactions = localExpenseEntries.mapIndexed { index, entry ->
                TransactionItem(
                    id = "expense-$index",
                    type = TransactionType.EXPENSE,
                    title = entry.category,
                    amountLabel = "- ${formatLkr(entry.amount)}",
                    meta = "${entry.spendingType} · ${entry.paymentMethod}",
                    originalAmount = entry.amount,
                    originalCurrency = entry.currency,
                    spendingType = entry.spendingType,
                    recurrenceType = entry.recurrenceType,
                    paymentMethod = entry.paymentMethod,
                    note = entry.note,
                )
            }

            return incomeTransactions + expenseTransactions
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