package com.mywallet.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mywallet.android.graphql.GetReportsSummaryQuery
import com.mywallet.android.ui.components.LoadingScreen
import com.mywallet.android.ui.components.SectionCard
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
                        Box(
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (state.userFullName != null) {
                                Text(
                                    text = getInitials(state.userFullName),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
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
                val reportsSummary = state.reportsSummary
                if (reportsSummary != null) {
                    val items = reportsSummary.items
                    val currentReport = items.firstOrNull()
                    val previousReport = if (items.size > 1) items[1] else null

                    SectionCard(title = "Reports Summary") {
                        TotalReportsCard(totalCount = reportsSummary.totalCount)
                        if (currentReport != null) {
                            Spacer(modifier = Modifier.height(8.dp))
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

                    // Income & Expenses chart
                    if (items.isNotEmpty()) {
                        var chartExpanded by remember { mutableStateOf(false) }
                        SectionCard(
                            title = "Income & Expenses",
                            trailing = {
                                IconButton(onClick = { chartExpanded = !chartExpanded }) {
                                    Icon(
                                        imageVector = if (chartExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (chartExpanded) "Collapse" else "Expand",
                                    )
                                }
                            },
                        ) {
                            AnimatedVisibility(visible = chartExpanded) {
                                IncomeExpensesChart(
                                    reports = items,
                                    onReportClick = onNavigateToReportDetail,
                                )
                            }
                        }
                    }
                }

                // Subscriptions summary
                val subs = state.activeSubscriptions
                if (subs != null && subs.items.isNotEmpty()) {
                    val totalMonthly = subs.items.sumOf { it.monthlyCost }
                    val currentIncome = state.reportsSummary?.items?.firstOrNull()
                        ?.transactions?.filter { it.type.rawValue == "INCOME" }
                        ?.sumOf { it.amount } ?: 0.0
                    val percentOfIncome = if (currentIncome > 0) {
                        "${"%.1f".format((totalMonthly / currentIncome) * 100)}%"
                    } else "-"
                    SectionCard(title = "Subscriptions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StatChip(label = "Active", value = "${subs.items.size}")
                            StatChip(label = "Monthly Cost", value = formatMoney(totalMonthly))
                            StatChip(label = "% of Income", value = percentOfIncome)
                        }
                    }

                    // Upcoming renewals (all future, sorted by date)
                    val upcoming = subs.items
                        .mapNotNull { sub ->
                            getNextRenewalDate(sub.startDate, sub.billingCycle)?.let { date -> sub to date }
                        }
                        .sortedBy { it.second }
                        .take(5)

                    var renewalsExpanded by remember { mutableStateOf(false) }
                    SectionCard(
                        title = "Upcoming Renewals",
                        trailing = {
                            IconButton(onClick = { renewalsExpanded = !renewalsExpanded }) {
                                Icon(
                                    imageVector = if (renewalsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (renewalsExpanded) "Collapse" else "Expand",
                                )
                            }
                        },
                    ) {
                        AnimatedVisibility(visible = renewalsExpanded) {
                            if (upcoming.isEmpty()) {
                                Text(
                                    text = "No upcoming renewals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column {
                                    upcoming.forEachIndexed { index, (sub, renewalDate) ->
                                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
                                                    formatDate(renewalDate.toString()),
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
private fun TotalReportsCard(totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Total Reports",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalCount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
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
private fun IncomeExpensesChart(
    reports: List<GetReportsSummaryQuery.Item>,
    onReportClick: (String) -> Unit,
) {
    // Take up to 6 most recent reports, reversed to show oldest first (left to right)
    val chartData = reports.take(6).reversed().map { report ->
        val income = report.transactions.filter { it.type.rawValue == "INCOME" }.sumOf { it.amount }
        val expenses = report.transactions.filter { it.type.rawValue == "EXPENSE" }.sumOf { it.amount }
        Triple(report, income, expenses)
    }

    val maxValue = chartData.maxOfOrNull { (_, income, expenses) -> maxOf(income, expenses) }
        ?.takeIf { it > 0 } ?: 1.0

    val barMaxHeight = 140.dp

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(IncomeGreen, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("Income", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(ExpenseRed, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("Expenses", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barMaxHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            chartData.forEach { (report, income, expenses) ->
                val incomeRatio = (income / maxValue).toFloat().coerceIn(0f, 1f)
                val expensesRatio = (expenses / maxValue).toFloat().coerceIn(0f, 1f)

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(barMaxHeight),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(barMaxHeight * incomeRatio)
                            .background(IncomeGreen, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .clickable { onReportClick(report.id) }
                    )
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(barMaxHeight * expensesRatio)
                            .background(ExpenseRed, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .clickable { onReportClick(report.id) }
                    )
                }
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            chartData.forEach { (report, _, _) ->
                Text(
                    text = report.title.take(10).let { if (report.title.length > 10) "$it…" else it },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
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
