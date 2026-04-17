package com.antoniszisis.mywallet.ui.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.NetWorthRepository
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotsQuery
import com.antoniszisis.mywallet.graphql.type.NetWorthEntryInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val ASSET_CATEGORIES = listOf("Savings", "Investments", "Real Estate", "Vehicle", "Other")
val LIABILITY_CATEGORIES = listOf("Mortgage", "Car Loan", "Student Loan", "Credit Card", "Personal Loan", "Other")

data class EntryDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String = "ASSET",
    val label: String = "",
    val amount: String = "",
    val category: String = "Other",
)

data class CreateSnapshotFormState(
    val title: String = "",
    val entries: List<EntryDraft> = listOf(EntryDraft()),
    val error: String? = null,
)

data class NetWorthUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val snapshots: List<GetNetWorthSnapshotsQuery.Item> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val showCreateDialog: Boolean = false,
    val createForm: CreateSnapshotFormState = CreateSnapshotFormState(),
    val isCreating: Boolean = false,
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

    // Create form
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            createForm = CreateSnapshotFormState(),
        )
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun onCreateTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(
            createForm = _uiState.value.createForm.copy(title = title, error = null)
        )
    }

    fun addEntry() {
        val form = _uiState.value.createForm
        _uiState.value = _uiState.value.copy(
            createForm = form.copy(entries = form.entries + EntryDraft())
        )
    }

    fun removeEntry(id: String) {
        val form = _uiState.value.createForm
        if (form.entries.size <= 1) return
        _uiState.value = _uiState.value.copy(
            createForm = form.copy(entries = form.entries.filter { it.id != id })
        )
    }

    fun updateEntry(id: String, update: EntryDraft.() -> EntryDraft) {
        val form = _uiState.value.createForm
        _uiState.value = _uiState.value.copy(
            createForm = form.copy(
                entries = form.entries.map { if (it.id == id) it.update() else it }
            )
        )
    }

    fun createSnapshot(onSuccess: () -> Unit) {
        val form = _uiState.value.createForm
        if (form.title.isBlank()) {
            _uiState.value = _uiState.value.copy(
                createForm = form.copy(error = "Title is required")
            )
            return
        }
        val entriesInput = form.entries.mapNotNull { entry ->
            val amount = entry.amount.toDoubleOrNull()
            if (amount != null && amount > 0 && entry.label.isNotBlank()) {
                NetWorthEntryInput(
                    type = entry.type,
                    label = entry.label.trim(),
                    amount = amount,
                    category = entry.category,
                )
            } else null
        }
        if (entriesInput.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                createForm = form.copy(error = "Add at least one valid entry")
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            netWorthRepository.createSnapshot(
                title = form.title.trim(),
                entries = entriesInput,
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isCreating = false, showCreateDialog = false)
                    loadSnapshots()
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        createForm = form.copy(error = e.message ?: "Failed to create snapshot"),
                    )
                }
            )
        }
    }

    // Delete
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
