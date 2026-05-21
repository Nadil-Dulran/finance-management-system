package com.example.finance_management_system.assistant

import com.example.finance_management_system.data.currency.CurrencyConverter
import com.example.finance_management_system.data.local.entity.ExpenseEntity
import com.example.finance_management_system.data.local.entity.GoalEntity
import com.example.finance_management_system.data.local.entity.IncomeEntity
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class AssistantEngine @Inject constructor() {
    fun reply(
        input: String,
        snapshot: AssistantFinanceSnapshot?,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        val normalized = input.lowercase(Locale.US).trim()

        if (normalized.isBlank()) {
            return AssistantReply(
                message = "Ask me about income, expenses, free cash, spending trends, or goals.",
                memory = memory,
            )
        }

        if (snapshot == null) {
            return AssistantReply(
                message = "I can help once your finance data is available in the app.",
                memory = memory,
            )
        }

        val goals = snapshot.goals.sortedBy { it.deadlineAt }
        val expenses = snapshot.expenses.filterNot { it.isRecurringTemplate }
        val selectedGoal = resolveGoal(normalized, goals, memory)
        val comparisonPeriod = resolveComparisonPeriod(normalized, memory)
        val comparisonMetric = resolveComparisonMetric(normalized, memory)
        val comparisonCategory = resolveComparisonCategory(normalized, expenses, memory)

        return when {
            isAllGoalsQuery(normalized) -> replyWithAllGoals(snapshot, goals, memory)
            isClosestGoalQuery(normalized) -> replyWithClosestGoal(snapshot, goals, memory)
            isGoalQuery(normalized, memory, selectedGoal != null) -> replyWithGoalDetails(
                normalized = normalized,
                snapshot = snapshot,
                goals = goals,
                selectedGoal = selectedGoal,
                memory = memory,
            )
            isTopCategoryQuery(normalized) -> replyWithTopExpenseCategory(snapshot, expenses, memory)
            isComparisonQuery(normalized, memory) -> replyWithComparison(
                snapshot = snapshot,
                expenses = expenses,
                period = comparisonPeriod,
                metric = comparisonMetric,
                category = comparisonCategory,
                selectedGoalId = selectedGoal?.id ?: memory.selectedGoalId,
            )
            isIncomeQuery(normalized) -> replyWithIncome(snapshot, memory)
            isFreeCashQuery(normalized) -> replyWithFreeCash(snapshot, memory)
            isExpensesQuery(normalized) -> replyWithExpenses(snapshot, expenses, memory)
            else -> AssistantReply(
                message = "I can help with income, expenses, free cash, spending comparisons, and goals from your app data.",
                memory = memory,
            )
        }
    }

    private fun replyWithAllGoals(
        snapshot: AssistantFinanceSnapshot,
        goals: List<GoalEntity>,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        if (goals.isEmpty()) {
            return AssistantReply(
                message = "You do not have any goals in the app yet.",
                memory = memory.copy(currentTopic = AssistantTopic.GOAL, selectedGoalId = null),
            )
        }

        val message = buildString {
            append("Your goals:")
            goals.take(3).forEachIndexed { index, goal ->
                val detail = goalDetail(goal, snapshot)
                append("\n${index + 1}. ${goal.title} — ${detail.savedLabel} saved, ${detail.remainingLabel} left.")
            }
        }

        return AssistantReply(
            message = message,
            memory = memory.copy(
                currentTopic = AssistantTopic.GOAL,
                selectedGoalId = goals.firstOrNull()?.id,
            ),
        )
    }

    private fun replyWithClosestGoal(
        snapshot: AssistantFinanceSnapshot,
        goals: List<GoalEntity>,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        val goal = goals.firstOrNull()
            ?: return AssistantReply(
                message = "You do not have any goals in the app yet.",
                memory = memory.copy(currentTopic = AssistantTopic.GOAL, selectedGoalId = null),
            )

        val detail = goalDetail(goal, snapshot)
        return AssistantReply(
            message = buildString {
                append("Closest goal: ${goal.title}.\n")
                append("Saved: ${detail.savedLabel}.\n")
                append("Left: ${detail.remainingLabel}.\n")
                append("Deadline: ${detail.deadlineLabel}.")
            },
            memory = memory.copy(
                currentTopic = AssistantTopic.GOAL,
                selectedGoalId = goal.id,
            ),
        )
    }

    private fun replyWithGoalDetails(
        normalized: String,
        snapshot: AssistantFinanceSnapshot,
        goals: List<GoalEntity>,
        selectedGoal: GoalEntity?,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        if (goals.isEmpty()) {
            return AssistantReply(
                message = "You do not have any goals in the app yet.",
                memory = memory.copy(currentTopic = AssistantTopic.GOAL, selectedGoalId = null),
            )
        }

        val goal = selectedGoal
            ?: goals.firstOrNull { it.id == memory.selectedGoalId }
            ?: goals.first()

        val detail = goalDetail(goal, snapshot)
        val message = when {
            normalized.contains("name") -> "Your current goal is ${goal.title}."
            normalized.contains("target") -> "Target: ${detail.targetLabel}."
            normalized.contains("saved") || normalized.contains("save so far") -> "Saved: ${detail.savedLabel}."
            normalized.contains("left") || normalized.contains("remaining") -> "Left: ${detail.remainingLabel}."
            normalized.contains("deadline") || normalized.contains("due") -> "Deadline: ${detail.deadlineLabel}."
            normalized.contains("monthly") || normalized.contains("per month") || normalized.contains("need") -> {
                if (normalized.contains("contribution")) {
                    "Current monthly contribution for ${goal.title}: ${detail.monthlyContributionLabel}."
                } else {
                    "Monthly needed savings for ${goal.title}: ${detail.monthlyNeedLabel}."
                }
            }
            normalized.contains("contribution") -> "Current monthly contribution for ${goal.title}: ${detail.monthlyContributionLabel}."
            normalized.contains("goal") || normalized.contains("what is my goal") || normalized.contains("current goal") -> {
                buildString {
                    append("Goal: ${goal.title}.\n")
                    append("Saved: ${detail.savedLabel}.\n")
                    append("Left: ${detail.remainingLabel}.\n")
                    append("Deadline: ${detail.deadlineLabel}.")
                }
            }
            else -> {
                buildString {
                    append("Goal: ${goal.title}.\n")
                    append("Saved: ${detail.savedLabel}.\n")
                    append("Left: ${detail.remainingLabel}.\n")
                    append("Deadline: ${detail.deadlineLabel}.")
                }
            }
        }

        return AssistantReply(
            message = message,
            memory = memory.copy(
                currentTopic = AssistantTopic.GOAL,
                selectedGoalId = goal.id,
            ),
        )
    }

    private fun replyWithTopExpenseCategory(
        snapshot: AssistantFinanceSnapshot,
        expenses: List<ExpenseEntity>,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        if (expenses.isEmpty()) {
            return AssistantReply(
                message = "I do not have any recorded expenses yet, so I cannot find a top category.",
                memory = memory.copy(currentTopic = AssistantTopic.EXPENSES),
            )
        }

        val topCategory = expenses.groupBy { it.category }
            .maxByOrNull { (_, items) -> items.sumOf { it.amountLkr } }
            ?.key
            ?: return AssistantReply(
                message = "I could not identify a top expense category from the current data.",
                memory = memory.copy(currentTopic = AssistantTopic.EXPENSES),
            )

        return AssistantReply(
            message = "Your top expense category right now is $topCategory.",
            memory = memory.copy(currentTopic = AssistantTopic.EXPENSES),
        )
    }

    private fun replyWithIncome(
        snapshot: AssistantFinanceSnapshot,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        if (snapshot.incomes.isEmpty()) {
            return AssistantReply(
                message = "I do not have any recorded income yet.",
                memory = memory.copy(currentTopic = AssistantTopic.INCOME),
            )
        }

        val totalIncome = snapshot.incomes.sumOf { it.amountLkr }
        return AssistantReply(
            message = "Your total income is ${formatCurrency(totalIncome, snapshot)}.",
            memory = memory.copy(currentTopic = AssistantTopic.INCOME),
        )
    }

    private fun replyWithExpenses(
        snapshot: AssistantFinanceSnapshot,
        expenses: List<ExpenseEntity>,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        if (expenses.isEmpty()) {
            return AssistantReply(
                message = "I do not have any recorded expenses yet.",
                memory = memory.copy(currentTopic = AssistantTopic.EXPENSES),
            )
        }

        val totalExpenses = expenses.sumOf { it.amountLkr }
        return AssistantReply(
            message = "Your total expenses are ${formatCurrency(totalExpenses, snapshot)}.",
            memory = memory.copy(currentTopic = AssistantTopic.EXPENSES),
        )
    }

    private fun replyWithFreeCash(
        snapshot: AssistantFinanceSnapshot,
        memory: AssistantConversationMemory,
    ): AssistantReply {
        val incomeTotal = snapshot.incomes.sumOf { it.amountLkr }
        val expenseTotal = snapshot.expenses.filterNot { it.isRecurringTemplate }.sumOf { it.amountLkr }
        val reservedTotal = snapshot.goals.sumOf { it.currentSavedLkr }
        val freeCash = incomeTotal - expenseTotal - reservedTotal

        val message = if (freeCash >= 0.0) {
            "Your estimated free cash is ${formatCurrency(freeCash, snapshot)}."
        } else {
            "You are currently ${formatCurrency(abs(freeCash), snapshot)} over recorded income after expenses and goal reserves."
        }

        return AssistantReply(
            message = message,
            memory = memory.copy(currentTopic = AssistantTopic.FREE_CASH),
        )
    }

    private fun replyWithComparison(
        snapshot: AssistantFinanceSnapshot,
        expenses: List<ExpenseEntity>,
        period: ComparisonPeriod,
        metric: ComparisonMetric,
        category: String?,
        selectedGoalId: String?,
    ): AssistantReply {
        val message = when (metric) {
            ComparisonMetric.EXPENSES -> {
                val scopedExpenses = category?.let { selected ->
                    expenses.filter { it.category.equals(selected, ignoreCase = true) }
                } ?: expenses
                if (scopedExpenses.isEmpty()) {
                    if (category != null) {
                        "I do not have enough $category expense data to compare yet."
                    } else {
                        "I do not have enough expense data to compare spending yet."
                    }
                } else {
                    compareSpending(scopedExpenses, snapshot, period)?.toMessage(
                        if (category != null) "$category spending" else "spending",
                    ) ?: if (category != null) {
                        "I do not have enough $category expense data to compare for that period yet."
                    } else {
                        "I do not have enough expense data to compare spending for that period yet."
                    }
                }
            }
            ComparisonMetric.INCOME -> {
                if (snapshot.incomes.isEmpty()) {
                    "I do not have enough income data to compare income yet."
                } else {
                    compareIncome(snapshot.incomes, snapshot, period)?.toMessage("income")
                        ?: "I do not have enough income data to compare income for that period yet."
                }
            }
            ComparisonMetric.FREE_CASH -> compareFreeCash(snapshot, period)
        }

        return AssistantReply(
            message = message,
            memory = AssistantConversationMemory(
                currentTopic = AssistantTopic.COMPARISON,
                selectedGoalId = selectedGoalId,
                comparisonPeriod = period,
                comparisonMetric = metric,
                comparisonCategory = category,
            ),
        )
    }

    private fun isAllGoalsQuery(normalized: String): Boolean {
        return listOf("all goals", "list goals", "show goals", "what are my goals").any(normalized::contains)
    }

    private fun isClosestGoalQuery(normalized: String): Boolean {
        return normalized.contains("closest goal") || normalized.contains("nearest goal")
    }

    private fun isGoalQuery(
        normalized: String,
        memory: AssistantConversationMemory,
        hasResolvedGoal: Boolean,
    ): Boolean {
        val goalKeywords = listOf(
            "goal",
            "saved",
            "left",
            "remaining",
            "deadline",
            "due",
            "target",
            "monthly need",
            "per month",
            "contribution",
            "its name",
            "what's its name",
        )
        val followUp = memory.currentTopic == AssistantTopic.GOAL && listOf("it", "that", "this one").any(normalized::contains)
        return hasResolvedGoal || followUp || goalKeywords.any(normalized::contains)
    }

    private fun isTopCategoryQuery(normalized: String): Boolean {
        return normalized.contains("top expense category") ||
            normalized.contains("top category") ||
            normalized.contains("highest category")
    }

    private fun isComparisonQuery(normalized: String, memory: AssistantConversationMemory): Boolean {
        if (normalized.contains("compare")) return true
        if (normalized.contains("vs ")) return true
        if (normalized.contains("last month") || normalized.contains("last week") || normalized.contains("last year")) return true
        if (
            listOf("more", "less", "difference", "different").any(normalized::contains) &&
            listOf("month", "week", "year").any(normalized::contains)
        ) return true
        return memory.currentTopic in setOf(AssistantTopic.EXPENSES, AssistantTopic.INCOME, AssistantTopic.FREE_CASH) &&
            normalized.contains("that")
    }

    private fun isIncomeQuery(normalized: String): Boolean {
        return normalized.contains("income")
    }

    private fun isExpensesQuery(normalized: String): Boolean {
        return normalized.contains("expense") || normalized.contains("spending") || normalized.contains("spent")
    }

    private fun isFreeCashQuery(normalized: String): Boolean {
        return normalized.contains("free cash") ||
            normalized.contains("cash left") ||
            normalized.contains("how much cash") ||
            (normalized.contains("how much left") && !normalized.contains("goal"))
    }

    private fun resolveGoal(
        normalized: String,
        goals: List<GoalEntity>,
        memory: AssistantConversationMemory,
    ): GoalEntity? {
        if (goals.isEmpty()) return null

        val ordinalGoal = when {
            normalized.contains("first") || normalized.contains("1st") -> goals.getOrNull(0)
            normalized.contains("second") || normalized.contains("2nd") -> goals.getOrNull(1)
            normalized.contains("third") || normalized.contains("3rd") -> goals.getOrNull(2)
            else -> null
        }
        if (ordinalGoal != null) return ordinalGoal

        goals.firstOrNull { normalized.contains(it.title.lowercase(Locale.US)) }?.let { return it }

        if (normalized.contains("closest goal") || normalized.contains("nearest goal") || normalized.contains("primary goal") || normalized.contains("current goal")) {
            return goals.first()
        }

        return null
    }

    private fun resolveComparisonPeriod(
        normalized: String,
        memory: AssistantConversationMemory,
    ): ComparisonPeriod {
        return when {
            normalized.contains("week") -> ComparisonPeriod.WEEK
            normalized.contains("year") -> ComparisonPeriod.YEAR
            normalized.contains("month") -> ComparisonPeriod.MONTH
            memory.comparisonPeriod != null -> memory.comparisonPeriod
            else -> ComparisonPeriod.MONTH
        }
    }

    private fun resolveComparisonMetric(
        normalized: String,
        memory: AssistantConversationMemory,
    ): ComparisonMetric {
        return when {
            normalized.contains("income") -> ComparisonMetric.INCOME
            normalized.contains("free cash") || normalized.contains("cash left") -> ComparisonMetric.FREE_CASH
            normalized.contains("expense") || normalized.contains("spending") || normalized.contains("spent") -> ComparisonMetric.EXPENSES
            memory.comparisonMetric != null -> memory.comparisonMetric
            memory.currentTopic == AssistantTopic.INCOME -> ComparisonMetric.INCOME
            memory.currentTopic == AssistantTopic.FREE_CASH -> ComparisonMetric.FREE_CASH
            else -> ComparisonMetric.EXPENSES
        }
    }

    private fun resolveComparisonCategory(
        normalized: String,
        expenses: List<ExpenseEntity>,
        memory: AssistantConversationMemory,
    ): String? {
        val categories = expenses.map { it.category }.distinct()
        categories.firstOrNull { category ->
            normalized.contains(category.lowercase(Locale.US))
        }?.let { return it }

        return if (normalized.contains("that") || normalized.contains("this category")) {
            memory.comparisonCategory
        } else {
            null
        }
    }

    private fun goalDetail(
        goal: GoalEntity,
        snapshot: AssistantFinanceSnapshot,
    ): GoalDetail {
        val remaining = max(goal.targetAmountLkr - goal.currentSavedLkr, 0.0)
        val monthsRemaining = monthsUntil(goal.deadlineAt).coerceAtLeast(1)
        return GoalDetail(
            targetLabel = formatCurrency(goal.targetAmountLkr, snapshot),
            savedLabel = formatCurrency(goal.currentSavedLkr, snapshot),
            remainingLabel = formatCurrency(remaining, snapshot),
            deadlineLabel = "$monthsRemaining months remaining",
            monthlyNeedLabel = formatCurrency(remaining / monthsRemaining, snapshot),
            monthlyContributionLabel = formatCurrency(goal.monthlyContributionLkr, snapshot),
        )
    }

    private fun compareSpending(
        expenses: List<ExpenseEntity>,
        snapshot: AssistantFinanceSnapshot,
        period: ComparisonPeriod,
    ): SpendingComparison? {
        val now = Calendar.getInstance()
        val currentRange = currentRange(now, period)
        val previousRange = previousRange(currentRange.start, period)

        val currentAmount = expenses
            .filter { it.spentAt in currentRange.start until currentRange.endExclusive }
            .sumOf { it.amountLkr }
        val previousAmount = expenses
            .filter { it.spentAt in previousRange.start until previousRange.endExclusive }
            .sumOf { it.amountLkr }

        return buildComparison(period, currentAmount, previousAmount, snapshot)
    }

    private fun compareIncome(
        incomes: List<IncomeEntity>,
        snapshot: AssistantFinanceSnapshot,
        period: ComparisonPeriod,
    ): SpendingComparison? {
        val now = Calendar.getInstance()
        val currentRange = currentRange(now, period)
        val previousRange = previousRange(currentRange.start, period)

        val currentAmount = incomes
            .filter { it.receivedAt in currentRange.start until currentRange.endExclusive }
            .sumOf { it.amountLkr }
        val previousAmount = incomes
            .filter { it.receivedAt in previousRange.start until previousRange.endExclusive }
            .sumOf { it.amountLkr }

        return buildComparison(period, currentAmount, previousAmount, snapshot)
    }

    private fun compareFreeCash(
        snapshot: AssistantFinanceSnapshot,
        period: ComparisonPeriod,
    ): String {
        val hasReservedGoals = snapshot.goals.any { it.currentSavedLkr > 0.0 || it.monthlyContributionLkr > 0.0 }
        if (hasReservedGoals) {
            return "I can compare current free cash, but period-to-period free cash is not reliable yet because goal reserves are stored as a current total, not a full timeline."
        }

        val now = Calendar.getInstance()
        val currentRange = currentRange(now, period)
        val previousRange = previousRange(currentRange.start, period)
        val actualExpenses = snapshot.expenses.filterNot { it.isRecurringTemplate }

        val currentAmount = snapshot.incomes
            .filter { it.receivedAt in currentRange.start until currentRange.endExclusive }
            .sumOf { it.amountLkr } - actualExpenses
            .filter { it.spentAt in currentRange.start until currentRange.endExclusive }
            .sumOf { it.amountLkr }

        val previousAmount = snapshot.incomes
            .filter { it.receivedAt in previousRange.start until previousRange.endExclusive }
            .sumOf { it.amountLkr } - actualExpenses
            .filter { it.spentAt in previousRange.start until previousRange.endExclusive }
            .sumOf { it.amountLkr }

        return buildComparison(period, currentAmount, previousAmount, snapshot)?.toMessage("free cash")
            ?: "I do not have enough recorded income and expense data to compare free cash for that period yet."
    }

    private fun buildComparison(
        period: ComparisonPeriod,
        currentAmount: Double,
        previousAmount: Double,
        snapshot: AssistantFinanceSnapshot,
    ): SpendingComparison? {
        if (currentAmount == 0.0 && previousAmount == 0.0) {
            return null
        }

        val difference = currentAmount - previousAmount
        val trend = when {
            difference > 0.01 -> "up"
            difference < -0.01 -> "down"
            else -> "flat"
        }
        val deltaPercent = if (previousAmount == 0.0) null else (difference / previousAmount) * 100.0

        val currentLabel = when (period) {
            ComparisonPeriod.WEEK -> "This week"
            ComparisonPeriod.MONTH -> "This month"
            ComparisonPeriod.YEAR -> "This year"
        }
        val previousLabel = when (period) {
            ComparisonPeriod.WEEK -> "Last week"
            ComparisonPeriod.MONTH -> "Last month"
            ComparisonPeriod.YEAR -> "Last year"
        }

        val amountDelta = formatSignedCurrency(difference, snapshot)
        val note = if (deltaPercent == null) {
            "$amountDelta vs $previousLabel."
        } else {
            "$amountDelta and ${formatSignedPercent(deltaPercent)} vs $previousLabel."
        }

        return SpendingComparison(
            currentLabel = currentLabel,
            previousLabel = previousLabel,
            currentAmountLabel = formatCurrency(currentAmount, snapshot),
            trend = trend,
            comparisonNote = note,
        )
    }

    private fun currentRange(
        now: Calendar,
        period: ComparisonPeriod,
    ): TimeRange {
        val start = (now.clone() as Calendar).apply {
            when (period) {
                ComparisonPeriod.WEEK -> {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }
                ComparisonPeriod.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
                ComparisonPeriod.YEAR -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val end = (start.clone() as Calendar).apply {
            when (period) {
                ComparisonPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, 1)
                ComparisonPeriod.MONTH -> add(Calendar.MONTH, 1)
                ComparisonPeriod.YEAR -> add(Calendar.YEAR, 1)
            }
        }

        return TimeRange(start.timeInMillis, end.timeInMillis)
    }

    private fun previousRange(
        currentStart: Long,
        period: ComparisonPeriod,
    ): TimeRange {
        val start = Calendar.getInstance().apply { timeInMillis = currentStart }
        when (period) {
            ComparisonPeriod.WEEK -> start.add(Calendar.WEEK_OF_YEAR, -1)
            ComparisonPeriod.MONTH -> start.add(Calendar.MONTH, -1)
            ComparisonPeriod.YEAR -> start.add(Calendar.YEAR, -1)
        }
        val end = Calendar.getInstance().apply { timeInMillis = currentStart }
        return TimeRange(start.timeInMillis, end.timeInMillis)
    }

    private fun monthsUntil(deadlineAt: Long): Int {
        val now = Calendar.getInstance()
        val deadline = Calendar.getInstance().apply { timeInMillis = deadlineAt }
        val yearDiff = deadline.get(Calendar.YEAR) - now.get(Calendar.YEAR)
        val monthDiff = deadline.get(Calendar.MONTH) - now.get(Calendar.MONTH)
        return yearDiff * 12 + monthDiff
    }

    private fun formatCurrency(
        amountLkr: Double,
        snapshot: AssistantFinanceSnapshot,
    ): String {
        val currencyCode = snapshot.preferredCurrency.uppercase(Locale.US)
        if (currencyCode == "LKR") {
            return CurrencyConverter.format(amountLkr, currencyCode)
        }

        val rateToLkr = snapshot.ratesToLkr[currencyCode] ?: return CurrencyConverter.format(amountLkr, "LKR")
        return CurrencyConverter.format(CurrencyConverter.fromLkr(amountLkr, rateToLkr), currencyCode)
    }

    private fun formatSignedCurrency(
        amountLkr: Double,
        snapshot: AssistantFinanceSnapshot,
    ): String {
        val amountLabel = formatCurrency(abs(amountLkr), snapshot)
        return if (amountLkr >= 0.0) "+$amountLabel" else "-$amountLabel"
    }

    private fun formatSignedPercent(value: Double): String {
        return String.format(Locale.US, "%+.1f%%", value)
    }

    private data class GoalDetail(
        val targetLabel: String,
        val savedLabel: String,
        val remainingLabel: String,
        val deadlineLabel: String,
        val monthlyNeedLabel: String,
        val monthlyContributionLabel: String,
    )

    private data class SpendingComparison(
        val currentLabel: String,
        val previousLabel: String,
        val currentAmountLabel: String,
        val trend: String,
        val comparisonNote: String,
    ) {
        fun toMessage(subject: String): String {
            return buildString {
                append("$currentLabel $subject is $currentAmountLabel. ")
                append("Compared with ${previousLabel.lowercase(Locale.US)}, it is $trend. ")
                append(comparisonNote)
            }
        }
    }

    private data class TimeRange(
        val start: Long,
        val endExclusive: Long,
    )
}
