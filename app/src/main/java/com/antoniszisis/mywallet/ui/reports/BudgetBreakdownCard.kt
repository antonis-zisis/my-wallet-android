package com.antoniszisis.mywallet.ui.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.antoniszisis.mywallet.graphql.GetReportQuery
import com.antoniszisis.mywallet.ui.theme.CategoryColors

private val CATEGORY_TO_BUCKET = mapOf(
    "Rent" to "Needs",
    "Utilities" to "Needs",
    "Groceries" to "Needs",
    "Health" to "Needs",
    "Insurance" to "Needs",
    "Loan" to "Needs",
    "Transport" to "Needs",
    "Dining Out" to "Wants",
    "Entertainment" to "Wants",
    "Shopping" to "Wants",
    "Other" to "Wants",
    "Investment" to "Invest",
)

private val BUCKET_ORDER = listOf("Needs", "Wants", "Invest")

fun computeBudgetSegments(transactions: List<GetReportQuery.Transaction>): List<ChartSegment> {
    val bucketTotals = mutableMapOf<String, Double>()
    for (tx in transactions) {
        if (tx.type.rawValue != "EXPENSE") continue
        val bucket = CATEGORY_TO_BUCKET[tx.category] ?: "Wants"
        bucketTotals[bucket] = (bucketTotals[bucket] ?: 0.0) + tx.amount
    }
    val total = bucketTotals.values.sum()
    if (total == 0.0) return emptyList()

    return bucketTotals.entries
        .map { (bucket, amount) ->
            ChartSegment(
                label = bucket,
                amount = amount,
                percentage = (amount / total).toFloat(),
                color = CategoryColors.forBudgetBucket(bucket),
            )
        }
        .sortedBy { seg -> BUCKET_ORDER.indexOf(seg.label).let { if (it == -1) Int.MAX_VALUE else it } }
}

@Composable
fun BudgetBreakdownCard(
    transactions: List<GetReportQuery.Transaction>,
    modifier: Modifier = Modifier,
) {
    val segments = remember(transactions) { computeBudgetSegments(transactions) }
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")

    if (segments.isEmpty()) return

    BreakdownCard(
        title = "Budget Breakdown",
        segments = segments,
        isExpanded = isExpanded,
        chevronRotation = chevronRotation,
        onToggle = { isExpanded = !isExpanded },
        modifier = modifier,
    )
}
