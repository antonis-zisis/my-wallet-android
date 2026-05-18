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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antoniszisis.mywallet.ui.components.ErrorMessage
import com.antoniszisis.mywallet.ui.components.LoadingScreen
import com.antoniszisis.mywallet.ui.theme.LocalHideAmounts
import com.antoniszisis.mywallet.ui.theme.incomeColor
import com.antoniszisis.mywallet.ui.theme.netWorthColor
import com.antoniszisis.mywallet.util.formatMoney
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNetWorthSnapshotScreen(
    snapshotId: String,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: EditNetWorthSnapshotViewModel = hiltViewModel(),
) {
    val hideAmounts = LocalHideAmounts.current
    LaunchedEffect(snapshotId) { viewModel.init(snapshotId) }
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Snapshot") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        ) { padding ->
            LoadingScreen(modifier = Modifier.padding(padding))
        }
        return
    }

    if (state.error != null && state.entries.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Snapshot") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        ) { padding ->
            ErrorMessage(message = state.error!!, modifier = Modifier.padding(padding))
        }
        return
    }

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
                title = { Text("Edit Snapshot") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (state.isSaving) {
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
                            onClick = { viewModel.updateSnapshot(snapshotId, onSuccess) },
                            enabled = isValid,
                        ) {
                            Text("Save", fontWeight = FontWeight.SemiBold)
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

            if (hasSomeAmount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple("Net Worth", if (hideAmounts) "••••" else formatMoney(kotlin.math.abs(netWorth)), netWorthColor(netWorth >= 0)),
                        Triple("Assets", if (hideAmounts) "••••" else formatMoney(totalAssets), incomeColor()),
                        Triple("Liabilities", if (hideAmounts) "••••" else formatMoney(totalLiabilities), MaterialTheme.colorScheme.error),
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
