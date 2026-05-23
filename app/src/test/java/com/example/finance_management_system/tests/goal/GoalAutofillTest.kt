package com.example.finance_management_system.tests.goal

import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.repository.GoalRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.ceil

@OptIn(ExperimentalCoroutinesApi::class)
class GoalAutofillTest {
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
    fun tc13_goalAutoFill_calculatesMinimumMonthlyContributionAndSavesGoal() = runTest {
        val fakeRepository = FakeGoalRepository()
        val viewModel = GoalViewModel(fakeRepository)

        backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val targetAmount = 15000.0
        val currentSaved = 3000.0
        val monthsToDeadline = 6
        val expectedMonthlyContribution = calculateMinimumMonthlyRequired(
            targetAmount = targetAmount,
            currentSaved = currentSaved,
            monthsToDeadline = monthsToDeadline,
        )
        val autoFilledMonthlyContribution = formatGoalAmount(expectedMonthlyContribution)

        assertEquals(2000.0, expectedMonthlyContribution, 0.0)
        assertEquals("2000", autoFilledMonthlyContribution)

        viewModel.addGoal(
            title = "Laptop",
            targetAmount = targetAmount.toString(),
            currentSaved = currentSaved.toString(),
            monthsToDeadline = monthsToDeadline.toString(),
            monthlyContribution = autoFilledMonthlyContribution,
            contributionDayOfMonth = "5",
            allowEmergencyUse = true,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(fakeRepository.addGoalCalled)
        assertEquals(2000.0, fakeRepository.addedMonthlyContribution ?: 0.0, 0.0)
        assertEquals("Goal added.", state.message)
        assertFalse(state.isSaving)
    }

    @Test
    fun tc13_goalAutoFill_zeroMonthsDoesNotDivideAndShowsValidationError() = runTest {
        val fakeRepository = FakeGoalRepository()
        val viewModel = GoalViewModel(fakeRepository)

        backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        val expectedMonthlyContribution = calculateMinimumMonthlyRequired(
            targetAmount = 15000.0,
            currentSaved = 3000.0,
            monthsToDeadline = 0,
        )

        assertEquals(0.0, expectedMonthlyContribution, 0.0)
        assertEquals("0", formatGoalAmount(expectedMonthlyContribution))

        viewModel.addGoal(
            title = "Laptop",
            targetAmount = "15000",
            currentSaved = "3000",
            monthsToDeadline = "0",
            monthlyContribution = formatGoalAmount(expectedMonthlyContribution),
            contributionDayOfMonth = "5",
            allowEmergencyUse = true,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(fakeRepository.addGoalCalled)
        assertEquals("Enter a valid goal, deadline, monthly saving amount, and contribution day.", state.message)
        assertFalse(state.isSaving)
    }

    private fun calculateMinimumMonthlyRequired(
        targetAmount: Double?,
        currentSaved: Double,
        monthsToDeadline: Int?,
    ): Double {
        if (targetAmount == null || targetAmount <= 0.0 || monthsToDeadline == null || monthsToDeadline <= 0) {
            return 0.0
        }
        val remaining = (targetAmount - currentSaved).coerceAtLeast(0.0)
        return ceil((remaining / monthsToDeadline) * 100.0) / 100.0
    }

    private fun formatGoalAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            String.format("%.2f", amount)
        }
    }

    private class FakeGoalRepository : GoalRepository {
        private val primaryGoalFlow = MutableStateFlow<GoalOverview?>(null)
        private val goalsFlow = MutableStateFlow<List<GoalOverview>>(emptyList())

        var addGoalCalled = false
        var addedMonthlyContribution: Double? = null

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
        ): Result<Unit> {
            addGoalCalled = true
            addedMonthlyContribution = monthlyContribution
            goalsFlow.value = listOf(
                GoalOverview(
                    id = "goal-1",
                    title = title,
                    targetAmountLabel = "LKR 15,000.00",
                    currentSavedLabel = "LKR 3,000.00",
                    remainingAmountLabel = "LKR 12,000.00",
                    deadlineLabel = "$monthsToDeadline months remaining",
                    monthlyNeedLabel = "Need about LKR 2,000.00 per month",
                    monthlyContributionLabel = "LKR 2,000.00",
                    contributionScheduleLabel = "Auto-save on day $contributionDayOfMonth from Salary",
                    emergencyUseLabel = if (allowEmergencyUse) "Emergency use is allowed" else "Emergency use is locked",
                    emergencyUsedLabel = "No emergency withdrawals yet",
                    targetAmountLkr = targetAmount,
                    currentSavedLkr = currentSaved,
                    monthlyContributionLkr = monthlyContribution,
                    contributionDayOfMonth = contributionDayOfMonth,
                    monthsRemaining = monthsToDeadline,
                    allowEmergencyUse = allowEmergencyUse,
                    progress = 0.2f,
                    isCompleted = false,
                ),
            )
            primaryGoalFlow.value = goalsFlow.value.first()
            return Result.success(Unit)
        }

        override suspend fun updateGoal(
            goalId: String,
            title: String,
            targetAmount: Double,
            currentSaved: Double,
            monthsToDeadline: Int,
            monthlyContribution: Double,
            contributionDayOfMonth: Int,
            allowEmergencyUse: Boolean,
        ): Result<Unit> {
            error("Not used in this test")
        }

        override suspend fun deleteGoal(goalId: String): Result<Unit> {
            error("Not used in this test")
        }

        override suspend fun applyEmergencyWithdrawal(goalId: String, amount: Double): Result<Unit> {
            error("Not used in this test")
        }

        override suspend fun refreshGoalContributionsIfNeeded() {}

        override suspend fun seedDemoGoalIfNeeded() {}
    }
}