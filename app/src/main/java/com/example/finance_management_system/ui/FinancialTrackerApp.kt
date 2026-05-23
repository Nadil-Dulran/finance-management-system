package com.example.finance_management_system.ui


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.finance_management_system.navigation.AppDestination
import com.example.finance_management_system.navigation.FinancialTrackerNavHost
import com.example.finance_management_system.ui.components.AssistantOverlay
import com.example.finance_management_system.viewmodel.AssistantViewModel

@Composable
fun FinancialTrackerApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val assistantViewModel: AssistantViewModel = hiltViewModel()
    val assistantUiState = assistantViewModel.uiState.collectAsStateWithLifecycle().value
    val assistantVisibleRoutes = setOf(
        AppDestination.Dashboard.route,
        AppDestination.AddIncome.route,
        AppDestination.AddExpense.route,
        AppDestination.Transactions.route,
        AppDestination.RecurringBills.route,
        AppDestination.Goal.route,
        AppDestination.Settings.route,
        AppDestination.Profile.route,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        FinancialTrackerNavHost(navController = navController)
        AssistantOverlay(
            uiState = assistantUiState,
            visible = currentRoute in assistantVisibleRoutes,
            onOpen = assistantViewModel::openAssistant,
            onClose = assistantViewModel::closeAssistant,
            onDraftChange = assistantViewModel::updateDraft,
            onSend = assistantViewModel::sendMessage,
        )
    }
}
