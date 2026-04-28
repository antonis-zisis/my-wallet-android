package com.antoniszisis.mywallet.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import com.antoniszisis.mywallet.graphql.GetReportsSummaryQuery
import com.antoniszisis.mywallet.ui.components.LoadingScreen
import com.antoniszisis.mywallet.ui.components.SectionCard
import com.antoniszisis.mywallet.ui.theme.expenseColor
import com.antoniszisis.mywallet.ui.theme.incomeColor
import com.antoniszisis.mywallet.ui.theme.netWorthColor
import com.antoniszisis.mywallet.util.formatDate
import com.antoniszisis.mywallet.util.formatMoney
import com.antoniszisis.mywallet.util.formatReportTitle
import com.antoniszisis.mywallet.util.getNextRenewalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReportDetail: (String) -> Unit,
    onNavigateToNetWorthDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Overview") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Reports section
                val reportsSummary = state.reportsSummary
                if (reportsSummary != null) {
                    val items = reportsSummary.items
                    val currentReport = items.firstOrNull()
                    val previousReport = if (items.size > 1) items[1] else null

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Reports",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TotalReportsCard(totalCount = reportsSummary.totalCount)
                    if (currentReport != null) {
                        ReportSummaryRow(
                            label = "Current",
                            report = currentReport,
                            onClick = { onNavigateToReportDetail(currentReport.id) },
                        )
                    }
                    if (previousReport != null) {
                        ReportSummaryRow(
                            label = "Previous",
                            report = previousReport,
                            onClick = { onNavigateToReportDetail(previousReport.id) },
                        )
                    }

                    // Income & Expenses chart
                    if (items.isNotEmpty()) {
                        var chartExpanded by remember { mutableStateOf(false) }
                        SectionCard(
                            title = "Monthly Summary",
                            showContentGap = chartExpanded,
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
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subscriptions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Subscriptions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${subs.items.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    val totalMonthly = subs.items.sumOf { it.monthlyCost }
                    val currentIncome = state.reportsSummary?.items?.firstOrNull()
                        ?.transactions?.filter { it.type.rawValue == "INCOME" }
                        ?.sumOf { it.amount } ?: 0.0
                    val percentOfIncome = if (currentIncome > 0) {
                        "${"%.1f".format((totalMonthly / currentIncome) * 100)}%"
                    } else "-"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SubscriptionStatCard(
                            label = "Monthly Cost",
                            value = formatMoney(totalMonthly),
                            modifier = Modifier.weight(1f),
                        )
                        SubscriptionStatCard(
                            label = "Yearly Cost",
                            value = formatMoney(totalMonthly * 12),
                            modifier = Modifier.weight(1f),
                        )
                        SubscriptionStatCard(
                            label = "% of Income",
                            value = percentOfIncome,
                            tooltip = "Based on the income of your latest report.",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Upcoming renewals — active subs renewing within 40 days, sorted by nearest date
                    val today = LocalDate.now()
                    val upcoming = subs.items
                        .mapNotNull { sub ->
                            getNextRenewalDate(sub.startDate, sub.billingCycle)?.let { date ->
                                val days = ChronoUnit.DAYS.between(today, date).toInt()
                                if (days in 0..40) Triple(sub, date, days) else null
                            }
                        }
                        .sortedBy { it.third }
                        .take(5)

                    var renewalsExpanded by remember { mutableStateOf(false) }
                    SectionCard(
                        title = "Upcoming Renewals",
                        showContentGap = renewalsExpanded,
                        titleTrailing = {
                            val infoTooltipState = rememberTooltipState()
                            val infoScope = rememberCoroutineScope()
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Shows up to 5 active subscriptions renewing within the next 40 days, sorted by nearest date.") } },
                                state = infoTooltipState,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { infoScope.launch { infoTooltipState.show() } },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
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
                                    text = "No renewals due in the next 40 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Column {
                                    upcoming.forEachIndexed { index, (sub, renewalDate, daysUntil) ->
                                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    sub.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    val billingLabel = if (sub.billingCycle == "MONTHLY") "Monthly" else "Yearly"
                                                    Text(billingLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(formatDate(renewalDate.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    val urgencyColor = when {
                                                        daysUntil <= 3 -> MaterialTheme.colorScheme.error
                                                        daysUntil <= 7 -> Color(0xFFF59E0B)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                    val urgencyLabel = when (daysUntil) {
                                                        0 -> "Today"
                                                        1 -> "Tomorrow"
                                                        else -> "in ${daysUntil}d"
                                                    }
                                                    Text(urgencyLabel, style = MaterialTheme.typography.bodySmall, color = urgencyColor, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                            Text(
                                                formatMoney(sub.amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
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
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Net Worth",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                                    color = netWorthColor(snapshot.netWorth >= 0),
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
                                        color = incomeColor(),
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
                                        color = expenseColor(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionStatCard(
    label: String,
    value: String,
    tooltip: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (tooltip != null) {
                val tooltipState = rememberTooltipState()
                val scope = rememberCoroutineScope()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(tooltip) } },
                    state = tooltipState,
                ) {
                    Row(
                        modifier = Modifier.clickable { scope.launch { tooltipState.show() } },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SummaryBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TotalReportsCard(totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "$totalCount",
                style = MaterialTheme.typography.titleLarge,
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
                    text = report.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                SummaryBadge(label = label)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = incomeColor(),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.bodySmall,
                        color = incomeColor(),
                    )
                }
                Text(
                    text = formatMoney(income),
                    style = MaterialTheme.typography.bodySmall,
                    color = incomeColor(),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = expenseColor(),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseColor(),
                    )
                }
                Text(
                    text = formatMoney(expenses),
                    style = MaterialTheme.typography.bodySmall,
                    color = expenseColor(),
                )
            }
        }
    }
}

@Composable
private fun IncomeExpensesChart(
    reports: List<GetReportsSummaryQuery.Item>,
    onReportClick: (String) -> Unit,
) {
    val chartData = reports.take(6).reversed().map { report ->
        val income = report.transactions.filter { it.type.rawValue == "INCOME" }.sumOf { it.amount }
        val expenses = report.transactions.filter { it.type.rawValue == "EXPENSE" }.sumOf { it.amount }
        Triple(report, income, expenses)
    }

    val maxValue = chartData.maxOfOrNull { (_, income, expenses) -> maxOf(income, expenses) }
        ?.takeIf { it > 0 } ?: 1.0

    val barMaxHeight = 140.dp
    val yAxisWidth = 36.dp
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    fun formatAxisValue(v: Double): String =
        if (v >= 1000) "${"%.1f".format(v / 1000)}k" else v.toInt().toString()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(incomeColor(), RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("Income", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(expenseColor(), RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("Expenses", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Y-axis labels + bars area with grid lines
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(yAxisWidth)
                    .height(barMaxHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(maxValue, maxValue / 2, 0.0).forEach { v ->
                    Text(
                        text = formatAxisValue(v),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barMaxHeight),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = size.height * fraction
                        drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
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
                                    .background(incomeColor(), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .clickable { onReportClick(report.id) }
                            )
                            Spacer(Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(barMaxHeight * expensesRatio)
                                    .background(expenseColor(), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .clickable { onReportClick(report.id) }
                            )
                        }
                    }
                }
            }
        }

        // X-axis labels — Spacer offsets to align with the chart area
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(yAxisWidth + 4.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                chartData.forEach { (report, _, _) ->
                    Text(
                        text = formatReportTitle(report.title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

