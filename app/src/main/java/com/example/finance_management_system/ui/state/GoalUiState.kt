package com.tetrova_flowledger.ui.state

import com.tetrova_flowledger.model.GoalOverview

data class GoalUiState(
    val overview: GoalOverview? = null,
    val goals: List<GoalOverview> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
)
