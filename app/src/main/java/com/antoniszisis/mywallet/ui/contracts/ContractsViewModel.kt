package com.antoniszisis.mywallet.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.ContractRepository
import com.antoniszisis.mywallet.graphql.GetContractsQuery
import com.antoniszisis.mywallet.graphql.type.ContractSortField
import com.antoniszisis.mywallet.graphql.type.SortOrder
import com.antoniszisis.mywallet.util.toInputDate
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

const val CONTRACT_EXPIRING_SOON_DAYS = 30
private const val PAGE_SIZE = 10
private const val SEARCH_DEBOUNCE_MS = 300L

/** Mirrors the web app's contract sort options (`CONTRACT_SORT_OPTIONS`). */
enum class ContractSortOption(
    val label: String,
    val sortBy: ContractSortField,
    val sortOrder: SortOrder,
) {
    PROVIDER("Provider (A–Z)", ContractSortField.PROVIDER, SortOrder.ASC),
    EXPIRY_DATE("Expiry Date", ContractSortField.END_DATE, SortOrder.ASC),
}

val CONTRACT_CATEGORIES = listOf(
    "Electricity",
    "Gas",
    "Water",
    "Internet",
    "Mobile",
    "TV",
    "Insurance",
    "Loan/Mortgage",
    "Other",
)

data class ContractFormState(
    val id: String? = null,
    val provider: String = "",
    val category: String = "",
    val customCategory: String = "",
    val plan: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val cost: String = "",
    val error: String? = null,
) {
    val resolvedCategory: String
        get() = if (category == "Other" && customCategory.isNotBlank()) customCategory.trim() else category
}

data class ContractsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val contracts: List<GetContractsQuery.Item> = emptyList(),
    val totalCount: Int = 0,
    val loadedPages: Int = 0,
    val hasMore: Boolean = false,
    val searchQuery: String = "",
    val sortOption: ContractSortOption = ContractSortOption.EXPIRY_DATE,
    val showForm: Boolean = false,
    val form: ContractFormState = ContractFormState(),
    val isSaving: Boolean = false,
    val contractToDelete: GetContractsQuery.Item? = null,
    val isDeleting: Boolean = false,
)

@HiltViewModel
class ContractsViewModel @Inject constructor(
    private val contractRepository: ContractRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractsUiState())
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

    fun onSortOptionChange(option: ContractSortOption) {
        if (_uiState.value.sortOption == option) return
        _uiState.value = _uiState.value.copy(sortOption = option)
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = current.copy(
                isLoading = !silent && current.contracts.isEmpty(),
                isRefreshing = !silent && current.contracts.isNotEmpty(),
                error = null,
            )
            contractRepository.getContracts(
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
                        contracts = data.items,
                        totalCount = data.totalCount,
                        loadedPages = 1,
                        hasMore = data.items.size < data.totalCount,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load contracts",
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
            contractRepository.getContracts(
                page = nextPage,
                pageSize = PAGE_SIZE,
                search = state.searchQuery.trim().ifBlank { null },
                sortBy = state.sortOption.sortBy,
                sortOrder = state.sortOption.sortOrder,
            ).fold(
                onSuccess = { data ->
                    val merged = _uiState.value.contracts + data.items
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        contracts = merged,
                        totalCount = data.totalCount,
                        loadedPages = nextPage,
                        hasMore = merged.size < data.totalCount,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Failed to load more contracts",
                    )
                }
            )
        }
    }

    // Form
    fun showAddForm() {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            form = ContractFormState(),
        )
    }

    fun showEditForm(contract: GetContractsQuery.Item) {
        val isPredefined = CONTRACT_CATEGORIES.contains(contract.category) && contract.category != "Other"
        _uiState.value = _uiState.value.copy(
            showForm = true,
            form = ContractFormState(
                id = contract.id,
                provider = contract.provider,
                category = if (isPredefined) contract.category else "Other",
                customCategory = if (isPredefined) "" else contract.category,
                plan = contract.plan ?: "",
                startDate = contract.startDate?.let { toInputDate(it) } ?: "",
                endDate = contract.endDate?.let { toInputDate(it) } ?: "",
                cost = contract.cost?.toString() ?: "",
            ),
        )
    }

    fun dismissForm() {
        _uiState.value = _uiState.value.copy(showForm = false)
    }

    fun onFormProviderChange(v: String) = updateForm { copy(provider = v, error = null) }
    fun onFormCategoryChange(v: String) = updateForm { copy(category = v, error = null) }
    fun onFormCustomCategoryChange(v: String) = updateForm { copy(customCategory = v, error = null) }
    fun onFormPlanChange(v: String) = updateForm { copy(plan = v, error = null) }
    fun onFormStartDateChange(v: String) = updateForm { copy(startDate = v, error = null) }
    fun onFormEndDateChange(v: String) = updateForm { copy(endDate = v, error = null) }
    fun onFormCostChange(v: String) = updateForm { copy(cost = v, error = null) }

    private fun updateForm(update: ContractFormState.() -> ContractFormState) {
        _uiState.value = _uiState.value.copy(form = _uiState.value.form.update())
    }

    fun saveContract() {
        val form = _uiState.value.form
        if (form.provider.isBlank()) {
            updateForm { copy(error = "Provider is required") }
            return
        }
        val hasCategory = form.category.isNotBlank() &&
            (form.category != "Other" || form.customCategory.isNotBlank())
        if (!hasCategory) {
            updateForm { copy(error = "Category is required") }
            return
        }
        if (form.startDate.isNotBlank() && form.endDate.isNotBlank() && form.endDate < form.startDate) {
            updateForm { copy(error = "End date must be after start date") }
            return
        }
        val cost = form.cost.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (form.cost.isNotBlank() && cost == null) {
            updateForm { copy(error = "Enter a valid cost") }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = if (form.id == null) {
                contractRepository.createContract(
                    category = form.resolvedCategory,
                    provider = form.provider.trim(),
                    plan = form.plan.trim().takeIf { it.isNotBlank() },
                    startDate = form.startDate.takeIf { it.isNotBlank() },
                    endDate = form.endDate.takeIf { it.isNotBlank() },
                    cost = cost,
                )
            } else {
                contractRepository.updateContract(
                    id = form.id,
                    category = form.resolvedCategory,
                    provider = form.provider.trim(),
                    plan = form.plan.trim().takeIf { it.isNotBlank() },
                    startDate = form.startDate.takeIf { it.isNotBlank() },
                    endDate = form.endDate.takeIf { it.isNotBlank() },
                    cost = cost,
                )
            }
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, showForm = false)
                    refresh()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    updateForm { copy(error = e.message ?: "Failed to save") }
                }
            )
        }
    }

    // Delete
    fun confirmDelete(contract: GetContractsQuery.Item) {
        _uiState.value = _uiState.value.copy(contractToDelete = contract)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(contractToDelete = null)
    }

    fun deleteContract() {
        val contract = _uiState.value.contractToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            contractRepository.deleteContract(contract.id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleting = false, contractToDelete = null)
                    refresh()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isDeleting = false)
                }
            )
        }
    }
}
