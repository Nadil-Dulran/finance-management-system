package com.example.finance_management_system.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.finance_management_system.R
import com.example.finance_management_system.model.supportedCurrencies
import com.example.finance_management_system.ui.components.AppScaffold
import com.example.finance_management_system.ui.components.DropdownField
import com.example.finance_management_system.ui.state.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSaveCurrency: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedCurrency by remember { mutableStateOf(uiState.preferredCurrency) }

    LaunchedEffect(uiState.preferredCurrency) {
        selectedCurrency = uiState.preferredCurrency
    }

    AppScaffold(
        title = stringResource(R.string.settings_title),
        currentRoute = null,
        showBottomBar = false,
        onBottomNavClick = {},
    ) { modifier ->
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_copy),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DropdownField(
                label = stringResource(R.string.settings_display_currency),
                value = selectedCurrency,
                options = supportedCurrencies,
                onValueSelected = { selectedCurrency = it },
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = { onSaveCurrency(selectedCurrency) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving,
            ) {
                Text(
                    if (uiState.isSaving) stringResource(R.string.button_saving)
                    else stringResource(R.string.button_save_preference),
                )
            }
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.button_back))
            }
        }
    }
}
