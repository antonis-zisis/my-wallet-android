package com.antoniszisis.mywallet.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.ReportRepository
import com.antoniszisis.mywallet.graphql.GetReportsQuery
import com.antoniszisis.mywallet.graphql.type.ReportSortField
import com.antoniszisis.mywallet.graphql.type.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 10
private const val MIN_TITLE_LENGTH = 3
private const val SEARCH_DEBOUNCE_MS = 300L
const val MAX_REPORT_TITLE_LENGTH = 100

/** Mirrors the web app's report sort options (`REPORT_SORT_OPTIONS` / `REPORT_SORT_CONFIG`). */
enum class ReportSortOption(
    val label: String,
    val sortBy: ReportSortField,
    val sortOrder: SortOrder,
) {
    NEWEST("Created At", ReportSortField.NEWEST, SortOrder.DESC),
    NET_HIGH_LOW("Net (High–Low)", ReportSortField.NET_BALANCE, SortOrder.DESC),
    NET_LOW_HIGH("Net (Low–High)", ReportSortField.NET_BALANCE, SortOrder.ASC),
}

data class ReportsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val reports: List<GetReportsQuery.Item> = emptyList(),
    val totalCount: Int = 0,
    val loadedPages: Int = 0,
    val hasMore: Boolean = false,
    val searchQuery: String = "",
    val sortOption: ReportSortOption = ReportSortOption.NEWEST,
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

    private val searchQueryFlow = MutableStateFlow("")

    init {
        refresh()
        searchQueryFlow
            .drop(1)
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchQueryFlow.value = query
    }

    fun onSortOptionChange(option: ReportSortOption) {
        if (_uiState.value.sortOption == option) return
        _uiState.value = _uiState.value.copy(sortOption = option)
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = current.copy(
                isLoading = !silent && current.reports.isEmpty(),
                isRefreshing = !silent && current.reports.isNotEmpty(),
                error = null,
            )
            reportRepository.getReports(
                page = 1,
                pageSize = PAGE_SIZE,
                search = current.searchQuery.trim().ifBlank { null },
                sortBy = current.sortOption.sortBy,
                sortOrder = current.sortOption.sortOrder,
            ).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        reports = data.items,
                        totalCount = data.totalCount,
                        loadedPages = 1,
                        hasMore = data.items.size < data.totalCount,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load reports",
                    )
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        val nextPage = state.loadedPages + 1
        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            reportRepository.getReports(
                page = nextPage,
                pageSize = PAGE_SIZE,
                search = state.searchQuery.trim().ifBlank { null },
                sortBy = state.sortOption.sortBy,
                sortOrder = state.sortOption.sortOrder,
            ).fold(
                onSuccess = { data ->
                    val merged = _uiState.value.reports + data.items
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        reports = merged,
                        totalCount = data.totalCount,
                        loadedPages = nextPage,
                        hasMore = merged.size < data.totalCount,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Failed to load more reports",
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
        val truncated = if (title.length > MAX_REPORT_TITLE_LENGTH) {
            title.take(MAX_REPORT_TITLE_LENGTH)
        } else title
        _uiState.value = _uiState.value.copy(createTitle = truncated, createError = null)
    }

    fun createReport(onSuccess: (String) -> Unit) {
        val title = _uiState.value.createTitle.trim()
        if (title.length !in MIN_TITLE_LENGTH..MAX_REPORT_TITLE_LENGTH) {
            _uiState.value = _uiState.value.copy(
                createError = "Title must be between $MIN_TITLE_LENGTH and $MAX_REPORT_TITLE_LENGTH characters",
            )
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
                    refresh()
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
}
