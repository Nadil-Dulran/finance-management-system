package com.example.finance_management_system.navigation

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.finance_management_system.R
import com.example.finance_management_system.ui.screens.addexpense.AddExpenseScreen
import com.example.finance_management_system.ui.screens.addincome.AddIncomeScreen
import com.example.finance_management_system.ui.screens.auth.LandingScreen
import com.example.finance_management_system.ui.screens.auth.LoginScreen
import com.example.finance_management_system.ui.screens.auth.ResetPasswordScreen
import com.example.finance_management_system.ui.screens.auth.RegisterScreen
import com.example.finance_management_system.ui.screens.dashboard.DashboardScreen
import com.example.finance_management_system.ui.screens.goal.GoalScreen
import com.example.finance_management_system.ui.screens.profile.ProfileScreen
import com.example.finance_management_system.ui.screens.settings.SettingsScreen
import com.example.finance_management_system.ui.screens.transactions.TransactionsScreen
import com.example.finance_management_system.ui.screens.transactions.RecurringBillsScreen
import com.example.finance_management_system.viewmodel.AddExpenseViewModel
import com.example.finance_management_system.viewmodel.AddIncomeViewModel
import com.example.finance_management_system.viewmodel.AuthViewModel
import com.example.finance_management_system.viewmodel.DashboardViewModel
import com.example.finance_management_system.viewmodel.GoalViewModel
import com.example.finance_management_system.viewmodel.ProfileViewModel
import com.example.finance_management_system.viewmodel.SettingsViewModel
import com.example.finance_management_system.viewmodel.TransactionsViewModel
import com.example.finance_management_system.viewmodel.RecurringBillsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun FinancialTrackerNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("476845779069-n3o7a0nr22a4c5019vj4slagm1aj717i.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val authViewModel: AuthViewModel = hiltViewModel()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                authViewModel.loginWithGoogle(idToken) {
                    navController.navigate(AppDestination.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e("Auth", "Google sign in failed", e)
            // The AuthViewModel won't know about this failure unless we tell it
            // or if we rely on the internal ApiException. 
            // ApiException 10 is usually configuration.
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Landing.route,
    ) {
        composable(AppDestination.Landing.route) {
            LandingScreen(
                onGetStartedClick = {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(AppDestination.Landing.route) {
                            inclusive = true
                        }
                    }
                },
                onLoginClick = {
                    navController.navigate(AppDestination.Login.route)
                },
            )
        }

        composable(AppDestination.Login.route) {
            val uiState = authViewModel.uiState.collectAsStateWithLifecycle().value
            LoginScreen(
                uiState = uiState,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onLogin = {
                    authViewModel.login {
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(AppDestination.Login.route) {
                                inclusive = true
                            }
                        }
                    }
                },
                onGoogleLogin = {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onForgotPasswordClick = {
                    navController.navigate(AppDestination.ResetPassword.route)
                },
                onRegisterClick = {
                    navController.navigate(AppDestination.Register.route)
                },
            )
        }

        composable(AppDestination.Register.route) {
            val uiState = authViewModel.uiState.collectAsStateWithLifecycle().value
            RegisterScreen(
                uiState = uiState,
                onNameChange = authViewModel::updateName,
                onEmailChange = authViewModel::updateEmail,
                onPasswordChange = authViewModel::updatePassword,
                onRegister = {
                    authViewModel.register {
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(AppDestination.Login.route) {
                                inclusive = true
                            }
                        }
                    }
                },
                onGoogleRegister = {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                onBackToLogin = {
                    navController.popBackStack()
                },
            )
        }

        composable(AppDestination.ResetPassword.route) {
            val uiState = authViewModel.uiState.collectAsStateWithLifecycle().value
            ResetPasswordScreen(
                uiState = uiState,
                onEmailChange = authViewModel::updateEmail,
                onSendReset = authViewModel::sendPasswordReset,
                onBackToLogin = {
                    navController.popBackStack()
                },
            )
        }

        composable(AppDestination.Dashboard.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            DashboardScreen(
                uiState = uiState,
                onAddIncomeClick = { navController.navigate(AppDestination.AddIncome.route) },
                onAddExpenseClick = { navController.navigate(AppDestination.AddExpense.route) },
                onTransactionsClick = { navController.navigate(AppDestination.Transactions.route) },
                onRecurringBillsClick = { navController.navigate(AppDestination.RecurringBills.route) },
                onGoalClick = { navController.navigate(AppDestination.Goal.route) },
                onSettingsClick = { navController.navigate(AppDestination.Profile.route) },
                onBottomNavClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                currentRoute = AppDestination.Dashboard.route,
            )
        }

        composable(AppDestination.AddIncome.route) {
            val viewModel: AddIncomeViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            AddIncomeScreen(
                uiState = uiState,
                onSourceChange = viewModel::updateSource,
                onAmountChange = viewModel::updateAmount,
                onCurrencyChange = viewModel::updateCurrency,
                onNoteChange = viewModel::updateNote,
                onSave = {
                    viewModel.save {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                onBottomNavClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                currentRoute = null,
            )
        }

        composable(AppDestination.AddExpense.route) {
            val viewModel: AddExpenseViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            AddExpenseScreen(
                uiState = uiState,
                onCategoryChange = viewModel::updateCategory,
                onAmountChange = viewModel::updateAmount,
                onCurrencyChange = viewModel::updateCurrency,
                onTypeChange = viewModel::updateType,
                onPaymentMethodChange = viewModel::updatePaymentMethod,
                onRecurrenceChange = viewModel::updateRecurrence,
                onNoteChange = viewModel::updateNote,
                onSave = {
                    viewModel.save {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                onBottomNavClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                currentRoute = AppDestination.AddExpense.route,
            )
        }

        composable(AppDestination.Transactions.route) {
            val viewModel: TransactionsViewModel = hiltViewModel()
            val transactions = viewModel.transactions.collectAsStateWithLifecycle().value
            val detectedTransactions = viewModel.detectedTransactions.collectAsStateWithLifecycle().value
            val insights = viewModel.insights.collectAsStateWithLifecycle().value
            val spendingStatus = viewModel.spendingStatus.collectAsStateWithLifecycle().value
            val message = viewModel.message.collectAsStateWithLifecycle().value
            TransactionsScreen(
                transactions = transactions,
                detectedTransactions = detectedTransactions,
                insights = insights,
                spendingStatus = spendingStatus,
                message = message,
                onConfirmDetectedTransaction = viewModel::confirmDetectedTransaction,
                onIgnoreDetectedTransaction = viewModel::ignoreDetectedTransaction,
                onUpdateTransaction = viewModel::updateTransaction,
                onDeleteTransaction = viewModel::deleteTransaction,
                onConsumeMessage = viewModel::consumeMessage,
                onAddIncomeClick = { navController.navigate(AppDestination.AddIncome.route) },
                onAddExpenseClick = { navController.navigate(AppDestination.AddExpense.route) },
                onBottomNavClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                currentRoute = AppDestination.Transactions.route,
            )
        }

        composable(AppDestination.RecurringBills.route) {
            val viewModel: RecurringBillsViewModel = hiltViewModel()
            val bills = viewModel.recurringBills.collectAsStateWithLifecycle().value
            RecurringBillsScreen(
                bills = bills,
                onUpdateBill = viewModel::updateSubscription,
                onDeleteBill = viewModel::deleteSubscription,
                onAddExpenseClick = { navController.navigate(AppDestination.AddExpense.route) },
                onAddIncomeClick = { navController.navigate(AppDestination.AddIncome.route) },
                onBottomNavClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                currentRoute = AppDestination.RecurringBills.route,
            )
        }

        composable(AppDestination.Goal.route) {
            val viewModel: GoalViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            GoalScreen(
                uiState = uiState,
                onAddGoal = viewModel::addGoal,
                onUpdateGoal = viewModel::updateGoal,
                onDeleteGoal = viewModel::deleteGoal,
                onEmergencyWithdraw = viewModel::applyEmergencyWithdrawal,
                onAddIncomeClick = { navController.navigate(AppDestination.AddIncome.route) },
                onAddExpenseClick = { navController.navigate(AppDestination.AddExpense.route) },
                onBottomNavClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                currentRoute = AppDestination.Goal.route,
            )
        }

        composable(AppDestination.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            SettingsScreen(
                uiState = uiState,
                onSaveCurrency = viewModel::savePreferredCurrency,
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppDestination.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val context = LocalContext.current
            ProfileScreen(
                uiState = uiState,
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                onOpenNotificationAccess = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(AppDestination.Landing.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeleteAccount = {
                    viewModel.deleteAccount {
                        navController.navigate(AppDestination.Landing.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onAddIncomeClick = { navController.navigate(AppDestination.AddIncome.route) },
                onAddExpenseClick = { navController.navigate(AppDestination.AddExpense.route) },
                onBottomNavClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                currentRoute = AppDestination.Profile.route,
            )
        }
    }
}
