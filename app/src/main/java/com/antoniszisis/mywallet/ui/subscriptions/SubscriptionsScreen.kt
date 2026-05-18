package com.antoniszisis.mywallet.ui.subscriptions

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antoniszisis.mywallet.graphql.GetSubscriptionsQuery
import com.antoniszisis.mywallet.ui.components.ConfirmDialog
import com.antoniszisis.mywallet.ui.components.EmptyState
import com.antoniszisis.mywallet.ui.components.ErrorMessage
import com.antoniszisis.mywallet.ui.components.LoadingScreen
import com.antoniszisis.mywallet.ui.theme.LocalHideAmounts
import com.antoniszisis.mywallet.ui.theme.cancelledBadgeColors
import com.antoniszisis.mywallet.ui.theme.trialBadgeColors
import com.antoniszisis.mywallet.ui.theme.trialCountdownColor
import com.antoniszisis.mywallet.util.formatDate
import com.antoniszisis.mywallet.util.formatDateShort
import com.antoniszisis.mywallet.util.formatMoney
import com.antoniszisis.mywallet.util.getDaysUntil
import com.antoniszisis.mywallet.util.getNextRenewalDate
import com.antoniszisis.mywallet.util.toInputDate
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val hideAmounts = LocalHideAmounts.current
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddForm) {
                Icon(Icons.Default.Add, contentDescription = "Add subscription")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen()
            state.error != null -> ErrorMessage(
                message = state.error!!,
                onRetry = viewModel::loadAll,
                modifier = Modifier.padding(padding),
            )
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Cost summary cards
                    item {
                        val now = LocalDate.now()
                        val totalMonthly = state.activeSubscriptions.sumOf { it.monthlyCost }
                        val totalYearly = totalMonthly * 12
                        val mostExpensive = state.activeSubscriptions.maxByOrNull { it.monthlyCost }
                        val firstOfMonth = now.withDayOfMonth(1)
                        val lastOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                        val renewingThisMonthTotal = state.activeSubscriptions
                            .filter { sub ->
                                try {
                                    val start = LocalDate.parse(toInputDate(sub.startDate))
                                    val increment = if (sub.billingCycle == "MONTHLY") 1L else 12L
                                    var renewal = start
                                    while (renewal.isBefore(firstOfMonth)) {
                                        renewal = renewal.plusMonths(increment)
                                    }
                                    !renewal.isAfter(lastOfMonth)
                                } catch (e: Exception) { false }
                            }
                            .sumOf { it.amount }
                        val nextRenewalEntry = state.activeSubscriptions
                            .mapNotNull { sub ->
                                getNextRenewalDate(sub.startDate, sub.billingCycle)?.let { sub to it }
                            }
                            .minByOrNull { (_, date) -> date }
                        val monthName = now.month.name.lowercase().replaceFirstChar { it.titlecase() }

                        val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        val scope = rememberCoroutineScope()
                        val thisMonthTooltipState = rememberTooltipState()
                        val mostExpensiveTooltipState = rememberTooltipState()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1: Monthly cost | Yearly cost | This month
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    "Monthly cost" to if (hideAmounts) "••••" else formatMoney(totalMonthly),
                                    "Yearly cost" to if (hideAmounts) "••••" else formatMoney(totalYearly),
                                ).forEach { (label, value) ->
                                    Card(modifier = Modifier.weight(1f), colors = cardColors, elevation = cardElevation) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                // This month — with info tooltip
                                Card(modifier = Modifier.weight(1f), colors = cardColors, elevation = cardElevation) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = if (hideAmounts) "••••" else formatMoney(renewingThisMonthTotal),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = "This month",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                                tooltip = { PlainTooltip { Text("Total charged in $monthName") } },
                                                state = thisMonthTooltipState,
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = "Info",
                                                    modifier = Modifier
                                                        .padding(start = 2.dp)
                                                        .size(10.dp)
                                                        .clickable { scope.launch { thisMonthTooltipState.show() } },
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Row 2: Next renewal | Most expensive
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Next renewal
                                Card(modifier = Modifier.weight(1f), colors = cardColors, elevation = cardElevation) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        if (nextRenewalEntry != null) {
                                            val (sub, date) = nextRenewalEntry
                                            // Line 1: name · price
                                            Text(
                                                text = "${sub.name} · ${if (hideAmounts) "••••" else formatMoney(sub.amount)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            // Line 2: Next renewal - May 2, 2026
                                            Text(
                                                text = "Next renewal - ${formatDate(date.toString())}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        } else {
                                            Text(
                                                text = "—",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = "Next renewal",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                // Most expensive
                                Card(modifier = Modifier.weight(1f), colors = cardColors, elevation = cardElevation) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        // Line 1: name
                                        Text(
                                            text = mostExpensive?.name ?: "—",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        // Line 2: "Most expensive · €X/mo" + info icon
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = if (mostExpensive != null)
                                                    "Most expensive · ${if (hideAmounts) "••••" else formatMoney(mostExpensive.monthlyCost)}/mo"
                                                else
                                                    "Most expensive",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (mostExpensive != null) {
                                                TooltipBox(
                                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                                    tooltip = {
                                                        PlainTooltip {
                                                            Text("Yearly cost: ${if (hideAmounts) "••••" else formatMoney(mostExpensive.monthlyCost * 12)}")
                                                        }
                                                    },
                                                    state = mostExpensiveTooltipState,
                                                ) {
                                                    Icon(
                                                        Icons.Default.Info,
                                                        contentDescription = "Info",
                                                        modifier = Modifier
                                                            .padding(start = 2.dp)
                                                            .size(10.dp)
                                                            .clickable { scope.launch { mostExpensiveTooltipState.show() } },
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Active section header
                    item {
                        Text(
                            "Active (${state.activeSubscriptions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (state.activeSubscriptions.isEmpty()) {
                        item { EmptyState("No active subscriptions") }
                    }

                    items(state.activeSubscriptions, key = { it.id }) { sub ->
                        SubscriptionCard(
                            sub = sub,
                            onEdit = { viewModel.showEditForm(sub) },
                            onCancel = if (sub.cancelledAt == null) { { viewModel.confirmCancel(sub) } } else null,
                            onResume = if (sub.cancelledAt != null) { { viewModel.showResumeForm(sub) } } else null,
                            onDelete = { viewModel.confirmDelete(sub) },
                        )
                    }

                    // Inactive section toggle
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = viewModel::toggleShowInactive,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Inactive (${state.inactiveTotalCount})")
                                Icon(
                                    if (state.showInactive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    if (state.showInactive) {
                        if (state.inactiveSubscriptions.isEmpty()) {
                            item { EmptyState("No inactive subscriptions") }
                        }
                        items(state.inactiveSubscriptions, key = { it.id }) { sub ->
                            SubscriptionCard(
                                sub = sub,
                                onEdit = { viewModel.showEditForm(sub) },
                                onCancel = null,
                                onResume = { viewModel.showResumeForm(sub) },
                                onDelete = { viewModel.confirmDelete(sub) },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
                } // PullToRefreshBox
            }
        }
    }

    // Subscription form
    if (state.showForm) {
        SubscriptionFormDialog(
            state = state.form,
            isSaving = state.isSaving,
            onNameChange = viewModel::onFormNameChange,
            onAmountChange = viewModel::onFormAmountChange,
            onBillingCycleChange = viewModel::onFormBillingCycleChange,
            onStartDateChange = viewModel::onFormStartDateChange,
            onIsTrialChange = viewModel::onFormIsTrialChange,
            onTrialEndsAtChange = viewModel::onFormTrialEndsAtChange,
            onUrlChange = viewModel::onFormUrlChange,
            onPaymentMethodChange = viewModel::onFormPaymentMethodChange,
            onNotesChange = viewModel::onFormNotesChange,
            onSave = viewModel::saveSubscription,
            onDismiss = viewModel::dismissForm,
        )
    }

    // Cancel confirm
    if (state.subscriptionToCancel != null) {
        ConfirmDialog(
            title = "Cancel Subscription",
            message = "Cancel \"${state.subscriptionToCancel!!.name}\"? It will be marked inactive.",
            confirmLabel = "Cancel Subscription",
            isDestructive = false,
            isLoading = state.isCancelling,
            onConfirm = viewModel::cancelSubscription,
            onDismiss = viewModel::dismissCancel,
        )
    }

    // Resume form
    if (state.showResumeForm) {
        ResumeFormDialog(
            state = state.resumeForm,
            isResuming = state.isResuming,
            onAmountChange = viewModel::onResumeAmountChange,
            onBillingCycleChange = viewModel::onResumeBillingCycleChange,
            onStartDateChange = viewModel::onResumeStartDateChange,
            onResume = viewModel::resumeSubscription,
            onDismiss = viewModel::dismissResumeForm,
        )
    }

    // Delete confirm
    if (state.subscriptionToDelete != null) {
        ConfirmDialog(
            title = "Delete Subscription",
            message = "Permanently delete \"${state.subscriptionToDelete!!.name}\"?",
            confirmLabel = "Delete",
            isDestructive = true,
            isLoading = state.isDeleting,
            onConfirm = viewModel::deleteSubscription,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun SubscriptionCard(
    sub: GetSubscriptionsQuery.Item,
    onEdit: () -> Unit,
    onCancel: (() -> Unit)?,
    onResume: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val hideAmounts = LocalHideAmounts.current
    var showMenu by remember { mutableStateOf(false) }
    val nextRenewal = getNextRenewalDate(sub.startDate, sub.billingCycle)


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
                val isCancelled = sub.cancelledAt != null
                val trialDaysLeft = sub.trialEndsAt?.let { getDaysUntil(it) }
                val isActiveTrial = trialDaysLeft != null && trialDaysLeft >= 0

                val cycleLabel = if (sub.billingCycle == "MONTHLY") "Monthly" else "Yearly"
                val cycleBg = MaterialTheme.colorScheme.surfaceVariant
                val cycleFg = MaterialTheme.colorScheme.onSurfaceVariant
                val altCost = if (hideAmounts) "••••" else if (sub.billingCycle == "MONTHLY") {
                    "(${formatMoney(sub.amount * 12)}/yr)"
                } else {
                    "(${formatMoney(sub.monthlyCost)}/mo)"
                }
                val amountText = "${if (hideAmounts) "••••" else formatMoney(sub.amount)} $altCost"

                val (dateText, isAmberSubtitle) = when {
                    isCancelled && sub.endDate != null -> {
                        val d = getDaysUntil(sub.endDate)
                        val s = when {
                            d < 0 -> "ended ${formatDate(sub.endDate)}"
                            d == 0 -> "ends today"
                            d == 1 -> "ends tomorrow"
                            else -> "ends in $d days · ${formatDate(sub.endDate)}"
                        }
                        s to true
                    }
                    isActiveTrial -> {
                        val d = trialDaysLeft!!
                        val s = when {
                            d == 0 -> "trial ends today"
                            d == 1 -> "trial ends tomorrow"
                            else -> "trial ends in $d days · ${formatDate(sub.trialEndsAt!!)}"
                        }
                        s to true
                    }
                    sub.isActive && nextRenewal != null -> {
                        val d = getDaysUntil(nextRenewal.toString())
                        val rel = when {
                            d == 0 -> " · today"
                            d == 1 -> " · tomorrow"
                            d in 2..29 -> " · in ${d}d"
                            else -> ""
                        }
                        "next renewal at ${formatDate(nextRenewal.toString())}$rel" to false
                    }
                    else -> null to false
                }

                val subtitleColor = if (isAmberSubtitle) trialCountdownColor()
                                    else MaterialTheme.colorScheme.onSurfaceVariant

                // Title row with badge(s)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    SubscriptionBadge(cycleLabel, cycleBg, cycleFg)
                    if (isActiveTrial) {
                        val (trialBg, trialFg) = trialBadgeColors()
                        SubscriptionBadge("Trial", trialBg, trialFg)
                    }
                    if (isCancelled) {
                        val (cancelBg, cancelFg) = cancelledBadgeColors()
                        SubscriptionBadge("Cancelled", cancelBg, cancelFg)
                    }
                }
                Spacer(Modifier.height(2.dp))
                // Amount row
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Date text row
                if (dateText != null) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                    )
                }
                // Tertiary line: payment method + notes
                val tertiaryParts = listOfNotNull(
                    sub.paymentMethod?.takeIf { it.isNotBlank() }?.let { "via $it" },
                    sub.notes?.takeIf { it.isNotBlank() },
                )
                if (tertiaryParts.isNotEmpty()) {
                    Text(
                        text = tertiaryParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Action menu
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
                if (onCancel != null) {
                    DropdownMenuItem(
                        text = { Text("Cancel") },
                        leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) },
                        onClick = { showMenu = false; onCancel() },
                    )
                }
                if (onResume != null) {
                    DropdownMenuItem(
                        text = { Text("Resume") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = { showMenu = false; onResume() },
                    )
                }
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
            } // Box
        }
    }
}

@Composable
private fun SubscriptionBadge(label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
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
private fun ResumeFormDialog(
    state: ResumeFormState,
    isResuming: Boolean,
    onAmountChange: (String) -> Unit,
    onBillingCycleChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resume Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter a new start date for ${state.name}. You can also update the amount and billing cycle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("MONTHLY", "YEARLY").forEachIndexed { index, cycle ->
                        SegmentedButton(
                            selected = state.billingCycle == cycle,
                            onClick = { onBillingCycleChange(cycle) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) {
                            Text(cycle.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                OutlinedTextField(
                    value = state.startDate,
                    onValueChange = onStartDateChange,
                    label = { Text("New Start Date (YYYY-MM-DD)") },
                    singleLine = true,
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
            Button(onClick = onResume, enabled = !isResuming) {
                if (isResuming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Resume")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isResuming) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionFormDialog(
    state: SubscriptionFormState,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onBillingCycleChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onIsTrialChange: (Boolean) -> Unit,
    onTrialEndsAtChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val billingCycleOptions = listOf("MONTHLY", "YEARLY", "WEEKLY", "QUARTERLY", "BIANNUAL")
    val billingCycleLabel: (String) -> String = {
        when (it) {
            "MONTHLY" -> "Monthly"
            "YEARLY" -> "Yearly"
            "WEEKLY" -> "Weekly"
            "QUARTERLY" -> "Quarterly"
            "BIANNUAL" -> "Bi-Annual"
            else -> it.lowercase().replaceFirstChar { c -> c.uppercase() }
        }
    }

    var billingExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showTrialDatePicker by remember { mutableStateOf(false) }
    var showAdditionalDetails by remember(state.id) {
        mutableStateOf(state.url.isNotBlank() || state.paymentMethod.isNotBlank() || state.notes.isNotBlank())
    }

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

    if (showTrialDatePicker) {
        val initialMillis = remember {
            try {
                LocalDate.parse(state.trialEndsAt)
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showTrialDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        onTrialEndsAtChange(picked)
                    }
                    showTrialDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTrialDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "New Subscription" else "Edit Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = billingExpanded,
                    onExpandedChange = { billingExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = billingCycleLabel(state.billingCycle),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Billing Cycle") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = billingExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = billingExpanded,
                        onDismissRequest = { billingExpanded = false },
                    ) {
                        billingCycleOptions.forEach { cycle ->
                            DropdownMenuItem(
                                text = { Text(billingCycleLabel(cycle)) },
                                onClick = {
                                    onBillingCycleChange(cycle)
                                    billingExpanded = false
                                },
                            )
                        }
                    }
                }

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

                // Trial
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIsTrialChange(!state.isTrial) },
                ) {
                    Checkbox(
                        checked = state.isTrial,
                        onCheckedChange = onIsTrialChange,
                    )
                    Text(
                        text = "Currently on a free trial",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                AnimatedVisibility(visible = state.isTrial) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.trialEndsAt,
                            onValueChange = {},
                            label = { Text("Trial ends") },
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Select date")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showTrialDatePicker = true })
                    }
                }

                // Additional details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdditionalDetails = !showAdditionalDetails },
                ) {
                    Text(
                        text = "Additional details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (showAdditionalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                AnimatedVisibility(visible = showAdditionalDetails) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.url,
                            onValueChange = onUrlChange,
                            label = { Text("Website URL") },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.paymentMethod,
                            onValueChange = onPaymentMethodChange,
                            label = { Text("Payment Method") },
                            placeholder = { Text("e.g. Revolut, Visa *1234") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = onNotesChange,
                            label = { Text("Notes") },
                            placeholder = { Text("e.g. shared with sister") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

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
            Button(
                onClick = onSave,
                enabled = !isSaving,
                shape = RoundedCornerShape(4.dp),
            ) {
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
