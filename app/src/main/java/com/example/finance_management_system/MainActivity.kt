package com.example.finance_management_system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.finance_management_system.data.StartupCoordinator
import com.example.finance_management_system.ui.FinancialTrackerApp
import com.example.finance_management_system.ui.theme.MyFinancialTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var startupCoordinator: StartupCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFinancialTrackerTheme {
                FinancialTrackerApp(startupCoordinator = startupCoordinator)
            }
        }
        window.decorView.post {
            startupCoordinator.runDeferredStartupWork()
        }
    }
}
