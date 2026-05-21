package com.example.finance_management_system.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance_management_system.R
import com.example.finance_management_system.ui.components.AppScaffold
import com.example.finance_management_system.ui.components.FrostedBadge
import com.example.finance_management_system.ui.components.GradientHeroCard
import com.example.finance_management_system.ui.state.AuthUiState

@Composable
fun ResetPasswordScreen(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    AppScaffold(
        title = stringResource(R.string.reset_password_title),
        currentRoute = null,
        showBottomBar = false,
        showTopBar = false,
        onBottomNavClick = {},
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GradientHeroCard(
                eyebrow = "ACCOUNT RECOVERY",
                title = stringResource(R.string.reset_password_headline),
                amount = "Reset Access",
                subtitle = stringResource(R.string.reset_password_copy),
                modifier = Modifier.fillMaxWidth(),
                accent = {
                    FrostedBadge(
                        text = "Email Link",
                        icon = Icons.Outlined.MailOutline,
                    )
                },
            )

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reset_password_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.reset_password_instructions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = { Text(stringResource(R.string.field_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    uiState.errorMessage?.let { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(12.dp),
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    uiState.successMessage?.let { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(12.dp),
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Button(
                        onClick = onSendReset,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(vertical = 2.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.button_send_reset_link))
                        }
                    }

                    OutlinedButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                    ) {
                        Text(stringResource(R.string.button_back_to_sign_in))
                    }
                }
            }
        }
    }
}
