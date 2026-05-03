package com.antoniszisis.mywallet.ui.networth

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antoniszisis.mywallet.ui.theme.incomeColor
import com.antoniszisis.mywallet.ui.theme.netWorthColor
import com.antoniszisis.mywallet.util.formatMoney
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNetWorthSnapshotScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CreateNetWorthSnapshotViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val assetEntries = remember(state.entries) { state.entries.filter { it.type == "ASSET" } }
    val liabilityEntries = remember(state.entries) { state.entries.filter { it.type == "LIABILITY" } }
    val totalAssets = remember(assetEntries) { assetEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 } }
    val totalLiabilities = remember(liabilityEntries) { liabilityEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 } }
    val netWorth = totalAssets - totalLiabilities
    val hasSomeAmount = remember(state.entries) { state.entries.any { (it.amount.toDoubleOrNull() ?: 0.0) > 0 } }
    val hasIncompleteEntries = remember(state.entries) {
        state.entries.any { it.label.isBlank() || (it.amount.toDoubleOrNull() ?: 0.0) <= 0 }
    }

    val dateDisplay = remember(state.snapshotDate) {
        state.snapshotDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }

    val isValid = state.title.isNotBlank() && !hasIncompleteEntries && state.entries.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Snapshot") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (state.isCreating) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.createSnapshot(onSuccess) },
                            enabled = isValid,
                        ) {
                            Text("Create", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Title & Date
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Snapshot Title") },
                        placeholder = { Text("e.g. ${generateSnapshotTitle(state.snapshotDate)}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val dateInteractionSource = remember { MutableInteractionSource() }
                    val isDatePressed by dateInteractionSource.collectIsPressedAsState()
                    LaunchedEffect(isDatePressed) {
                        if (isDatePressed) viewModel.showDatePicker()
                    }
                    OutlinedTextField(
                        value = dateDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        interactionSource = dateInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Summary — only visible when at least one amount is entered
            if (hasSomeAmount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple("Net Worth", formatMoney(kotlin.math.abs(netWorth)), netWorthColor(netWorth >= 0)),
                        Triple("Assets", formatMoney(totalAssets), incomeColor()),
                        Triple("Liabilities", formatMoney(totalLiabilities), MaterialTheme.colorScheme.error),
                    ).forEach { (label, value, color) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            SummaryCard(
                                label = label,
                                value = value,
                                color = color,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            )
                        }
                    }
                }
            }

            // Assets section
            EntriesSection(
                title = "Assets",
                total = if (hasSomeAmount) totalAssets else null,
                totalColor = incomeColor(),
                onAdd = { viewModel.addEntry("ASSET") },
                emptyLabel = "No assets added yet.",
                entries = assetEntries,
                totalEntries = state.entries.size,
                onUpdateEntry = { id, update -> viewModel.updateEntry(id, update) },
                onRemoveEntry = viewModel::removeEntry,
            )

            // Liabilities section
            EntriesSection(
                title = "Liabilities",
                total = if (hasSomeAmount) totalLiabilities else null,
                totalColor = MaterialTheme.colorScheme.error,
                onAdd = { viewModel.addEntry("LIABILITY") },
                emptyLabel = "No liabilities added yet.",
                entries = liabilityEntries,
                totalEntries = state.entries.size,
                onUpdateEntry = { id, update -> viewModel.updateEntry(id, update) },
                onRemoveEntry = viewModel::removeEntry,
            )

            if (hasIncompleteEntries && hasSomeAmount) {
                Text(
                    "All entries need a label and an amount greater than zero.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.snapshotDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = viewModel::dismissDatePicker,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDateChange(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatePicker) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
internal fun SummaryCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
internal fun EntriesSection(
    title: String,
    total: Double?,
    totalColor: Color,
    onAdd: () -> Unit,
    emptyLabel: String,
    entries: List<EntryDraft>,
    totalEntries: Int,
    onUpdateEntry: (String, EntryDraft.() -> EntryDraft) -> Unit,
    onRemoveEntry: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (total != null) {
                        Text(
                            formatMoney(total),
                            style = MaterialTheme.typography.bodySmall,
                            color = totalColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                IconButton(onClick = onAdd) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Add $title entry",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (entries.isEmpty()) {
                Text(
                    emptyLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    EntryRow(
                        entry = entry,
                        onUpdate = { update -> onUpdateEntry(entry.id) { update() } },
                        onRemove = { onRemoveEntry(entry.id) },
                        canRemove = totalEntries > 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryRow(
    entry: EntryDraft,
    onUpdate: (EntryDraft.() -> EntryDraft) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    val categories = if (entry.type == "ASSET") ASSET_CATEGORIES else LIABILITY_CATEGORIES
    var catExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = entry.label,
                onValueChange = { onUpdate { copy(label = it) } },
                label = { Text("Label") },
                placeholder = { Text("e.g. Savings Account") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Remove entry",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = entry.amount,
                onValueChange = { onUpdate { copy(amount = it) } },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )

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
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false },
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                onUpdate { copy(category = cat) }
                                catExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
