package com.antoniszisis.mywallet.ui.subscriptions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antoniszisis.mywallet.graphql.GetSubscriptionsQuery
import com.antoniszisis.mywallet.ui.reports.BreakdownCard
import com.antoniszisis.mywallet.ui.reports.ChartSegment
import com.antoniszisis.mywallet.ui.theme.CategoryColors
import com.antoniszisis.mywallet.ui.theme.LocalHideAmounts
import com.antoniszisis.mywallet.util.formatMoney
import com.antoniszisis.mywallet.util.getDaysUntil

private const val UNCATEGORIZED_LABEL = "Uncategorized"

private fun isActiveTrial(sub: GetSubscriptionsQuery.Item): Boolean {
    val daysLeft = sub.trialEndsAt?.let { getDaysUntil(it) } ?: return false
    return daysLeft >= 0
}

fun computeSubscriptionCategorySegments(
    subscriptions: List<GetSubscriptionsQuery.Item>,
): List<ChartSegment> {
    val totalsByCategory = mutableMapOf<String, Double>()
    for (sub in subscriptions) {
        if (sub.cancelledAt != null || isActiveTrial(sub)) continue
        val category = sub.category?.takeIf { it.isNotBlank() } ?: UNCATEGORIZED_LABEL
        totalsByCategory[category] = (totalsByCategory[category] ?: 0.0) + sub.monthlyCost
    }
    val total = totalsByCategory.values.sum()
    if (total == 0.0) return emptyList()

    return totalsByCategory.entries
        .map { (category, amount) ->
            ChartSegment(
                label = category,
                amount = amount,
                percentage = (amount / total).toFloat(),
                color = CategoryColors.forSubscription(category),
            )
        }
        .sortedByDescending { it.amount }
}

@Composable
fun SubscriptionBreakdownCard(
    subscriptions: List<GetSubscriptionsQuery.Item>,
    modifier: Modifier = Modifier,
) {
    val segments = remember(subscriptions) { computeSubscriptionCategorySegments(subscriptions) }
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")

    if (segments.isEmpty()) return

    BreakdownCard(
        title = "Spending by Category",
        isExpanded = isExpanded,
        chevronRotation = chevronRotation,
        onToggle = { isExpanded = !isExpanded },
        modifier = modifier,
    ) {
        SpendingBar(segments = segments)
        Spacer(modifier = Modifier.height(12.dp))
        SpendingLegend(segments = segments)
    }
}

@Composable
private fun SpendingBar(segments: List<ChartSegment>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50)),
    ) {
        for (segment in segments) {
            Box(
                modifier = Modifier
                    .weight(segment.percentage.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(segment.color),
            )
        }
    }
}

@Composable
private fun SpendingLegend(segments: List<ChartSegment>, modifier: Modifier = Modifier) {
    val hideAmounts = LocalHideAmounts.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (segment in segments) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(segment.color, CircleShape),
                )
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (hideAmounts) "••••" else formatMoney(segment.amount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "(${"%.1f".format(segment.percentage * 100)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
