package com.example.finance_management_system.assistant

import com.example.finance_management_system.data.local.entity.ExpenseEntity
import com.example.finance_management_system.data.local.entity.GoalEntity
import com.example.finance_management_system.data.local.entity.IncomeEntity

enum class ComparisonMetric {
    EXPENSES,
    INCOME,
    FREE_CASH,
}

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
}

enum class AssistantTopic {
    GOAL,
    COMPARISON,
    INCOME,
    EXPENSES,
    FREE_CASH,
}

enum class ComparisonPeriod {
    WEEK,
    MONTH,
    YEAR,
}

data class AssistantMessage(
    val id: String,
    val role: AssistantMessageRole,
    val text: String,
    val timestamp: Long,
)

data class AssistantUiState(
    val isOpen: Boolean = false,
    val draft: String = "",
    val messages: List<AssistantMessage> = emptyList(),
)

data class AssistantConversationMemory(
    val currentTopic: AssistantTopic? = null,
    val selectedGoalId: String? = null,
    val comparisonPeriod: ComparisonPeriod? = null,
    val comparisonMetric: ComparisonMetric? = null,
    val comparisonCategory: String? = null,
)

data class AssistantFinanceSnapshot(
    val incomes: List<IncomeEntity>,
    val expenses: List<ExpenseEntity>,
    val goals: List<GoalEntity>,
    val preferredCurrency: String,
    val ratesToLkr: Map<String, Double>,
)

data class AssistantReply(
    val message: String,
    val memory: AssistantConversationMemory,
)
