package com.antoniszisis.mywallet.ui.networth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotQuery
import com.antoniszisis.mywallet.ui.components.ConfirmDialog
import com.antoniszisis.mywallet.ui.components.ErrorMessage
import com.antoniszisis.mywallet.ui.components.LoadingScreen
import com.antoniszisis.mywallet.ui.theme.expenseColor
import com.antoniszisis.mywallet.ui.theme.incomeColor
import com.antoniszisis.mywallet.ui.theme.netWorthColor
import com.antoniszisis.mywallet.util.formatDate
import com.antoniszisis.mywallet.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthDetailScreen(
    snapshotId: String,
    onNavigateBack: () -> Unit,
    viewModel: NetWorthDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(snapshotId) { viewModel.init(snapshotId) }
    val state by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.snapshot?.title ?: "Net Worth")
                        val snapshot = state.snapshot
                        if (snapshot != null) {
                            Text(
                                text = formatDate(snapshot.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showMenu = false },
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
                            onClick = {
                                showMenu = false
                                viewModel.showDeleteConfirm()
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.error != null -> ErrorMessage(
                message = state.error!!,
                modifier = Modifier.padding(padding),
            )
            state.snapshot != null -> {
                val snapshot = state.snapshot!!
                val assets = snapshot.entries.filter { it.type == "ASSET" }
                val liabilities = snapshot.entries.filter { it.type == "LIABILITY" }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Summary cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                Triple("Net Worth", formatMoney(kotlin.math.abs(snapshot.netWorth)), netWorthColor(snapshot.netWorth >= 0)),
                                Triple("Assets", formatMoney(snapshot.totalAssets), incomeColor()),
                                Triple("Liabilities", formatMoney(snapshot.totalLiabilities), expenseColor()),
                            ).forEach { (label, value, color) ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                ) {
                                    NetWorthStat(
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
                    if (assets.isNotEmpty()) {
                        item {
                            CollapsibleEntriesCard(
                                title = "Assets",
                                total = formatMoney(snapshot.totalAssets),
                                titleColor = incomeColor(),
                                entries = assets,
                                entryColor = incomeColor(),
                                categoryOrder = ASSET_CATEGORIES,
                            )
                        }
                    }

                    // Liabilities section
                    if (liabilities.isNotEmpty()) {
                        item {
                            CollapsibleEntriesCard(
                                title = "Liabilities",
                                total = formatMoney(snapshot.totalLiabilities),
                                titleColor = expenseColor(),
                                entries = liabilities,
                                entryColor = expenseColor(),
                                categoryOrder = LIABILITY_CATEGORIES,
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete Snapshot",
            message = "Delete \"${state.snapshot?.title}\"? This cannot be undone.",
            confirmLabel = "Delete",
            isDestructive = true,
            isLoading = state.isDeleting,
            onConfirm = { viewModel.deleteSnapshot(onNavigateBack) },
            onDismiss = viewModel::dismissDeleteConfirm,
        )
    }
}

@Composable
private fun CollapsibleEntriesCard(
    title: String,
    total: String,
    titleColor: Color,
    entries: List<GetNetWorthSnapshotQuery.Entry>,
    entryColor: Color,
    categoryOrder: List<String>,
) {
    var expanded by remember { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")

    val grouped = entries.groupBy { it.category }
    val orderedCategories = categoryOrder.filter { it in grouped } +
        grouped.keys.filterNot { it in categoryOrder }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
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
                    Text(
                        total,
                        style = MaterialTheme.typography.bodySmall,
                        color = titleColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    orderedCategories.forEach { category ->
                        val categoryEntries = grouped[category] ?: return@forEach
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                                categoryEntries.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            entry.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            formatMoney(entry.amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = entryColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetWorthStat(
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
