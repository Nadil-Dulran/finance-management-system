package com.example.finance_management_system;

import android.app.Application
import com.example.finance_management_system.data.AppContainer

class MyFinancialTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}
