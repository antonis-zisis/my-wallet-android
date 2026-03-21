package com.mywallet.android.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mywallet.android.data.repository.ReportRepository
import com.mywallet.android.graphql.GetReportsQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val reports: List<GetReportsQuery.Item> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val showCreateDialog: Boolean = false,
    val createTitle: String = "",
    val isCreating: Boolean = false,
    val createError: String? = null,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            reportRepository.getReports(page).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reports = data.items,
                        totalCount = data.totalCount,
                        currentPage = page,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load reports",
                    )
                }
            )
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            createTitle = "",
            createError = null,
        )
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun onCreateTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(createTitle = title, createError = null)
    }

    fun createReport(onSuccess: (String) -> Unit) {
        val title = _uiState.value.createTitle.trim()
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(createError = "Title is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            reportRepository.createReport(title).fold(
                onSuccess = { report ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        showCreateDialog = false,
                    )
                    loadReports()
                    onSuccess(report.id)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        createError = e.message ?: "Failed to create report",
                    )
                }
            )
        }
    }

    fun nextPage() {
        val state = _uiState.value
        val totalPages = (state.totalCount + 19) / 20
        if (state.currentPage < totalPages) loadReports(state.currentPage + 1)
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.currentPage > 1) loadReports(state.currentPage - 1)
    }
}
