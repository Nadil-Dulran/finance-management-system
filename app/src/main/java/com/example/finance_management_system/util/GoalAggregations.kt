package com.example.finance_management_system.util

import com.example.finance_management_system.data.currency.CurrencyConverter
import com.example.finance_management_system.model.GoalOverview

private fun parseAmountFromLabel(label: String): Double {
    // Remove any non-numeric characters except dot and minus, e.g. "LKR 1,234.56" -> "1234.56"
    val numeric = label.replace(Regex("[^0-9.-]"), "")
    return numeric.toDoubleOrNull() ?: 0.0
}

fun calculateTotalGoalSavings(goals: List<GoalOverview>): Double = goals.sumOf { it.currentSavedLkr }

fun calculateTotalGoalTarget(goals: List<GoalOverview>): Double = goals.sumOf { it.targetAmountLkr }

fun calculateTotalRemaining(goals: List<GoalOverview>): Double = goals.sumOf { (it.targetAmountLkr - it.currentSavedLkr).coerceAtLeast(0.0) }

fun calculateTotalMonthlyGoalContribution(goals: List<GoalOverview>): Double = goals.sumOf { it.monthlyContributionLkr }

fun calculateTotalMonthlyNeeded(goals: List<GoalOverview>): Double {
    return goals.sumOf { goal ->
        if (goal.isCompleted || goal.monthsRemaining <= 0) {
            0.0
        } else {
            val remaining = (goal.targetAmountLkr - goal.currentSavedLkr).coerceAtLeast(0.0)
            if (remaining <= 0.0) {
                0.0
            } else {
                remaining / goal.monthsRemaining
            }
        }
    }
}

/**
 * Produce an aggregated GoalOverview representing totals across all provided goals.
 * Returns null when the list is empty.
 */
fun aggregateGoalsOverview(goals: List<GoalOverview>): GoalOverview? {
    if (goals.isEmpty()) return null

    val totalTargetLkr = calculateTotalGoalTarget(goals)
    val totalSavedLkr = calculateTotalGoalSavings(goals)
    val totalRemainingLkr = (totalTargetLkr - totalSavedLkr).coerceAtLeast(0.0)
    val totalMonthlyContributionLkr = calculateTotalMonthlyGoalContribution(goals)

    // Try to infer display currency from the first goal's labels (fallback to LKR)
    val sampleLabel = goals.first().targetAmountLabel
    val currencyCode = sampleLabel.split(" ").firstOrNull()?.filter { it.isLetter() }?.ifEmpty { "LKR" } ?: "LKR"

    // Sum the already-formatted display amounts by parsing numeric parts from the labels.
    val totalTargetDisplay = goals.sumOf { parseAmountFromLabel(it.targetAmountLabel) }
    val totalSavedDisplay = goals.sumOf { parseAmountFromLabel(it.currentSavedLabel) }
    val totalRemainingDisplay = (totalTargetDisplay - totalSavedDisplay).coerceAtLeast(0.0)

    val targetAmountLabel = CurrencyConverter.format(totalTargetDisplay, currencyCode)
    val currentSavedLabel = CurrencyConverter.format(totalSavedDisplay, currencyCode)
    val remainingAmountLabel = CurrencyConverter.format(totalRemainingDisplay, currencyCode)

    val monthsRemaining = goals.map { it.monthsRemaining }.maxOrNull() ?: 0
    val monthlyNeedLabel = if (monthsRemaining > 0) {
        val needPerMonth = (totalRemainingDisplay / monthsRemaining)
        "Need about ${CurrencyConverter.format(needPerMonth, currencyCode)} per month"
    } else {
        ""
    }

    val monthlyContributionLabel = CurrencyConverter.format(goals.sumOf { parseAmountFromLabel(it.monthlyContributionLabel) }, currencyCode)

    val progress = if (totalTargetLkr <= 0.0) 0f else (totalSavedLkr / totalTargetLkr).toFloat().coerceIn(0f, 1f)

    return GoalOverview(
        id = "aggregate",
        title = "All Goals",
        targetAmountLabel = targetAmountLabel,
        currentSavedLabel = currentSavedLabel,
        remainingAmountLabel = remainingAmountLabel,
        deadlineLabel = "",
        monthlyNeedLabel = monthlyNeedLabel,
        monthlyContributionLabel = monthlyContributionLabel,
        contributionScheduleLabel = "",
        emergencyUseLabel = "",
        emergencyUsedLabel = CurrencyConverter.format(goals.sumOf { it.emergencyUsedLabel.let { l -> parseAmountFromLabel(l) } }, currencyCode),
        targetAmountLkr = totalTargetLkr,
        currentSavedLkr = totalSavedLkr,
        monthlyContributionLkr = totalMonthlyContributionLkr,
        contributionDayOfMonth = goals.map { it.contributionDayOfMonth }.minOrNull() ?: 1,
        monthsRemaining = monthsRemaining,
        allowEmergencyUse = goals.any { it.allowEmergencyUse },
        progress = progress,
        isCompleted = totalSavedLkr >= totalTargetLkr,
    )
}
