package com.mywallet.android.ui.networth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mywallet.android.ui.components.ConfirmDialog
import com.mywallet.android.ui.components.ErrorMessage
import com.mywallet.android.ui.components.LoadingScreen
import com.mywallet.android.ui.theme.ExpenseRed
import com.mywallet.android.ui.theme.IncomeGreen
import com.mywallet.android.ui.theme.NetWorthNegative
import com.mywallet.android.ui.theme.NetWorthPositive
import com.mywallet.android.util.formatDate
import com.mywallet.android.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthDetailScreen(
    snapshotId: String,
    onNavigateBack: () -> Unit,
    viewModel: NetWorthDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(snapshotId) { viewModel.init(snapshotId) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.snapshot?.title ?: "Net Worth") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showDeleteConfirm) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete snapshot",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
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
                    // Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    formatDate(snapshot.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    NetWorthStat("Assets", formatMoney(snapshot.totalAssets), IncomeGreen)
                                    NetWorthStat("Liabilities", formatMoney(snapshot.totalLiabilities), ExpenseRed)
                                    NetWorthStat(
                                        "Net Worth",
                                        formatMoney(snapshot.netWorth),
                                        if (snapshot.netWorth >= 0) NetWorthPositive else NetWorthNegative,
                                    )
                                }
                            }
                        }
                    }

                    // Assets section
                    if (assets.isNotEmpty()) {
                        item {
                            Text(
                                "Assets",
                                style = MaterialTheme.typography.titleMedium,
                                color = IncomeGreen,
                            )
                        }
                        items(assets, key = { it.id }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(entry.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        entry.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    formatMoney(entry.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = IncomeGreen,
                                )
                            }
                            HorizontalDivider()
                        }
                    }

                    // Liabilities section
                    if (liabilities.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Liabilities",
                                style = MaterialTheme.typography.titleMedium,
                                color = ExpenseRed,
                            )
                        }
                        items(liabilities, key = { it.id }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(entry.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        entry.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    formatMoney(entry.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ExpenseRed,
                                )
                            }
                            HorizontalDivider()
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
private fun NetWorthStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
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
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
