package com.example.finance_management_system.ui.state

import com.example.finance_management_system.model.SummaryCard
import com.example.finance_management_system.model.ChartDatum
import com.example.finance_management_system.model.GoalOverview
import com.example.finance_management_system.model.InsightItem
import com.example.finance_management_system.model.TransactionItem

data class DashboardUiState(
    val welcomeTitle: String = "Money overview",
    val summaryCards: List<SummaryCard> = emptyList(),
    val insightItems: List<InsightItem> = emptyList(),
    val expenseChart: List<ChartDatum> = emptyList(),
    val incomeChart: List<ChartDatum> = emptyList(),
    val spendingSplitChart: List<ChartDatum> = emptyList(),
    val spendVsLeftChart: List<ChartDatum> = emptyList(),
    val spendVsLeftMessage: String = "",
    val featuredGoal: GoalOverview? = null,
    val recentTransactions: List<TransactionItem> = emptyList(),
)
