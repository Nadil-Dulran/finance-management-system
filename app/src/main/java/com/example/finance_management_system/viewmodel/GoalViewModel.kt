package com.example.finance_management_system.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance_management_system.repository.GoalRepository
import com.example.finance_management_system.repository.local.LocalGoalRepository
import com.example.finance_management_system.ui.state.GoalUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val repository: GoalRepository,
) : ViewModel() {
    private val statusState = MutableStateFlow(GoalUiState())
    private val localGoalRepository = repository as? LocalGoalRepository

    init {
        viewModelScope.launch {
            runCatching { repository.refreshGoalContributionsIfNeeded() }
            // Legacy demo cleanup disabled to avoid accidental deletions on startup.
            // runCatching { localGoalRepository?.cleanupLegacyDemoGoals() }
        }
    }

    val uiState: StateFlow<GoalUiState> = combine(
        repository.observePrimaryGoal(),
        repository.observeGoals(),
        statusState,
    ) { overview, goals, status ->
        status.copy(
            overview = overview,
            goals = goals,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoalUiState(),
        )

    fun addGoal(
        title: String,
        targetAmount: String,
        currentSaved: String,
        monthsToDeadline: String,
        monthlyContribution: String,
        contributionDayOfMonth: String,
        allowEmergencyUse: Boolean,
    ) {
        val parsedTarget = targetAmount.toDoubleOrNull()
        val parsedSaved = currentSaved.toDoubleOrNull() ?: 0.0
        val parsedMonths = monthsToDeadline.toIntOrNull()
        val parsedMonthlyContribution = monthlyContribution.toDoubleOrNull() ?: 0.0
        val parsedContributionDay = contributionDayOfMonth.toIntOrNull()
        val minimumMonthlyRequired = calculateMinimumMonthlyRequired(parsedTarget, parsedSaved, parsedMonths)

        if (
            title.isBlank() ||
            parsedTarget == null ||
            parsedTarget <= 0.0 ||
            parsedMonths == null ||
            parsedMonths <= 0 ||
            parsedContributionDay == null ||
            parsedContributionDay !in 1..28 ||
            parsedMonthlyContribution < minimumMonthlyRequired
        ) {
            statusState.update {
                it.copy(
                    message = if (
                        parsedTarget != null &&
                        parsedTarget > 0.0 &&
                        parsedMonths != null &&
                        parsedMonths > 0 &&
                        parsedMonthlyContribution < minimumMonthlyRequired
                    ) {
                        "Monthly saving amount must be at least ${formatMinimumAmount(minimumMonthlyRequired)} to reach this goal on time."
                    } else {
                        "Enter a valid goal, deadline, monthly saving amount, and contribution day."
                    },
                )
            }
            return
        }

        viewModelScope.launch {
            statusState.update { it.copy(isSaving = true, message = null) }
            repository.addGoal(
                title = title,
                targetAmount = parsedTarget,
                currentSaved = parsedSaved,
                monthsToDeadline = parsedMonths,
                monthlyContribution = parsedMonthlyContribution,
                contributionDayOfMonth = parsedContributionDay,
                allowEmergencyUse = allowEmergencyUse,
            ).onSuccess {
                runCatching { repository.refreshGoalContributionsIfNeeded() }
                statusState.update { it.copy(isSaving = false, message = "Goal added.") }
            }.onFailure { throwable ->
                statusState.update { it.copy(isSaving = false, message = throwable.message ?: "Could not add goal.") }
            }
        }
    }

    fun updateGoal(
        goalId: String,
        title: String,
        targetAmount: String,
        currentSaved: String,
        monthsToDeadline: String,
        monthlyContribution: String,
        contributionDayOfMonth: String,
        allowEmergencyUse: Boolean,
    ) {
        val parsedTarget = targetAmount.toDoubleOrNull()
        val parsedSaved = currentSaved.toDoubleOrNull() ?: 0.0
        val parsedMonths = monthsToDeadline.toIntOrNull()
        val parsedMonthlyContribution = monthlyContribution.toDoubleOrNull() ?: 0.0
        val parsedContributionDay = contributionDayOfMonth.toIntOrNull()
        val minimumMonthlyRequired = calculateMinimumMonthlyRequired(parsedTarget, parsedSaved, parsedMonths)
        if (
            title.isBlank() ||
            parsedTarget == null ||
            parsedTarget <= 0.0 ||
            parsedMonths == null ||
            parsedMonths <= 0 ||
            parsedContributionDay == null ||
            parsedContributionDay !in 1..28 ||
            parsedMonthlyContribution < minimumMonthlyRequired
        ) {
            statusState.update {
                it.copy(
                    message = if (
                        parsedTarget != null &&
                        parsedTarget > 0.0 &&
                        parsedMonths != null &&
                        parsedMonths > 0 &&
                        parsedMonthlyContribution < minimumMonthlyRequired
                    ) {
                        "Monthly saving amount must be at least ${formatMinimumAmount(minimumMonthlyRequired)} to reach this goal on time."
                    } else {
                        "Enter a valid goal, deadline, monthly saving amount, and contribution day."
                    },
                )
            }
            return
        }

        viewModelScope.launch {
            statusState.update { it.copy(isSaving = true, message = null) }
            repository.updateGoal(
                goalId = goalId,
                title = title,
                targetAmount = parsedTarget,
                currentSaved = parsedSaved,
                monthsToDeadline = parsedMonths,
                monthlyContribution = parsedMonthlyContribution,
                contributionDayOfMonth = parsedContributionDay,
                allowEmergencyUse = allowEmergencyUse,
            ).onSuccess {
                runCatching { repository.refreshGoalContributionsIfNeeded() }
                statusState.update { it.copy(isSaving = false, message = "Goal updated.") }
            }.onFailure { throwable ->
                statusState.update { it.copy(isSaving = false, message = throwable.message ?: "Could not update goal.") }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
                .onSuccess { statusState.update { it.copy(message = "Goal deleted.") } }
                .onFailure { throwable ->
                    statusState.update { it.copy(message = throwable.message ?: "Could not delete goal.") }
                }
        }
    }

    fun applyEmergencyWithdrawal(goalId: String, amount: String) {
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0.0) {
            statusState.update { it.copy(message = "Enter a valid emergency withdrawal amount.") }
            return
        }

        viewModelScope.launch {
            repository.applyEmergencyWithdrawal(goalId, parsedAmount)
                .onSuccess {
                    statusState.update { it.copy(message = "Emergency withdrawal recorded.") }
                }
                .onFailure { throwable ->
                    statusState.update { it.copy(message = throwable.message ?: "Could not record emergency withdrawal.") }
                }
        }
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
        val rawMonthly = remaining / monthsToDeadline
        return ceil(rawMonthly * 100.0) / 100.0
    }

    private fun formatMinimumAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            String.format("%.2f", amount)
        }
    }
}
