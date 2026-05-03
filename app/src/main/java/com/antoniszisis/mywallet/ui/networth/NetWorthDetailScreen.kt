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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotQuery
import java.util.Locale
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
                        val prev = snapshot.previousSnapshot
                        val summaryCards = listOf(
                            SummaryCardData(
                                label = "Net Worth",
                                value = formatMoney(kotlin.math.abs(snapshot.netWorth)),
                                color = netWorthColor(snapshot.netWorth >= 0),
                                diff = prev?.let { buildDiff(snapshot.netWorth, it.netWorth) },
                                diffColor = prev?.let { if (snapshot.netWorth >= it.netWorth) incomeColor() else MaterialTheme.colorScheme.error },
                            ),
                            SummaryCardData(
                                label = "Assets",
                                value = formatMoney(snapshot.totalAssets),
                                color = incomeColor(),
                                diff = prev?.let { buildDiff(snapshot.totalAssets, it.totalAssets) },
                                diffColor = prev?.let { if (snapshot.totalAssets >= it.totalAssets) incomeColor() else MaterialTheme.colorScheme.error },
                            ),
                            SummaryCardData(
                                label = "Liabilities",
                                value = formatMoney(snapshot.totalLiabilities),
                                color = expenseColor(),
                                diff = prev?.let { buildDiff(snapshot.totalLiabilities, it.totalLiabilities) },
                                diffColor = prev?.let { if (snapshot.totalLiabilities <= it.totalLiabilities) incomeColor() else MaterialTheme.colorScheme.error },
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            summaryCards.forEach { card ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                ) {
                                    NetWorthStat(
                                        label = card.label,
                                        value = card.value,
                                        color = card.color,
                                        diff = card.diff,
                                        diffColor = card.diffColor,
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
                            val prevAssets = snapshot.previousSnapshot?.entries
                                ?.filter { it.type == "ASSET" } ?: emptyList()
                            CollapsibleEntriesCard(
                                title = "Assets",
                                total = formatMoney(snapshot.totalAssets),
                                totalAmount = snapshot.totalAssets,
                                titleColor = incomeColor(),
                                entries = assets,
                                previousEntries = prevAssets,
                                entryColor = incomeColor(),
                                positiveIsGood = true,
                                categoryOrder = ASSET_CATEGORIES,
                            )
                        }
                    }

                    // Liabilities section
                    if (liabilities.isNotEmpty()) {
                        item {
                            val prevLiabilities = snapshot.previousSnapshot?.entries
                                ?.filter { it.type == "LIABILITY" } ?: emptyList()
                            CollapsibleEntriesCard(
                                title = "Liabilities",
                                total = formatMoney(snapshot.totalLiabilities),
                                totalAmount = snapshot.totalLiabilities,
                                titleColor = expenseColor(),
                                entries = liabilities,
                                previousEntries = prevLiabilities,
                                entryColor = expenseColor(),
                                positiveIsGood = false,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsibleEntriesCard(
    title: String,
    total: String,
    totalAmount: Double,
    titleColor: Color,
    entries: List<GetNetWorthSnapshotQuery.Entry1>,
    previousEntries: List<GetNetWorthSnapshotQuery.Entry>,
    entryColor: Color,
    positiveIsGood: Boolean,
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
                                    val pct = if (totalAmount != 0.0)
                                        String.format(Locale.US, "%.1f", entry.amount / totalAmount * 100)
                                    else null
                                    val prevEntry = previousEntries.find { it.label == entry.label }
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                formatMoney(entry.amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = entryColor,
                                            )
                                            if (pct != null) {
                                                Text(
                                                    " ($pct%)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            if (prevEntry != null) {
                                                EntryDeltaIcon(
                                                    current = entry.amount,
                                                    previous = prevEntry.amount,
                                                    positiveIsGood = positiveIsGood,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDeltaIcon(
    current: Double,
    previous: Double,
    positiveIsGood: Boolean,
) {
    val delta = current - previous
    if (delta == 0.0) return

    val sign = if (delta >= 0) "+" else "-"
    val absDelta = kotlin.math.abs(delta)
    val pctDelta = if (previous != 0.0)
        " (${sign}${String.format(Locale.US, "%.1f", kotlin.math.abs(delta / previous * 100))}%)"
    else ""
    val deltaText = "${sign}${formatMoney(absDelta)}${pctDelta}"
    val deltaColor = if ((delta > 0) == positiveIsGood)
        incomeColor() else MaterialTheme.colorScheme.error

    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(deltaText, color = deltaColor)
            }
        },
        state = tooltipState,
        enableUserInput = false,
    ) {
        IconButton(
            onClick = { scope.launch { tooltipState.show() } },
            modifier = Modifier.size(20.dp),
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Change from previous snapshot",
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetWorthStat(
    label: String,
    value: String,
    color: Color,
    diff: String? = null,
    diffColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            if (diff != null && diffColor != null) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(diff) } },
                    state = tooltipState,
                    enableUserInput = false,
                ) {
                    IconButton(
                        onClick = { scope.launch { tooltipState.show() } },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Change from previous snapshot",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class SummaryCardData(
    val label: String,
    val value: String,
    val color: Color,
    val diff: String?,
    val diffColor: Color?,
)

private fun buildDiff(current: Double, previous: Double): String {
    val diff = current - previous
    val sign = if (diff >= 0) "+" else "-"
    val pct = if (previous != 0.0) kotlin.math.abs(diff / previous * 100) else null
    val pctStr = if (pct != null) " (${sign}${String.format(Locale.US, "%.1f", pct)}%)" else ""
    return "${sign}${formatMoney(kotlin.math.abs(diff))}${pctStr}"
}
