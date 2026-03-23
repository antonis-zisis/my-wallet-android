package com.mywallet.android.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mywallet.android.data.repository.SubscriptionRepository
import com.mywallet.android.graphql.GetSubscriptionsQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mywallet.android.util.toInputDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SubscriptionFormState(
    val id: String? = null,
    val name: String = "",
    val amount: String = "",
    val billingCycle: String = "MONTHLY",
    val startDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    val endDate: String = "",
    val error: String? = null,
)

data class SubscriptionsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val activeSubscriptions: List<GetSubscriptionsQuery.Item> = emptyList(),
    val activeTotalCount: Int = 0,
    val activeCurrentPage: Int = 1,
    val inactiveSubscriptions: List<GetSubscriptionsQuery.Item> = emptyList(),
    val inactiveTotalCount: Int = 0,
    val inactiveCurrentPage: Int = 1,
    val showInactive: Boolean = false,
    val showForm: Boolean = false,
    val form: SubscriptionFormState = SubscriptionFormState(),
    val isSaving: Boolean = false,
    val subscriptionToCancel: GetSubscriptionsQuery.Item? = null,
    val isCancelling: Boolean = false,
    val subscriptionToDelete: GetSubscriptionsQuery.Item? = null,
    val isDeleting: Boolean = false,
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val activeDeferred = async { subscriptionRepository.getSubscriptions(page = 1, active = true) }
            val inactiveDeferred = async { subscriptionRepository.getSubscriptions(page = 1, active = false) }

            val activeResult = activeDeferred.await()
            val inactiveResult = inactiveDeferred.await()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                activeSubscriptions = activeResult.getOrNull()?.items ?: emptyList(),
                activeTotalCount = activeResult.getOrNull()?.totalCount ?: 0,
                inactiveSubscriptions = inactiveResult.getOrNull()?.items ?: emptyList(),
                inactiveTotalCount = inactiveResult.getOrNull()?.totalCount ?: 0,
                error = activeResult.exceptionOrNull()?.message,
            )
        }
    }

    fun toggleShowInactive() {
        _uiState.value = _uiState.value.copy(showInactive = !_uiState.value.showInactive)
    }

    // Form
    fun showAddForm() {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            form = SubscriptionFormState(),
        )
    }

    fun showEditForm(sub: GetSubscriptionsQuery.Item) {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            form = SubscriptionFormState(
                id = sub.id,
                name = sub.name,
                amount = sub.amount.toString(),
                billingCycle = sub.billingCycle,
                startDate = toInputDate(sub.startDate),
                endDate = sub.endDate?.let { toInputDate(it) } ?: "",
            ),
        )
    }

    fun dismissForm() {
        _uiState.value = _uiState.value.copy(showForm = false)
    }

    fun onFormNameChange(v: String) = updateForm { copy(name = v, error = null) }
    fun onFormAmountChange(v: String) = updateForm { copy(amount = v, error = null) }
    fun onFormBillingCycleChange(v: String) = updateForm { copy(billingCycle = v, error = null) }
    fun onFormStartDateChange(v: String) = updateForm { copy(startDate = v, error = null) }
    fun onFormEndDateChange(v: String) = updateForm { copy(endDate = v, error = null) }

    private fun updateForm(update: SubscriptionFormState.() -> SubscriptionFormState) {
        _uiState.value = _uiState.value.copy(form = _uiState.value.form.update())
    }

    fun saveSubscription() {
        val form = _uiState.value.form
        if (form.name.isBlank()) {
            updateForm { copy(error = "Name is required") }
            return
        }
        val amount = form.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            updateForm { copy(error = "Enter a valid amount") }
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = if (form.id == null) {
                subscriptionRepository.createSubscription(
                    name = form.name.trim(),
                    amount = amount,
                    billingCycle = form.billingCycle,
                    startDate = form.startDate,
                    endDate = form.endDate.takeIf { it.isNotBlank() },
                )
            } else {
                subscriptionRepository.updateSubscription(
                    id = form.id,
                    name = form.name.trim(),
                    amount = amount,
                    billingCycle = form.billingCycle,
                    startDate = form.startDate,
                    endDate = form.endDate.takeIf { it.isNotBlank() },
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

    // Cancel
    fun confirmCancel(sub: GetSubscriptionsQuery.Item) {
        _uiState.value = _uiState.value.copy(subscriptionToCancel = sub)
    }

    fun dismissCancel() {
        _uiState.value = _uiState.value.copy(subscriptionToCancel = null)
    }

    fun cancelSubscription() {
        val sub = _uiState.value.subscriptionToCancel ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true)
            subscriptionRepository.cancelSubscription(sub.id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isCancelling = false, subscriptionToCancel = null)
                    loadAll()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isCancelling = false)
                }
            )
        }
    }

    // Delete
    fun confirmDelete(sub: GetSubscriptionsQuery.Item) {
        _uiState.value = _uiState.value.copy(subscriptionToDelete = sub)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(subscriptionToDelete = null)
    }

    fun deleteSubscription() {
        val sub = _uiState.value.subscriptionToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            subscriptionRepository.deleteSubscription(sub.id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleting = false, subscriptionToDelete = null)
                    loadAll()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isDeleting = false)
                }
            )
        }
    }
}
