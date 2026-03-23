package com.mywallet.android.ui.networth

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.mywallet.android.graphql.GetNetWorthSnapshotsQuery
import com.mywallet.android.ui.components.ConfirmDialog
import com.mywallet.android.ui.components.EmptyState
import com.mywallet.android.ui.components.ErrorMessage
import com.mywallet.android.ui.components.LoadingScreen
import com.mywallet.android.ui.components.PaginationControls
import com.mywallet.android.ui.theme.ExpenseRed
import com.mywallet.android.ui.theme.IncomeGreen
import com.mywallet.android.ui.theme.NetWorthNegative
import com.mywallet.android.ui.theme.NetWorthPositive
import com.mywallet.android.util.formatDate
import com.mywallet.android.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: NetWorthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Net Worth") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showCreateDialog) {
                Icon(Icons.Default.Add, contentDescription = "New snapshot")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.error != null -> ErrorMessage(
                message = state.error!!,
                onRetry = { viewModel.loadSnapshots() },
                modifier = Modifier.padding(padding),
            )
            state.snapshots.isEmpty() -> EmptyState(
                "No net worth snapshots yet",
                modifier = Modifier.padding(padding),
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.snapshots, key = { it.id }) { snapshot ->
                            SnapshotListItem(
                                snapshot = snapshot,
                                onClick = { onNavigateToDetail(snapshot.id) },
                                onDelete = { viewModel.confirmDelete(snapshot) },
                            )
                            HorizontalDivider()
                        }
                    }

                    if (state.totalCount > 20) {
                        PaginationControls(
                            currentPage = state.currentPage,
                            totalCount = state.totalCount,
                            onPrevious = viewModel::previousPage,
                            onNext = viewModel::nextPage,
                        )
                    }
                }
            }
        }
    }

    // Create dialog
    if (state.showCreateDialog) {
        CreateSnapshotDialog(
            form = state.createForm,
            isCreating = state.isCreating,
            onTitleChange = viewModel::onCreateTitleChange,
            onAddEntry = viewModel::addEntry,
            onRemoveEntry = viewModel::removeEntry,
            onUpdateEntry = viewModel::updateEntry,
            onCreate = { viewModel.createSnapshot(onSuccess = {}) },
            onDismiss = viewModel::dismissCreateDialog,
        )
    }

    // Delete confirm
    if (state.snapshotToDelete != null) {
        ConfirmDialog(
            title = "Delete Snapshot",
            message = "Delete \"${state.snapshotToDelete!!.title}\"? This cannot be undone.",
            confirmLabel = "Delete",
            isDestructive = true,
            isLoading = state.isDeleting,
            onConfirm = viewModel::deleteSnapshot,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun SnapshotListItem(
    snapshot: GetNetWorthSnapshotsQuery.Item,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(snapshot.title) },
        supportingContent = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Assets: ${formatMoney(snapshot.totalAssets)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeGreen,
                    )
                    Text(
                        "Net: ${formatMoney(snapshot.netWorth)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (snapshot.netWorth >= 0) NetWorthPositive else NetWorthNegative,
                    )
                }
                Text(
                    formatDate(snapshot.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSnapshotDialog(
    form: CreateSnapshotFormState,
    isCreating: Boolean,
    onTitleChange: (String) -> Unit,
    onAddEntry: () -> Unit,
    onRemoveEntry: (String) -> Unit,
    onUpdateEntry: (String, EntryDraft.() -> EntryDraft) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Net Worth Snapshot") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = form.title,
                    onValueChange = onTitleChange,
                    label = { Text("Snapshot title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Totals preview
                val totalAssets = form.entries
                    .filter { it.type == "ASSET" }
                    .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                val totalLiabilities = form.entries
                    .filter { it.type == "LIABILITY" }
                    .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Assets: ${formatMoney(totalAssets)}", color = IncomeGreen)
                    Text("Net: ${formatMoney(totalAssets - totalLiabilities)}")
                }

                HorizontalDivider()

                // Entries
                form.entries.forEach { entry ->
                    EntryRow(
                        entry = entry,
                        onUpdate = { update -> onUpdateEntry(entry.id) { update() } },
                        onRemove = { onRemoveEntry(entry.id) },
                        canRemove = form.entries.size > 1,
                    )
                    HorizontalDivider()
                }

                TextButton(
                    onClick = onAddEntry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Text(" Add Entry")
                }

                if (form.error != null) {
                    Text(
                        text = form.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = !isCreating) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(
    entry: EntryDraft,
    onUpdate: (EntryDraft.() -> EntryDraft) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                listOf("ASSET", "LIABILITY").forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = entry.type == type,
                        onClick = {
                            val defaultCat = if (type == "ASSET") ASSET_CATEGORIES.last()
                            else LIABILITY_CATEGORIES.last()
                            onUpdate { copy(type = type, category = defaultCat) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    ) {
                        Text(type.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        OutlinedTextField(
            value = entry.label,
            onValueChange = { onUpdate { copy(label = it) } },
            label = { Text("Label") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = entry.amount,
                onValueChange = { onUpdate { copy(amount = it) } },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f),
            )

            // Category dropdown
            val categories = if (entry.type == "ASSET") ASSET_CATEGORIES else LIABILITY_CATEGORIES
            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = entry.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false },
                ) {
                    categories.forEach { cat ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onUpdate { copy(category = cat) }
                                catExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
