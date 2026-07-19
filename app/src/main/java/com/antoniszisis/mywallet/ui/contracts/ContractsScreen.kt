package com.antoniszisis.mywallet.ui.contracts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antoniszisis.mywallet.graphql.GetContractsQuery
import com.antoniszisis.mywallet.ui.components.ConfirmDialog
import com.antoniszisis.mywallet.ui.components.EmptyState
import com.antoniszisis.mywallet.ui.components.ErrorMessage
import com.antoniszisis.mywallet.ui.components.LoadingScreen
import com.antoniszisis.mywallet.ui.components.SearchSortBar
import com.antoniszisis.mywallet.ui.theme.LocalHideAmounts
import com.antoniszisis.mywallet.ui.theme.cancelledBadgeColors
import com.antoniszisis.mywallet.ui.theme.trialBadgeColors
import com.antoniszisis.mywallet.util.formatDate
import com.antoniszisis.mywallet.util.formatMoney
import com.antoniszisis.mywallet.util.getDaysUntil
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsScreen(
    viewModel: ContractsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { trigger ->
                if (trigger) viewModel.loadMore()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Contracts")
                        if (!state.isLoading && state.error == null && state.totalCount > 0) {
                            Text(
                                text = if (state.totalCount == 1) "1 contract" else "${state.totalCount} contracts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddForm) {
                Icon(Icons.Default.Add, contentDescription = "Add contract")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val showControls = !state.isLoading && state.error == null &&
                (state.totalCount > 0 || state.searchQuery.isNotBlank())
            if (showControls) {
                SearchSortBar(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    searchPlaceholder = "Search by provider…",
                    sortOptions = ContractSortOption.entries,
                    selectedSortOption = state.sortOption,
                    sortOptionLabel = { it.label },
                    onSortOptionChange = viewModel::onSortOptionChange,
                    sortContentDescription = "Sort contracts",
                )
            }
            when {
                state.isLoading -> LoadingScreen()
                state.error != null -> ErrorMessage(
                    message = state.error!!,
                    onRetry = { viewModel.refresh() },
                )
                state.contracts.isEmpty() -> EmptyState(
                    if (state.searchQuery.isNotBlank()) {
                        "No contracts match your search"
                    } else {
                        "No contracts yet. Create your first one!"
                    }
                )
                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.weight(1f),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.contracts, key = { it.id }) { contract ->
                                ContractCard(
                                    contract = contract,
                                    onEdit = { viewModel.showEditForm(contract) },
                                    onDelete = { viewModel.confirmDelete(contract) },
                                )
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (state.showForm) {
        ContractFormDialog(
            state = state.form,
            isSaving = state.isSaving,
            onProviderChange = viewModel::onFormProviderChange,
            onCategoryChange = viewModel::onFormCategoryChange,
            onCustomCategoryChange = viewModel::onFormCustomCategoryChange,
            onPlanChange = viewModel::onFormPlanChange,
            onStartDateChange = viewModel::onFormStartDateChange,
            onEndDateChange = viewModel::onFormEndDateChange,
            onCostChange = viewModel::onFormCostChange,
            onSave = viewModel::saveContract,
            onDismiss = viewModel::dismissForm,
        )
    }

    if (state.contractToDelete != null) {
        ConfirmDialog(
            title = "Delete Contract",
            message = "Permanently delete \"${state.contractToDelete!!.provider}\"?",
            confirmLabel = "Delete",
            isDestructive = true,
            isLoading = state.isDeleting,
            onConfirm = viewModel::deleteContract,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun ContractCard(
    contract: GetContractsQuery.Item,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val hideAmounts = LocalHideAmounts.current
    var showMenu by remember { mutableStateOf(false) }
    val daysUntilExpiration = contract.endDate?.let { getDaysUntil(it) }
    val isExpiringSoon = !contract.isExpired &&
        daysUntilExpiration != null &&
        daysUntilExpiration in 0..CONTRACT_EXPIRING_SOON_DAYS

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = contract.provider,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    ContractBadge(
                        label = contract.category,
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        fg = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (contract.isExpired) {
                        val (bg, fg) = cancelledBadgeColors()
                        ContractBadge("Expired", bg, fg)
                    } else if (isExpiringSoon) {
                        val (bg, fg) = trialBadgeColors()
                        ContractBadge("Expiring soon", bg, fg)
                    }
                }
                Spacer(Modifier.height(2.dp))

                val expiryText = when {
                    contract.endDate == null -> "Open-ended"
                    daysUntilExpiration == null -> formatDate(contract.endDate)
                    daysUntilExpiration < 0 -> "expired ${formatDate(contract.endDate)}"
                    daysUntilExpiration == 0 -> "expires today"
                    daysUntilExpiration == 1 -> "expires tomorrow"
                    else -> "expires ${formatDate(contract.endDate)} · in ${daysUntilExpiration}d"
                }
                val tertiaryParts = listOfNotNull(
                    contract.plan?.takeIf { it.isNotBlank() },
                    expiryText,
                )
                Text(
                    text = tertiaryParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (contract.cost != null) {
                    Text(
                        text = if (hideAmounts) "••••" else formatMoney(contract.cost),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Box {
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
}

@Composable
private fun ContractBadge(label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractFormDialog(
    state: ContractFormState,
    isSaving: Boolean,
    onProviderChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onCustomCategoryChange: (String) -> Unit,
    onPlanChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val initialMillis = remember {
            try {
                LocalDate.parse(state.startDate)
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        onStartDateChange(picked)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val initialMillis = remember {
            try {
                LocalDate.parse(state.endDate)
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        onEndDateChange(picked)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "New Contract" else "Edit Contract") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.provider,
                    onValueChange = onProviderChange,
                    label = { Text("Provider") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = state.category.ifBlank { "Select a category" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        CONTRACT_CATEGORIES.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onCategoryChange(category)
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = state.category == "Other") {
                    OutlinedTextField(
                        value = state.customCategory,
                        onValueChange = onCustomCategoryChange,
                        label = { Text("Custom category") },
                        placeholder = { Text("e.g. Gym membership") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = state.plan,
                    onValueChange = onPlanChange,
                    label = { Text("Plan") },
                    placeholder = { Text("e.g. MyHome Online") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.startDate,
                        onValueChange = {},
                        label = { Text("Start Date") },
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Select date")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showStartDatePicker = true })
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.endDate,
                        onValueChange = {},
                        label = { Text("End Date") },
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Select date")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showEndDatePicker = true })
                }

                OutlinedTextField(
                    value = state.cost,
                    onValueChange = onCostChange,
                    label = { Text("Cost (optional)") },
                    placeholder = { Text("e.g. 29.90") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
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
