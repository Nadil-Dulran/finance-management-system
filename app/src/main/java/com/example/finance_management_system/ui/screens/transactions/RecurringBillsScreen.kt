package com.example.finance_management_system.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finance_management_system.R
import com.example.finance_management_system.model.AppDefaults
import com.example.finance_management_system.model.TransactionItem
import com.example.finance_management_system.model.recurrenceOptions
import com.example.finance_management_system.model.spendingTypes
import com.example.finance_management_system.model.supportedCurrencies
import com.example.finance_management_system.ui.components.AppScaffold
import com.example.finance_management_system.ui.components.DropdownField
import com.example.finance_management_system.ui.components.EmptyStateCard
import com.example.finance_management_system.ui.components.GradientHeroCard

@Composable
fun RecurringBillsScreen(
    bills: List<TransactionItem>,
    onUpdateBill: (TransactionItem) -> Unit,
    onDeleteBill: (TransactionItem) -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String,
) {
    var editingBill by remember { mutableStateOf<TransactionItem?>(null) }

    AppScaffold(
        title = "Recurring Bills",
        currentRoute = currentRoute,
        showBottomBar = true,
        onBottomNavClick = onBottomNavClick,
        onAddIncomeClick = onAddIncomeClick,
        onAddExpenseClick = onAddExpenseClick,
    ) { modifier ->
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                GradientHeroCard(
                    eyebrow = "MANAGEMENT",
                    title = "Active Subscriptions",
                    amount = "${bills.size}",
                    subtitle = "Manage your recurring payments and subscriptions here.",
                    modifier = Modifier.fillMaxWidth(),
                    accent = {
                        androidx.compose.material3.AssistChip(
                            onClick = {},
                            label = { Text("Active") },
                            leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Repeat, contentDescription = null) },
                        )
                    },
                )
            }

            if (bills.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No recurring bills",
                        subtitle = "Add an expense and set its recurrence to 'Monthly' or 'Yearly' to track it here.",
                    )
                }
            } else {
                items(bills) { bill ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(bill.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(bill.amountLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(bill.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (bill.note.isNotBlank()) {
                                Text(bill.note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                TextButton(onClick = { editingBill = bill }) {
                                    Text(stringResource(R.string.button_edit), color = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(onClick = { onDeleteBill(bill) }) {
                                    Text("Cancel Tracking", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingBill?.let { bill ->
        EditSubscriptionDialog(
            bill = bill,
            onDismiss = { editingBill = null },
            onSave = {
                onUpdateBill(it)
                editingBill = null
            }
        )
    }
}

@Composable
private fun EditSubscriptionDialog(
    bill: TransactionItem,
    onDismiss: () -> Unit,
    onSave: (TransactionItem) -> Unit,
) {
    //var title by remember(bill.id) { mutableStateOf(bill.title) }
    var amount by remember(bill.id) { mutableStateOf(bill.originalAmount.toString()) }
    var currency by remember(bill.id) { mutableStateOf(bill.originalCurrency) }
    var recurrenceType by remember(bill.id) { mutableStateOf(bill.recurrenceType ?: "Monthly") }
    var spendingType by remember(bill.id) { mutableStateOf(bill.spendingType ?: AppDefaults.DEFAULT_SPENDING_TYPE) }
    var note by remember(bill.id) { mutableStateOf(bill.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(bill.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.field_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownField(
                    label = stringResource(R.string.field_currency),
                    value = currency,
                    options = supportedCurrencies,
                    onValueSelected = { currency = it },
                )
                DropdownField(
                    label = "Recurrence",
                    value = recurrenceType,
                    options = recurrenceOptions.filter { it != "None" },
                    onValueSelected = { recurrenceType = it },
                )
                DropdownField(
                    label = stringResource(R.string.field_spending_type),
                    value = spendingType,
                    options = spendingTypes,
                    onValueSelected = { spendingType = it },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        bill.copy(
                            //title = title,
                            originalAmount = amount.toDoubleOrNull() ?: bill.originalAmount,
                            originalCurrency = currency,
                            recurrenceType = recurrenceType,
                            spendingType = spendingType,
                            note = note,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}
