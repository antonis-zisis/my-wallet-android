package com.antoniszisis.mywallet.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antoniszisis.mywallet.graphql.GetReportQuery
import com.antoniszisis.mywallet.ui.theme.CategoryColors
import com.antoniszisis.mywallet.util.formatMoney
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

data class ChartSegment(
    val label: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
)

fun computeExpenseSegments(transactions: List<GetReportQuery.Transaction>): List<ChartSegment> {
    val expensesByCategory = mutableMapOf<String, Double>()
    for (tx in transactions) {
        if (tx.type.rawValue != "EXPENSE") continue
        expensesByCategory[tx.category] = (expensesByCategory[tx.category] ?: 0.0) + tx.amount
    }
    val total = expensesByCategory.values.sum()
    if (total == 0.0) return emptyList()

    return expensesByCategory.entries
        .map { (category, amount) ->
            ChartSegment(
                label = category,
                amount = amount,
                percentage = (amount / total).toFloat(),
                color = CategoryColors.forExpense(category),
            )
        }
        .sortedBy { seg ->
            val idx = EXPENSE_CATEGORIES.indexOf(seg.label)
            if (idx == -1) Int.MAX_VALUE else idx
        }
}

@Composable
fun ExpenseBreakdownCard(
    transactions: List<GetReportQuery.Transaction>,
    modifier: Modifier = Modifier,
) {
    val segments = remember(transactions) { computeExpenseSegments(transactions) }
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")

    if (segments.isEmpty()) return

    BreakdownCard(
        title = "Expense Breakdown",
        segments = segments,
        isExpanded = isExpanded,
        chevronRotation = chevronRotation,
        onToggle = { isExpanded = !isExpanded },
        modifier = modifier,
    )
}

@Composable
internal fun BreakdownCard(
    title: String,
    segments: List<ChartSegment>,
    isExpanded: Boolean,
    chevronRotation: Float,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                ) {
                    DonutChart(
                        segments = segments,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BreakdownLegend(segments = segments)
                }
            }
        }
    }
}

@Composable
internal fun DonutChart(
    segments: List<ChartSegment>,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(segments) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val distance = sqrt(dx * dx + dy * dy)
                        val radius = minOf(size.width, size.height) / 2f
                        val innerR = radius * 0.40f
                        val outerR = radius * 0.75f

                        if (distance in innerR..outerR) {
                            var angleDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat() + 90f
                            if (angleDeg < 0f) angleDeg += 360f
                            if (angleDeg >= 360f) angleDeg -= 360f

                            var cumAngle = 0f
                            var tapped: Int? = null
                            for ((i, seg) in segments.withIndex()) {
                                val segSweep = seg.percentage * 360f
                                if (angleDeg < cumAngle + segSweep) {
                                    tapped = i
                                    break
                                }
                                cumAngle += segSweep
                            }
                            selectedIndex = if (tapped == selectedIndex) null else tapped
                        } else if (distance < innerR) {
                            selectedIndex = null
                        }
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension / 2f
            val innerR = radius * 0.40f
            val normalOuterR = radius * 0.65f
            val activeOuterR = radius * 0.72f
            val gapDegrees = if (segments.size > 1) 1.5f else 0f

            var startAngle = -90f

            for ((i, seg) in segments.withIndex()) {
                val isActive = i == selectedIndex
                val outerR = if (isActive) activeOuterR else normalOuterR
                val midR = (innerR + outerR) / 2f
                val strokeW = outerR - innerR
                val sweepAngle = maxOf(0.5f, seg.percentage * 360f - gapDegrees)

                drawArc(
                    color = seg.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - midR, cy - midR),
                    size = Size(midR * 2, midR * 2),
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                )

                startAngle += seg.percentage * 360f
            }
        }

        val sel = selectedIndex?.let { segments.getOrNull(it) }
        if (sel != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = sel.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = sel.color,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatMoney(sel.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${"%.1f".format(sel.percentage * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun BreakdownLegend(
    segments: List<ChartSegment>,
    modifier: Modifier = Modifier,
) {
    val rows = segments.chunked(2)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (seg in row) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(10.dp)
                                .background(seg.color, CircleShape),
                        )
                        Column {
                            Text(
                                text = seg.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = formatMoney(seg.amount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
