package com.antoniszisis.mywallet.ui.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.NetWorthRepository
import com.antoniszisis.mywallet.graphql.type.NetWorthEntryInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class EditSnapshotUiState(
    val title: String = "",
    val snapshotDate: LocalDate = LocalDate.now(),
    val showDatePicker: Boolean = false,
    val entries: List<EntryDraft> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class EditNetWorthSnapshotViewModel @Inject constructor(
    private val netWorthRepository: NetWorthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSnapshotUiState())
    val uiState = _uiState.asStateFlow()

    fun init(snapshotId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            netWorthRepository.getSnapshot(snapshotId).fold(
                onSuccess = { snapshot ->
                    val entries = snapshot.entries.map { entry ->
                        EntryDraft(
                            id = entry.id,
                            type = entry.type,
                            label = entry.label,
                            amount = entry.amount.toBigDecimal().stripTrailingZeros().toPlainString(),
                            category = entry.category,
                        )
                    }.ifEmpty { listOf(EntryDraft()) }
                    val date = runCatching { LocalDate.parse(snapshot.snapshotDate) }
                        .getOrDefault(LocalDate.now())
                    _uiState.value = EditSnapshotUiState(
                        title = snapshot.title,
                        snapshotDate = date,
                        entries = entries,
                        isLoading = false,
                    )
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

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title, error = null)
    }

    fun onDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(snapshotDate = date, showDatePicker = false)
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun dismissDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    fun addEntry(type: String) {
        val defaultCategory = if (type == "ASSET") ASSET_CATEGORIES.first() else LIABILITY_CATEGORIES.first()
        _uiState.value = _uiState.value.copy(
            entries = listOf(EntryDraft(type = type, category = defaultCategory)) + _uiState.value.entries,
        )
    }

    fun removeEntry(id: String) {
        if (_uiState.value.entries.size <= 1) return
        _uiState.value = _uiState.value.copy(
            entries = _uiState.value.entries.filter { it.id != id },
        )
    }

    fun updateEntry(id: String, update: EntryDraft.() -> EntryDraft) {
        _uiState.value = _uiState.value.copy(
            entries = _uiState.value.entries.map { if (it.id == id) it.update() else it },
        )
    }

    fun updateSnapshot(snapshotId: String, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Title is required")
            return
        }
        val entriesInput = state.entries.mapNotNull { entry ->
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
            _uiState.value = state.copy(error = "Add at least one entry with a label and amount greater than zero")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            netWorthRepository.updateSnapshot(
                id = snapshotId,
                title = state.title.trim(),
                snapshotDate = state.snapshotDate.toString(),
                entries = entriesInput,
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to update snapshot",
                    )
                }
            )
        }
    }
}
