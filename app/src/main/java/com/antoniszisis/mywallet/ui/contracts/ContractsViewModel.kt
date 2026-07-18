package com.antoniszisis.mywallet.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.ContractRepository
import com.antoniszisis.mywallet.graphql.GetContractsQuery
import com.antoniszisis.mywallet.util.toInputDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CONTRACT_EXPIRING_SOON_DAYS = 30

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
    val error: String? = null,
    val currentContracts: List<GetContractsQuery.Item> = emptyList(),
    val expiredContracts: List<GetContractsQuery.Item> = emptyList(),
    val expiredTotalCount: Int = 0,
    val showExpired: Boolean = false,
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

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchContracts()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            fetchContracts()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun fetchContracts() {
        val currentDeferred = viewModelScope.async { contractRepository.getContracts(page = 1, expired = false) }
        val expiredDeferred = viewModelScope.async { contractRepository.getContracts(page = 1, expired = true) }

        val currentResult = currentDeferred.await()
        val expiredResult = expiredDeferred.await()

        _uiState.value = _uiState.value.copy(
            currentContracts = currentResult.getOrNull()?.items ?: emptyList(),
            expiredContracts = expiredResult.getOrNull()?.items ?: emptyList(),
            expiredTotalCount = expiredResult.getOrNull()?.totalCount ?: 0,
            error = currentResult.exceptionOrNull()?.message,
        )
    }

    fun toggleShowExpired() {
        _uiState.value = _uiState.value.copy(showExpired = !_uiState.value.showExpired)
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
                    loadAll()
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
                    loadAll()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isDeleting = false)
                }
            )
        }
    }
}
