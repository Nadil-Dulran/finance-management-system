package com.example.finance_management_system.tests.goal

import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.repository.GoalRepository
import com.example.finance_management_system.viewmodel.GoalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalViewModelTest {
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
    fun addGoal_withValidDetails_createsGoalSuccessfully() = runTest {
        val fakeRepository = FakeGoalRepository()
        val viewModel = GoalViewModel(fakeRepository)
        val collectedStates = mutableListOf<String?>()
        val collectionJob = backgroundScope.launch {
            viewModel.uiState.collectLatest { state ->
                collectedStates += state.message
            }
        }

        viewModel.addGoal(
            title = "Laptop",
            targetAmount = "100000",
            currentSaved = "20000",
            monthsToDeadline = "8",
            monthlyContribution = "10000",
            contributionDayOfMonth = "5",
            allowEmergencyUse = true,
        )
        advanceUntilIdle()

        assertTrue(fakeRepository.addGoalCalled)
        assertEquals("Laptop", fakeRepository.addedTitle)
        assertNotNull(fakeRepository.addedTargetAmount)
        assertNotNull(fakeRepository.addedCurrentSaved)
        assertNotNull(fakeRepository.addedMonthlyContribution)
        assertEquals(100000.0, fakeRepository.addedTargetAmount!!, 0.0)
        assertEquals(20000.0, fakeRepository.addedCurrentSaved!!, 0.0)
        assertEquals(8, fakeRepository.addedMonthsToDeadline)
        assertEquals(10000.0, fakeRepository.addedMonthlyContribution!!, 0.0)
        assertEquals(5, fakeRepository.addedContributionDayOfMonth)
        assertTrue(fakeRepository.addedAllowEmergencyUse)
        assertTrue(fakeRepository.refreshCalled)
        assertEquals(1, viewModel.uiState.value.goals.size)
        assertEquals("Laptop", viewModel.uiState.value.goals.first().title)
        assertEquals("Goal added.", viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isSaving)

        collectionJob.cancel()
    }
}

private class FakeGoalRepository : GoalRepository {
    private val primaryGoalFlow = MutableStateFlow<GoalOverview?>(null)
    private val goalsFlow = MutableStateFlow<List<GoalOverview>>(emptyList())

    var addGoalCalled = false
    var refreshCalled = false

    var addedTitle: String? = null
    var addedTargetAmount: Double? = null
    var addedCurrentSaved: Double? = null
    var addedMonthsToDeadline: Int? = null
    var addedMonthlyContribution: Double? = null
    var addedContributionDayOfMonth: Int? = null
    var addedAllowEmergencyUse: Boolean = false

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
        addedTitle = title
        addedTargetAmount = targetAmount
        addedCurrentSaved = currentSaved
        addedMonthsToDeadline = monthsToDeadline
        addedMonthlyContribution = monthlyContribution
        addedContributionDayOfMonth = contributionDayOfMonth
        addedAllowEmergencyUse = allowEmergencyUse
        goalsFlow.value = listOf(
            fakeGoalOverview(
                id = "goal-1",
                title = title,
                targetAmountLkr = targetAmount,
                currentSavedLkr = currentSaved,
                monthlyContributionLkr = monthlyContribution,
                contributionDayOfMonth = contributionDayOfMonth,
                monthsRemaining = monthsToDeadline,
                allowEmergencyUse = allowEmergencyUse,
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

    override suspend fun refreshGoalContributionsIfNeeded() {
        refreshCalled = true
    }

    override suspend fun seedDemoGoalIfNeeded() {
        error("Not used in this test")
    }
}

private fun fakeGoalOverview(
    id: String,
    title: String,
    targetAmountLkr: Double,
    currentSavedLkr: Double,
    monthlyContributionLkr: Double,
    contributionDayOfMonth: Int,
    monthsRemaining: Int,
    allowEmergencyUse: Boolean,
): GoalOverview {
    return GoalOverview(
        id = id,
        title = title,
        targetAmountLabel = "LKR 100,000.00",
        currentSavedLabel = "LKR 20,000.00",
        remainingAmountLabel = "LKR 80,000.00",
        deadlineLabel = "$monthsRemaining months remaining",
        monthlyNeedLabel = "LKR 10,000.00",
        monthlyContributionLabel = "LKR 10,000.00",
        contributionScheduleLabel = "Day $contributionDayOfMonth",
        emergencyUseLabel = if (allowEmergencyUse) "Allowed" else "Not allowed",
        emergencyUsedLabel = "LKR 0.00",
        targetAmountLkr = targetAmountLkr,
        currentSavedLkr = currentSavedLkr,
        monthlyContributionLkr = monthlyContributionLkr,
        contributionDayOfMonth = contributionDayOfMonth,
        monthsRemaining = monthsRemaining,
        allowEmergencyUse = allowEmergencyUse,
        progress = 0.2f,
        isCompleted = false,
    )
}
