package com.example.finance_management_system

import android.os.Bundle
import androidx.activity.ComponentActivity

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.finance_management_system.data.AppContainer
import com.example.finance_management_system.ui.FinancialTrackerApp
import com.example.finance_management_system.ui.theme.MyFinancialTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFinancialTrackerTheme {
                FinancialTrackerApp()
            }
        }
        window.decorView.post {
            AppContainer.runDeferredStartupWork()
        }
    }
}