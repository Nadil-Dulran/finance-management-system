package com.example.finance_management_system.tests.income

import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.model.ChartDatum
import com.example.finance_management_system.model.DetectedTransactionItem
import com.example.finance_management_system.model.SummaryCard
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.viewmodel.AddIncomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddIncomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeFinanceRepository
    private lateinit var viewModel: AddIncomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeFinanceRepository()
        viewModel = AddIncomeViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveMultiSourceIncome_savesWithCorrectSource() {
        runTest {
            val sources = listOf("Freelance", "AdSense", "Crypto")
            val amounts = listOf("50000", "15000", "10000")

            for (i in sources.indices) {
                val source = sources[i]
                val amount = amounts[i]

                viewModel.updateSource(source)
                viewModel.updateAmount(amount)
                viewModel.updateCurrency("LKR")
                viewModel.updateNote("Income from $source")

                var savedCalled = false
                viewModel.save { savedCalled = true }
                advanceUntilIdle()

                assertTrue("Income for $source should be saved", savedCalled)
                val lastIncome = fakeRepository.lastAddedIncome
                assertEquals(source, lastIncome?.sourceType)
                assertEquals(amount.toDouble(), lastIncome?.amount ?: 0.0, 0.0)
                assertEquals("Income from $source", lastIncome?.note)

                val state = viewModel.uiState.value
                assertEquals(AppDefaults.SUCCESS_INCOME_SAVED, state.successMessage)
                assertNull(state.errorMessage)
                assertEquals("", state.amount) // Should reset after save
            }
        }
    }

    private class FakeFinanceRepository : FinanceRepository {
        var lastAddedIncome: AddedIncome? = null

        data class AddedIncome(
            val sourceType: String,
            val amount: Double,
            val currency: String,
            val note: String
        )

        override suspend fun addIncome(
            sourceType: String,
            amount: Double,
            currency: String,
            note: String
        ): Result<Unit> {
            lastAddedIncome = AddedIncome(sourceType, amount, currency, note)
            return Result.success(Unit)
        }

        override fun observeDashboardSummary(): Flow<List<SummaryCard>> = flowOf(emptyList())
        override fun observeExpenseChart(): Flow<List<ChartDatum>> = flowOf(emptyList())
        override fun observeIncomeChart(): Flow<List<ChartDatum>> = flowOf(emptyList())
        override fun observeRecentTransactions(): Flow<List<TransactionItem>> = flowOf(emptyList())
        override fun observeRecurringExpenses(): Flow<List<TransactionItem>> = flowOf(emptyList())
        override fun observeDetectedTransactions(): Flow<List<DetectedTransactionItem>> = flowOf(emptyList())
        override suspend fun seedDemoDataIfNeeded() {}
        override suspend fun addExpense(
            category: String,
            amount: Double,
            currency: String,
            spendingType: String,
            recurrenceType: String,
            paymentMethod: String,
            accountName: String,
            note: String
        ): Result<Unit> = Result.success(Unit)
        override suspend fun refreshRecurringExpensesIfNeeded() {}
        override suspend fun ingestDetectedTransaction(
            packageName: String,
            title: String,
            body: String,
            postedAt: Long
        ) {}
        override suspend fun confirmDetectedTransaction(
            id: String,
            chosenType: String,
            chosenCategoryOrSource: String,
            chosenSpendingType: String,
            note: String
        ): Result<Unit> = Result.success(Unit)
        override suspend fun ignoreDetectedTransaction(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
        override suspend fun deleteTransaction(transaction: TransactionItem): Result<Unit> = Result.success(Unit)
        override suspend fun checkAndNotifyUpcomingBills() {}
    }
}
