package com.antoniszisis.mywallet.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.ReportRepository
import com.antoniszisis.mywallet.data.repository.UserRepository
import com.antoniszisis.mywallet.graphql.GetReportQuery
import com.antoniszisis.mywallet.graphql.ShareReportMutation
import com.antoniszisis.mywallet.graphql.UnshareReportMutation
import com.antoniszisis.mywallet.graphql.UpdateReportShareRoleMutation
import com.antoniszisis.mywallet.graphql.type.ReportRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun ShareReportMutation.Member.toReportMember() =
    GetReportQuery.Member(id = id, userId = userId, email = email, fullName = fullName, role = role)

private fun UpdateReportShareRoleMutation.Member.toReportMember() =
    GetReportQuery.Member(id = id, userId = userId, email = email, fullName = fullName, role = role)

private fun UnshareReportMutation.Member.toReportMember() =
    GetReportQuery.Member(id = id, userId = userId, email = email, fullName = fullName, role = role)

data class ShareReportUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val reportTitle: String = "",
    val myRole: ReportRole = ReportRole.VIEWER,
    val members: List<GetReportQuery.Member> = emptyList(),
    val currentUserEmail: String? = null,
    val emailInput: String = "",
    val selectedRole: ReportRole = ReportRole.VIEWER,
    val isSharing: Boolean = false,
    val shareError: String? = null,
    val busyShareId: String? = null,
    val actionError: String? = null,
    val memberToRemove: GetReportQuery.Member? = null,
    val isRemoving: Boolean = false,
    val showLeaveConfirm: Boolean = false,
    val isLeaving: Boolean = false,
    val didLeave: Boolean = false,
)

@HiltViewModel
class ShareReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareReportUiState())
    val uiState = _uiState.asStateFlow()

    private var reportId: String = ""

    fun init(reportId: String) {
        this.reportId = reportId
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val meResult = userRepository.getMe()
            reportRepository.getReport(reportId).fold(
                onSuccess = { report ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reportTitle = report.title,
                        myRole = report.myRole,
                        members = report.members,
                        currentUserEmail = meResult.getOrNull()?.email,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load report",
                    )
                }
            )
        }
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(emailInput = email, shareError = null)
    }

    fun onRoleChange(role: ReportRole) {
        _uiState.value = _uiState.value.copy(selectedRole = role)
    }

    fun shareReport() {
        val email = _uiState.value.emailInput.trim()
        if (!email.contains("@")) {
            _uiState.value = _uiState.value.copy(shareError = "Enter a valid email address")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true, shareError = null)
            reportRepository.shareReport(reportId, email, _uiState.value.selectedRole).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        members = data.members.map { it.toReportMember() },
                        emailInput = "",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        shareError = e.message ?: "Failed to share report",
                    )
                }
            )
        }
    }

    fun updateMemberRole(member: GetReportQuery.Member, role: ReportRole) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyShareId = member.id, actionError = null)
            reportRepository.updateReportShareRole(member.id, role).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        busyShareId = null,
                        members = data.members.map { it.toReportMember() },
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        busyShareId = null,
                        actionError = e.message ?: "Failed to update role",
                    )
                }
            )
        }
    }

    fun confirmRemoveMember(member: GetReportQuery.Member) {
        _uiState.value = _uiState.value.copy(memberToRemove = member)
    }

    fun dismissRemoveMember() {
        _uiState.value = _uiState.value.copy(memberToRemove = null)
    }

    fun removeMember() {
        val member = _uiState.value.memberToRemove ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRemoving = true)
            reportRepository.unshareReport(member.id).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isRemoving = false,
                        members = data.members.map { it.toReportMember() },
                        memberToRemove = null,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRemoving = false,
                        memberToRemove = null,
                        actionError = e.message ?: "Failed to remove member",
                    )
                }
            )
        }
    }

    fun showLeaveConfirm() {
        _uiState.value = _uiState.value.copy(showLeaveConfirm = true)
    }

    fun dismissLeaveConfirm() {
        _uiState.value = _uiState.value.copy(showLeaveConfirm = false)
    }

    fun leaveReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLeaving = true)
            reportRepository.leaveSharedReport(reportId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLeaving = false,
                        showLeaveConfirm = false,
                        didLeave = true,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLeaving = false,
                        showLeaveConfirm = false,
                        actionError = e.message ?: "Failed to leave report",
                    )
                }
            )
        }
    }
}
