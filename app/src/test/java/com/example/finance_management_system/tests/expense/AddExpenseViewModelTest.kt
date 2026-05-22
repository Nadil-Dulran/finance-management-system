package com.example.finance_management_system.tests.expense

import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.model.ChartDatum
import com.example.finance_management_system.model.DetectedTransactionItem
import com.example.finance_management_system.model.SummaryCard
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.repository.FinanceRepository
import com.example.finance_management_system.viewmodel.AddExpenseViewModel
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
class AddExpenseViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeFinanceRepository
    private lateinit var viewModel: AddExpenseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeFinanceRepository()
        viewModel = AddExpenseViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveExpense_withCategoryAndPaymentMethod_savesSuccessfullyAndResetsAmount() = runTest {
        // User inputs category, amount and payment method
        val category = "Dining"
        val amount = "1250.0"
        val paymentMethod = "Credit Card"
        val note = "Dinner with friends"

        viewModel.updateCategory(category)
        viewModel.updateAmount(amount)
        viewModel.updatePaymentMethod(paymentMethod)
        viewModel.updateNote(note)

        var onSavedCalled = false
        viewModel.save { onSavedCalled = true }
        advanceUntilIdle()

        assertTrue("onSaved callback should be invoked", onSavedCalled)
        val lastExpense = fakeRepository.lastAddedExpense
        assertEquals(category, lastExpense?.category)
        assertEquals(1250.0, lastExpense?.amount ?: 0.0, 0.0)
        assertEquals(paymentMethod, lastExpense?.paymentMethod)
        assertEquals(note, lastExpense?.note)

        // Then: UI state is updated to show success and reset the amount field
        val state = viewModel.uiState.value
        assertEquals("", state.amount)
        assertEquals(AppDefaults.SUCCESS_EXPENSE_SAVED, state.successMessage)
    }

    @Test
    fun save_withBlankAmount_showsErrorMessageAndDoesNotSave() = runTest {
        // User inputs a blank amount
        viewModel.updateAmount("")

        // The save action is triggered
        var onSavedCalled = false
        viewModel.save { onSavedCalled = true }
        advanceUntilIdle()

        // Validation error
        val state = viewModel.uiState.value
        assertEquals(AppDefaults.ERROR_INVALID_EXPENSE, state.errorMessage)

        // Record is not saved
        assertTrue("onSaved callback should NOT be invoked", !onSavedCalled)
        assertNull("Repository should not have received any expense", fakeRepository.lastAddedExpense)
    }

    @Test
    fun save_withInvalidAmount_showsErrorMessageAndDoesNotSave() = runTest {
        // Invalid amount (non-numeric)
        viewModel.updateAmount("abc")

        // Save action
        var onSavedCalled = false
        viewModel.save { onSavedCalled = true }
        advanceUntilIdle()

        // Validation error
        val state = viewModel.uiState.value
        assertEquals(AppDefaults.ERROR_INVALID_EXPENSE, state.errorMessage)

        // Not saved
        assertTrue("onSaved callback should NOT be invoked", !onSavedCalled)
        assertNull("Repository should not have received any expense", fakeRepository.lastAddedExpense)
    }

    @Test
    fun save_withZeroAmount_showsErrorMessageAndDoesNotSave() = runTest {
        // User inputs zero amount
        viewModel.updateAmount("0")

        var onSavedCalled = false
        viewModel.save { onSavedCalled = true }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppDefaults.ERROR_INVALID_EXPENSE, state.errorMessage)

        assertTrue("onSaved callback should NOT be invoked", !onSavedCalled)
        assertNull("Repository should not have received any expense", fakeRepository.lastAddedExpense)
    }

    private class FakeFinanceRepository : FinanceRepository {
        var lastAddedExpense: AddedExpense? = null

        data class AddedExpense(
            val category: String,
            val amount: Double,
            val currency: String,
            val spendingType: String,
            val recurrenceType: String,
            val paymentMethod: String,
            val accountName: String,
            val note: String
        )

        override suspend fun addExpense(
            category: String,
            amount: Double,
            currency: String,
            spendingType: String,
            recurrenceType: String,
            paymentMethod: String,
            accountName: String,
            note: String
        ): Result<Unit> {
            lastAddedExpense = AddedExpense(
                category, amount, currency, spendingType, recurrenceType, paymentMethod, accountName, note
            )
            return Result.success(Unit)
        }

        override suspend fun addIncome(
            sourceType: String,
            amount: Double,
            currency: String,
            note: String
        ): Result<Unit> = Result.success(Unit)

        override fun observeDashboardSummary(): Flow<List<SummaryCard>> = flowOf(emptyList())
        override fun observeExpenseChart(): Flow<List<ChartDatum>> = flowOf(emptyList())
        override fun observeIncomeChart(): Flow<List<ChartDatum>> = flowOf(emptyList())
        override fun observeRecentTransactions(): Flow<List<TransactionItem>> = flowOf(emptyList())
        override fun observeRecurringExpenses(): Flow<List<TransactionItem>> = flowOf(emptyList())
        override fun observeDetectedTransactions(): Flow<List<DetectedTransactionItem>> = flowOf(emptyList())
        override suspend fun seedDemoDataIfNeeded() {}
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
