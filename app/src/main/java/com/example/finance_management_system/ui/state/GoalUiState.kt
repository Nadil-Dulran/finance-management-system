package com.example.finance_management_system.ui.state

import com.example.finance_management_system.model.GoalOverview

data class GoalUiState(
    val overview: GoalOverview? = null,
    val goals: List<GoalOverview> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
)
