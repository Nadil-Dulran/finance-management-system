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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalMonthlyMinimumTest {
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
    fun addGoal_withMonthlyContributionBelowRequiredMinimum_showsErrorAndDoesNotSaveGoal() = runTest {
        val fakeRepository = FakeGoalRepository()
        val viewModel = GoalViewModel(fakeRepository)

        backgroundScope.launch {
            viewModel.uiState.collect { }
        }

        advanceUntilIdle()

        viewModel.addGoal(
            title = "Laptop",
            targetAmount = "100000",
            currentSaved = "20000",
            monthsToDeadline = "8",
            monthlyContribution = "5000",
            contributionDayOfMonth = "5",
            allowEmergencyUse = true,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(fakeRepository.addGoalCalled)
        assertEquals(
            "Monthly saving amount must be at least 10000 to reach this goal on time.",
            state.message,
        )
        assertFalse(state.isSaving)
    }

    private class FakeGoalRepository : GoalRepository {
        private val primaryGoalFlow = MutableStateFlow<GoalOverview?>(null)
        private val goalsFlow = MutableStateFlow<List<GoalOverview>>(emptyList())

        var addGoalCalled = false

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