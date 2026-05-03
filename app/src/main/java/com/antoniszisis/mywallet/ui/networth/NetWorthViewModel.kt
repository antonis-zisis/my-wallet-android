package com.antoniszisis.mywallet.ui.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.NetWorthRepository
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotsQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetWorthUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val snapshots: List<GetNetWorthSnapshotsQuery.Item> = emptyList(),
    val trendSnapshots: List<GetNetWorthSnapshotsQuery.Item> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val snapshotToDelete: GetNetWorthSnapshotsQuery.Item? = null,
    val isDeleting: Boolean = false,
)

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val netWorthRepository: NetWorthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetWorthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSnapshots()
    }

    fun loadSnapshots(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            netWorthRepository.getSnapshots(page).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snapshots = data.items,
                        trendSnapshots = if (page == 1) data.items else _uiState.value.trendSnapshots,
                        totalCount = data.totalCount,
                        currentPage = page,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load snapshots",
                    )
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            netWorthRepository.getSnapshots(1).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        snapshots = data.items,
                        trendSnapshots = data.items,
                        totalCount = data.totalCount,
                        currentPage = 1,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to load snapshots",
                    )
                }
            )
        }
    }

    fun confirmDelete(snapshot: GetNetWorthSnapshotsQuery.Item) {
        _uiState.value = _uiState.value.copy(snapshotToDelete = snapshot)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(snapshotToDelete = null)
    }

    fun deleteSnapshot() {
        val snapshot = _uiState.value.snapshotToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            netWorthRepository.deleteSnapshot(snapshot.id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleting = false, snapshotToDelete = null)
                    loadSnapshots()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isDeleting = false)
                }
            )
        }
    }

    fun nextPage() {
        val state = _uiState.value
        val totalPages = (state.totalCount + 19) / 20
        if (state.currentPage < totalPages) loadSnapshots(state.currentPage + 1)
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.currentPage > 1) loadSnapshots(state.currentPage - 1)
    }
}
