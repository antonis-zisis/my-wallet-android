package com.mywallet.android.ui.reports

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mywallet.android.graphql.GetReportQuery
import com.mywallet.android.ui.components.ConfirmDialog
import com.mywallet.android.ui.components.ErrorMessage
import com.mywallet.android.ui.components.LoadingScreen
import com.mywallet.android.ui.theme.ExpenseRed
import com.mywallet.android.ui.theme.IncomeGreen
import com.mywallet.android.util.formatDate
import com.mywallet.android.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: String,
    onNavigateBack: () -> Unit,
    viewModel: ReportDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(reportId) { viewModel.init(reportId) }
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if (state.isEditingTitle) {
                        OutlinedTextField(
                            value = state.editedTitle,
                            onValueChange = viewModel::onTitleChange,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(state.report?.title ?: "Report")
                            if (state.report?.isLocked == true) {
                                Spacer(modifier = Modifier.size(8.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEditingTitle) {
                        TextButton(
                            onClick = viewModel::saveTitle,
                            enabled = !state.isSavingTitle,
                        ) {
                            Text("Save")
                        }
                        TextButton(onClick = viewModel::cancelEditTitle) {
                            Text("Cancel")
                        }
                    } else {
                        val isLocked = state.report?.isLocked == true
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                enabled = !isLocked,
                                onClick = {
                                    showMenu = false
                                    viewModel.startEditTitle()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isLocked) "Unlock Report" else "Lock Report") },
                                leadingIcon = {
                                    Icon(
                                        if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleLock()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Report", color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error,
                                    )
                                },
                                enabled = !isLocked,
                                onClick = {
                                    showMenu = false
                                    viewModel.showDeleteReport()
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!state.isLoading && state.report != null && state.report?.isLocked != true) {
                FloatingActionButton(onClick = viewModel::showAddTransaction) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.error != null -> ErrorMessage(
                message = state.error!!,
                onRetry = viewModel::loadReport,
                modifier = Modifier.padding(padding),
            )
            state.report != null -> {
                val report = state.report!!
                val income = report.transactions.filter { it.type.rawValue == "INCOME" }.sumOf { it.amount }
                val expenses = report.transactions.filter { it.type.rawValue == "EXPENSE" }.sumOf { it.amount }
                val net = income - expenses

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                SummaryItem("Income", formatMoney(income), IncomeGreen)
                                SummaryItem("Expenses", formatMoney(expenses), ExpenseRed)
                                SummaryItem(
                                    "Net Balance",
                                    formatMoney(net),
                                    if (net >= 0) IncomeGreen else ExpenseRed,
                                )
                            }
                        }
                    }

                    // Transactions header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Transactions (${report.transactions.size})",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }

                    if (report.transactions.isEmpty()) {
                        item {
                            Text(
                                "No transactions yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }

                    items(
                        report.transactions.sortedByDescending { it.date },
                        key = { it.id }
                    ) { tx ->
                        TransactionRow(
                            transaction = tx,
                            isLocked = report.isLocked,
                            onEdit = { viewModel.showEditTransaction(tx) },
                            onDelete = { viewModel.confirmDeleteTransaction(tx) },
                        )
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Transaction form dialog
    if (state.showTransactionForm) {
        TransactionFormDialog(
            state = state.transactionForm,
            isSaving = state.isSavingTransaction,
            onTypeChange = viewModel::onTransactionTypeChange,
            onAmountChange = viewModel::onTransactionAmountChange,
            onDescriptionChange = viewModel::onTransactionDescriptionChange,
            onCategoryChange = viewModel::onTransactionCategoryChange,
            onDateChange = viewModel::onTransactionDateChange,
            onSave = viewModel::saveTransaction,
            onDismiss = viewModel::dismissTransactionForm,
        )
    }

    // Delete transaction dialog
    if (state.transactionToDelete != null) {
        ConfirmDialog(
            title = "Delete Transaction",
            message = "Delete \"${state.transactionToDelete!!.description}\"? This cannot be undone.",
            confirmLabel = "Delete",
            isDestructive = true,
            isLoading = state.isDeletingTransaction,
            onConfirm = viewModel::deleteTransaction,
            onDismiss = viewModel::dismissDeleteTransaction,
        )
    }

    // Delete report dialog
    if (state.showDeleteReport) {
        ConfirmDialog(
            title = "Delete Report",
            message = "Delete \"${state.report?.title}\"? All transactions will be permanently deleted.",
            confirmLabel = "Delete",
            isDestructive = true,
            isLoading = state.isDeletingReport,
            onConfirm = { viewModel.deleteReport(onNavigateBack) },
            onDismiss = viewModel::dismissDeleteReport,
        )
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: GetReportQuery.Transaction,
    isLocked: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isIncome = transaction.type.rawValue == "INCOME"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${transaction.description} · ${formatDate(transaction.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = (if (isIncome) "+" else "-") + formatMoney(transaction.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) IncomeGreen else ExpenseRed,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (!isLocked) {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    state: TransactionFormState,
    isSaving: Boolean,
    onTypeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val categories = if (state.type == "INCOME") INCOME_CATEGORIES else EXPENSE_CATEGORIES
    val selectedCategory = if (state.category.isBlank() || state.category !in categories) {
        categories.first()
    } else {
        state.category
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type toggle
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("EXPENSE", "INCOME").forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) {
                            Text(type.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

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

                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Category dropdown (using exposed dropdown)
                CategoryDropdown(
                    selected = selectedCategory,
                    options = categories,
                    onSelect = onCategoryChange,
                )

                OutlinedTextField(
                    value = state.date,
                    onValueChange = onDateChange,
                    label = { Text("Date (YYYY-MM-DD)") },
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
