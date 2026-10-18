package com.tetrova_flowledger.ui.state

import com.tetrova_flowledger.model.SummaryCard
import com.tetrova_flowledger.model.ChartDatum
import com.tetrova_flowledger.model.GoalOverview
import com.tetrova_flowledger.model.InsightItem
import com.tetrova_flowledger.model.TransactionItem

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
