package com.example.finance_management_system.ui


import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.finance_management_system.navigation.FinancialTrackerNavHost

@Composable
fun FinancialTrackerApp() {
    val navController = rememberNavController()
    FinancialTrackerNavHost(navController = navController)
}