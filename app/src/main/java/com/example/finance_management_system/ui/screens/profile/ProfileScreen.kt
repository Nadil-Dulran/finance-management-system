package com.example.finance_management_system.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance_management_system.R
import com.example.finance_management_system.ui.components.AppScaffold
import com.example.finance_management_system.ui.components.FrostedBadge
import com.example.finance_management_system.ui.components.GradientHeroCard
import com.example.finance_management_system.ui.components.MetricCard
import com.example.finance_management_system.ui.state.ProfileUiState

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onOpenSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    AppScaffold(
        title = stringResource(R.string.profile_title),
        currentRoute = currentRoute,
        showBottomBar = true,
        onBottomNavClick = onBottomNavClick,
        onAddIncomeClick = onAddIncomeClick,
        onAddExpenseClick = onAddExpenseClick,
        showTopBar = false,
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GradientHeroCard(
                eyebrow = "PROFILE",
                title = uiState.preferredCurrency,
                amount = uiState.displayName,
                subtitle = uiState.email,
                modifier = Modifier.fillMaxWidth(),
                accent = {
                    FrostedBadge(
                        text = "Account",
                        icon = Icons.Outlined.Person,
                    )
                },
            )

            uiState.message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            MetricCard(
                title = "Notification Access",
                amount = if (uiState.notificationCaptureEnabled) "Enabled" else "Disabled",
                description = if (uiState.notificationCaptureEnabled) {
                    stringResource(R.string.profile_notifications_enabled)
                } else {
                    stringResource(R.string.profile_notifications_disabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Card(
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_account_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                    )
                    Text(
                        text = stringResource(R.string.profile_preferred_currency, uiState.preferredCurrency),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_how_it_works_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                    )
                    Text(
                        text = stringResource(R.string.profile_how_it_works_intro),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.profile_how_it_works_step_1))
                    Text(stringResource(R.string.profile_how_it_works_step_2))
                    Text(stringResource(R.string.profile_how_it_works_step_3))
                    Text(stringResource(R.string.profile_how_it_works_step_4))
                    Text(stringResource(R.string.profile_how_it_works_step_5))
                }
            }

            Button(onClick = onOpenNotificationAccess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_notification_access))
            }
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_change_currency_preference))
            }
            Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_sign_out))
            }
            Button(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isDeletingAccount,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(if (uiState.isDeletingAccount) "Deleting Account..." else "Delete Account")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account and all your data?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
