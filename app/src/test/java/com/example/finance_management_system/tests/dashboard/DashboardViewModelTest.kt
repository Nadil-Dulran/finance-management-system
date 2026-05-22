package com.example.finance_management_system.tests.dashboard

import com.example.finance_management_system.model.ChartDatum
import com.example.finance_management_system.model.DetectedTransactionItem
import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.model.SummaryCard
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.repository.GoalRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeFinanceRepository: FakeFinanceRepository
    private lateinit var fakeGoalRepository: FakeGoalRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeFinanceRepository = FakeFinanceRepository()
        fakeGoalRepository = FakeGoalRepository()
        viewModel = DashboardViewModel(fakeFinanceRepository, fakeGoalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun spendingAnalysis_reflectsCategoryDistributionCorrectly() = runTest {
        // Collect UI state in background to activate the StateFlow
        backgroundScope.launch {
            viewModel.uiState.collect {}
        }

        // Several expenses in different categories are provided by the repository
        val expenseData = listOf(
            ChartDatum("Food", 5000.0, "LKR 5,000.00"),
            ChartDatum("Transport", 2000.0, "LKR 2,000.00"),
            ChartDatum("Entertainment", 3000.0, "LKR 3,000.00")
        )
        fakeFinanceRepository.expenseChartFlow.value = expenseData

        advanceUntilIdle()

        // Dashboard UI state reflects the correct category distribution in the expense chart
        val state = viewModel.uiState.value
        assertEquals("Expense chart should have 3 categories", 3, state.expenseChart.size)
        
        val foodDatum = state.expenseChart.find { it.label == "Food" }
        assertEquals(5000.0, foodDatum?.value ?: 0.0, 0.0)
        assertEquals("LKR 5,000.00", foodDatum?.valueLabel)

        val transportDatum = state.expenseChart.find { it.label == "Transport" }
        assertEquals(2000.0, transportDatum?.value ?: 0.0, 0.0)
        assertEquals("LKR 2,000.00", transportDatum?.valueLabel)
        
        val entertainmentDatum = state.expenseChart.find { it.label == "Entertainment" }
        assertEquals(3000.0, entertainmentDatum?.value ?: 0.0, 0.0)
        assertEquals("LKR 3,000.00", entertainmentDatum?.valueLabel)
    }

    private class FakeFinanceRepository : FinanceRepository {
        val expenseChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val incomeChartFlow = MutableStateFlow<List<ChartDatum>>(emptyList())
        val summaryFlow = MutableStateFlow<List<SummaryCard>>(emptyList())
        val recentTransactionsFlow = MutableStateFlow<List<TransactionItem>>(emptyList())

        override fun observeExpenseChart(): Flow<List<ChartDatum>> = expenseChartFlow
        override fun observeIncomeChart(): Flow<List<ChartDatum>> = incomeChartFlow
        override fun observeDashboardSummary(): Flow<List<SummaryCard>> = summaryFlow
        override fun observeRecentTransactions(): Flow<List<TransactionItem>> = recentTransactionsFlow
        
        override fun observeRecurringExpenses(): Flow<List<TransactionItem>> = flowOf(emptyList())
        override fun observeDetectedTransactions(): Flow<List<DetectedTransactionItem>> = flowOf(emptyList())
        
        override suspend fun seedDemoDataIfNeeded() {}
        override suspend fun addIncome(sourceType: String, amount: Double, currency: String, note: String) = Result.success(Unit)
        override suspend fun addExpense(category: String, amount: Double, currency: String, spendingType: String, recurrenceType: String, paymentMethod: String, accountName: String, note: String) = Result.success(Unit)
        override suspend fun refreshRecurringExpensesIfNeeded() {}
        override suspend fun ingestDetectedTransaction(packageName: String, title: String, body: String, postedAt: Long) {}
        override suspend fun confirmDetectedTransaction(id: String, chosenType: String, chosenCategoryOrSource: String, chosenSpendingType: String, note: String) = Result.success(Unit)
        override suspend fun ignoreDetectedTransaction(id: String) = Result.success(Unit)
        override suspend fun updateTransaction(transaction: TransactionItem) = Result.success(Unit)
        override suspend fun deleteTransaction(transaction: TransactionItem) = Result.success(Unit)
        override suspend fun checkAndNotifyUpcomingBills() {}
    }

    private class FakeGoalRepository : GoalRepository {
        val primaryGoalFlow = MutableStateFlow<GoalOverview?>(null)
        override fun observePrimaryGoal(): Flow<GoalOverview?> = primaryGoalFlow
        override fun observeGoals(): Flow<List<GoalOverview>> = flowOf(emptyList())
        override suspend fun addGoal(title: String, targetAmount: Double, currentSaved: Double, monthsToDeadline: Int, monthlyContribution: Double, contributionDayOfMonth: Int, allowEmergencyUse: Boolean) = Result.success(Unit)
        override suspend fun updateGoal(goalId: String, title: String, targetAmount: Double, currentSaved: Double, monthsToDeadline: Int, monthlyContribution: Double, contributionDayOfMonth: Int, allowEmergencyUse: Boolean) = Result.success(Unit)
        override suspend fun deleteGoal(goalId: String) = Result.success(Unit)
        override suspend fun applyEmergencyWithdrawal(goalId: String, amount: Double) = Result.success(Unit)
        override suspend fun refreshGoalContributionsIfNeeded() {}
        override suspend fun seedDemoGoalIfNeeded() {}
    }
}
