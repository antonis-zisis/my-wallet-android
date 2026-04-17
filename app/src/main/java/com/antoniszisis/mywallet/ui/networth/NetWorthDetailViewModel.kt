package com.antoniszisis.mywallet.ui.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.NetWorthRepository
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetWorthDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val snapshot: GetNetWorthSnapshotQuery.NetWorthSnapshot? = null,
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
)

@HiltViewModel
class NetWorthDetailViewModel @Inject constructor(
    private val netWorthRepository: NetWorthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetWorthDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun init(snapshotId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            netWorthRepository.getSnapshot(snapshotId).fold(
                onSuccess = { snapshot ->
                    _uiState.value = _uiState.value.copy(isLoading = false, snapshot = snapshot)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load snapshot",
                    )
                }
            )
        }
    }

    fun showDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun deleteSnapshot(onDeleted: () -> Unit) {
        val id = _uiState.value.snapshot?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            netWorthRepository.deleteSnapshot(id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleting = false)
                    onDeleted()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isDeleting = false)
                }
            )
        }
    }
}
