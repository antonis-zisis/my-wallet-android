package com.mywallet.android.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mywallet.android.graphql.GetSubscriptionsQuery
import com.mywallet.android.ui.components.ConfirmDialog
import com.mywallet.android.ui.components.EmptyState
import com.mywallet.android.ui.components.ErrorMessage
import com.mywallet.android.ui.components.LoadingScreen
import com.mywallet.android.util.formatDate
import com.mywallet.android.util.formatMoney
import com.mywallet.android.util.getNextRenewalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Subscriptions") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddForm) {
                Icon(Icons.Default.Add, contentDescription = "Add subscription")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.error != null -> ErrorMessage(
                message = state.error!!,
                onRetry = viewModel::loadAll,
                modifier = Modifier.padding(padding),
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Active section header with summary
                    item {
                        val totalMonthly = state.activeSubscriptions.sumOf { it.monthlyCost }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Active (${state.activeSubscriptions.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatMoney(totalMonthly) + "/mo",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }

                    if (state.activeSubscriptions.isEmpty()) {
                        item { EmptyState("No active subscriptions") }
                    }

                    items(state.activeSubscriptions, key = { it.id }) { sub ->
                        SubscriptionCard(
                            sub = sub,
                            onEdit = { viewModel.showEditForm(sub) },
                            onCancel = { viewModel.confirmCancel(sub) },
                            onDelete = { viewModel.confirmDelete(sub) },
                        )
                    }

                    // Inactive section toggle
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = viewModel::toggleShowInactive,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Inactive (${state.inactiveTotalCount})")
                                Icon(
                                    if (state.showInactive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    if (state.showInactive) {
                        if (state.inactiveSubscriptions.isEmpty()) {
                            item { EmptyState("No inactive subscriptions") }
                        }
                        items(state.inactiveSubscriptions, key = { it.id }) { sub ->
                            SubscriptionCard(
                                sub = sub,
                                onEdit = { viewModel.showEditForm(sub) },
                                onCancel = null,
                                onDelete = { viewModel.confirmDelete(sub) },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Subscription form
    if (state.showForm) {
        SubscriptionFormDialog(
            state = state.form,
            isSaving = state.isSaving,
            onNameChange = viewModel::onFormNameChange,
            onAmountChange = viewModel::onFormAmountChange,
            onBillingCycleChange = viewModel::onFormBillingCycleChange,
            onStartDateChange = viewModel::onFormStartDateChange,
            onEndDateChange = viewModel::onFormEndDateChange,
            onSave = viewModel::saveSubscription,
            onDismiss = viewModel::dismissForm,
        )
    }

    // Cancel confirm
    if (state.subscriptionToCancel != null) {
        ConfirmDialog(
            title = "Cancel Subscription",
            message = "Cancel \"${state.subscriptionToCancel!!.name}\"? It will be marked inactive.",
            confirmLabel = "Cancel Subscription",
            isDestructive = false,
            isLoading = state.isCancelling,
            onConfirm = viewModel::cancelSubscription,
            onDismiss = viewModel::dismissCancel,
        )
    }

    // Delete confirm
    if (state.subscriptionToDelete != null) {
        ConfirmDialog(
            title = "Delete Subscription",
            message = "Permanently delete \"${state.subscriptionToDelete!!.name}\"?",
            confirmLabel = "Delete",
            isDestructive = true,
            isLoading = state.isDeleting,
            onConfirm = viewModel::deleteSubscription,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun SubscriptionCard(
    sub: GetSubscriptionsQuery.Item,
    onEdit: () -> Unit,
    onCancel: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val nextRenewal = getNextRenewalDate(sub.startDate, sub.billingCycle)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (sub.isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sub.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${formatMoney(sub.amount)}/${sub.billingCycle.lowercase()} " +
                            "(${formatMoney(sub.monthlyCost)}/mo)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (nextRenewal != null && sub.isActive) {
                    Text(
                        text = "Renews: $nextRenewal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!sub.isActive && sub.endDate != null) {
                    Text(
                        text = "Ended: ${formatDate(sub.endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Action menu
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { showMenu = false; onEdit() },
                )
                if (onCancel != null) {
                    DropdownMenuItem(
                        text = { Text("Cancel") },
                        leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) },
                        onClick = { showMenu = false; onCancel() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionFormDialog(
    state: SubscriptionFormState,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onBillingCycleChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "Add Subscription" else "Edit Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("MONTHLY", "YEARLY").forEachIndexed { index, cycle ->
                        SegmentedButton(
                            selected = state.billingCycle == cycle,
                            onClick = { onBillingCycleChange(cycle) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) {
                            Text(cycle.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                OutlinedTextField(
                    value = state.startDate,
                    onValueChange = onStartDateChange,
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.endDate,
                    onValueChange = onEndDateChange,
                    label = { Text("End Date (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}
