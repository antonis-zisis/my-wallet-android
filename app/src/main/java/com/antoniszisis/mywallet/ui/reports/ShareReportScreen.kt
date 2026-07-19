package com.antoniszisis.mywallet.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.antoniszisis.mywallet.graphql.GetReportQuery
import com.antoniszisis.mywallet.graphql.type.ReportRole
import com.antoniszisis.mywallet.ui.components.ConfirmDialog
import com.antoniszisis.mywallet.ui.components.ErrorMessage
import com.antoniszisis.mywallet.ui.components.LoadingScreen

private fun roleLabel(role: ReportRole): String = when (role) {
    ReportRole.OWNER -> "Owner"
    ReportRole.EDITOR -> "Can edit"
    ReportRole.VIEWER -> "Can view"
    ReportRole.UNKNOWN__ -> "Unknown"
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
    val result = parts.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    return result.ifEmpty { "?" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReportScreen(
    reportId: String,
    onNavigateBack: () -> Unit,
    onLeft: () -> Unit,
    viewModel: ShareReportViewModel = hiltViewModel(),
) {
    LaunchedEffect(reportId) { viewModel.init(reportId) }
    val state by viewModel.uiState.collectAsState()
    val isOwner = state.myRole == ReportRole.OWNER

    LaunchedEffect(state.didLeave) {
        if (state.didLeave) onLeft()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOwner) "Share Report" else "Members") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (!isOwner && !state.isLoading && state.error == null) {
                Surface(shadowElevation = 4.dp) {
                    Button(
                        onClick = viewModel::showLeaveConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text("Leave Report")
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(padding))
            state.error != null -> ErrorMessage(
                message = state.error!!,
                modifier = Modifier.padding(padding),
            )
            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (isOwner) {
                        ShareForm(state = state, viewModel = viewModel)
                        HorizontalDivider()
                    }
                    MembersList(state = state, isOwner = isOwner, viewModel = viewModel)
                    if (state.actionError != null) {
                        Text(
                            text = state.actionError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    if (state.memberToRemove != null) {
        val member = state.memberToRemove!!
        ConfirmDialog(
            title = "Remove Member",
            message = "Remove \"${member.fullName ?: member.email}\" from this report?",
            confirmLabel = "Remove",
            isDestructive = true,
            isLoading = state.isRemoving,
            onConfirm = viewModel::removeMember,
            onDismiss = viewModel::dismissRemoveMember,
        )
    }

    if (state.showLeaveConfirm) {
        ConfirmDialog(
            title = "Leave Report",
            message = "You will lose access to \"${state.reportTitle}\". The owner will need to share it with you again.",
            confirmLabel = "Leave",
            isDestructive = true,
            isLoading = state.isLeaving,
            onConfirm = viewModel::leaveReport,
            onDismiss = viewModel::dismissLeaveConfirm,
        )
    }
}

@Composable
private fun ShareForm(
    state: ShareReportUiState,
    viewModel: ShareReportViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Invite by email", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.emailInput,
                onValueChange = viewModel::onEmailChange,
                placeholder = { Text("name@example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.weight(1f),
            )
            RoleDropdown(
                selected = state.selectedRole,
                onSelect = viewModel::onRoleChange,
            )
        }
        Button(
            onClick = viewModel::shareReport,
            enabled = !state.isSharing && state.emailInput.isNotBlank(),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.align(Alignment.End),
        ) {
            if (state.isSharing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Share")
            }
        }
        if (state.shareError != null) {
            Text(
                text = state.shareError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    selected: ReportRole,
    onSelect: (ReportRole) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(132.dp),
    ) {
        OutlinedTextField(
            value = roleLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            listOf(ReportRole.VIEWER, ReportRole.EDITOR).forEach { role ->
                DropdownMenuItem(
                    text = { Text(roleLabel(role)) },
                    onClick = {
                        onSelect(role)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MembersList(
    state: ShareReportUiState,
    isOwner: Boolean,
    viewModel: ShareReportViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Members", style = MaterialTheme.typography.titleSmall)
        if (state.members.isEmpty()) {
            Text(
                text = "No members yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column {
                    state.members.forEachIndexed { index, member ->
                        if (index > 0) HorizontalDivider()
                        MemberRow(
                            member = member,
                            isCurrentUser = member.email == state.currentUserEmail,
                            isOwnerView = isOwner,
                            isBusy = state.busyShareId == member.id,
                            onRoleChange = { role -> viewModel.updateMemberRole(member, role) },
                            onRemove = { viewModel.confirmRemoveMember(member) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: GetReportQuery.Member,
    isCurrentUser: Boolean,
    isOwnerView: Boolean,
    isBusy: Boolean,
    onRoleChange: (ReportRole) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MemberAvatar(member = member)
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = member.fullName ?: member.email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (isCurrentUser) {
                    Text(
                        text = "(You)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (member.fullName != null) {
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isOwnerView && member.role != ReportRole.OWNER) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                CompactRoleMenu(role = member.role, onSelect = onRoleChange)
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove member",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            Text(
                text = roleLabel(member.role),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactRoleMenu(
    role: ReportRole,
    onSelect: (ReportRole) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(roleLabel(role), style = MaterialTheme.typography.labelSmall)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            listOf(ReportRole.VIEWER, ReportRole.EDITOR).forEach { option ->
                DropdownMenuItem(
                    text = { Text(roleLabel(option)) },
                    leadingIcon = {
                        if (option == role) Icon(Icons.Default.Check, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun MemberAvatar(member: GetReportQuery.Member) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(member.fullName ?: member.email),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
