package com.mywallet.android.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mywallet.android.graphql.GetReportsSummaryQuery
import com.mywallet.android.ui.components.EmptyState
import com.mywallet.android.ui.components.ErrorMessage
import com.mywallet.android.ui.components.LoadingScreen
import com.mywallet.android.ui.components.SectionCard
import com.mywallet.android.ui.navigation.Screen
import com.mywallet.android.ui.theme.ExpenseRed
import com.mywallet.android.ui.theme.Green500
import com.mywallet.android.ui.theme.IncomeGreen
import com.mywallet.android.ui.theme.NetWorthNegative
import com.mywallet.android.ui.theme.NetWorthPositive
import com.mywallet.android.util.formatDate
import com.mywallet.android.util.formatMoney
import com.mywallet.android.util.getInitials
import com.mywallet.android.util.getNextRenewalDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReportDetail: (String) -> Unit,
    onNavigateToNetWorthDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Dashboard") },
            actions = {
                IconButton(onClick = onNavigateToProfile) {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        if (state.userFullName != null) {
                            Text(
                                text = getInitials(state.userFullName),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.padding(6.dp),
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        )

        if (state.isLoading) {
            LoadingScreen()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Server health status
                HealthStatusCard(healthy = state.serverHealthy)

                // Reports summary
                val items = state.reportsSummary?.items ?: emptyList()
                val currentReport = items.firstOrNull()
                val previousReport = if (items.size > 1) items[1] else null

                if (currentReport != null || previousReport != null) {
                    SectionCard(title = "Reports Summary") {
                        if (currentReport != null) {
                            ReportSummaryRow(
                                label = "Current",
                                report = currentReport,
                                onClick = { onNavigateToReportDetail(currentReport.id) },
                            )
                        }
                        if (previousReport != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ReportSummaryRow(
                                label = "Previous",
                                report = previousReport,
                                onClick = { onNavigateToReportDetail(previousReport.id) },
                            )
                        }
                    }
                }

                // Subscriptions summary
                val subs = state.activeSubscriptions
                if (subs != null && subs.items.isNotEmpty()) {
                    val totalMonthly = subs.items.sumOf { it.monthlyCost }
                    SectionCard(title = "Subscriptions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StatChip(label = "Active", value = "${subs.items.size}")
                            StatChip(label = "Monthly Cost", value = formatMoney(totalMonthly))
                        }
                    }

                    // Upcoming renewals (next 30 days)
                    val upcoming = subs.items
                        .filter { sub ->
                            val next = getNextRenewalDate(sub.startDate, sub.billingCycle)
                            next != null && !next.isAfter(LocalDate.now().plusDays(30))
                        }
                        .sortedBy { getNextRenewalDate(it.startDate, it.billingCycle) }
                        .take(5)

                    if (upcoming.isNotEmpty()) {
                        SectionCard(title = "Upcoming Renewals") {
                            upcoming.forEach { sub ->
                                val renewalDate = getNextRenewalDate(sub.startDate, sub.billingCycle)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        sub.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            renewalDate?.toString() ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            formatMoney(sub.amount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Net worth latest snapshot
                val snapshot = state.latestSnapshot
                if (snapshot != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToNetWorthDetail(snapshot.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Net Worth",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = formatMoney(snapshot.netWorth),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (snapshot.netWorth >= 0) NetWorthPositive else NetWorthNegative,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        "Assets",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        formatMoney(snapshot.totalAssets),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IncomeGreen,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Liabilities",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        formatMoney(snapshot.totalLiabilities),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ExpenseRed,
                                    )
                                }
                            }
                            Text(
                                text = snapshot.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HealthStatusCard(healthy: Boolean?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (healthy) {
                true -> MaterialTheme.colorScheme.surface
                false -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surface
            }
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (healthy == true) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (healthy == true) Green500 else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (healthy) {
                    true -> "Server connected"
                    false -> "Cannot reach server"
                    null -> "Checking server..."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ReportSummaryRow(
    label: String,
    report: GetReportsSummaryQuery.Item,
    onClick: () -> Unit,
) {
    val income = report.transactions.filter { it.type.rawValue == "INCOME" }.sumOf { it.amount }
    val expenses = report.transactions.filter { it.type.rawValue == "EXPENSE" }.sumOf { it.amount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatMoney(income),
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeGreen,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatMoney(expenses),
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
