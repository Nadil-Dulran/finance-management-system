package com.example.finance_management_system.ui


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.finance_management_system.data.StartupCoordinator
import com.example.finance_management_system.navigation.AppDestination
import com.example.finance_management_system.navigation.FinancialTrackerNavHost
import com.example.finance_management_system.ui.components.AssistantOverlay
import com.example.finance_management_system.viewmodel.AssistantViewModel

@Composable
fun FinancialTrackerApp(startupCoordinator: StartupCoordinator) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val assistantViewModel: AssistantViewModel = hiltViewModel()
    val assistantUiState = assistantViewModel.uiState.collectAsStateWithLifecycle().value
    val startDestination by produceState<String?>(initialValue = null, key1 = startupCoordinator) {
        value = if (startupCoordinator.shouldAutoLogin()) {
            AppDestination.Dashboard.route
        } else {
            AppDestination.Landing.route
        }
    }
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

    if (startDestination == null) {
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FinancialTrackerNavHost(
            navController = navController,
            startDestination = startDestination!!,
        )
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
