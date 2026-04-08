package com.mywallet.android.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mywallet.android.data.repository.ReportRepository
import com.mywallet.android.data.repository.TransactionRepository
import com.mywallet.android.graphql.GetReportQuery
import com.mywallet.android.graphql.type.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mywallet.android.util.toInputDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TransactionFormState(
    val id: String? = null,
    val type: String = "EXPENSE",
    val amount: String = "",
    val description: String = "",
    val category: String = "",
    val date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    val error: String? = null,
)

data class ReportDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val report: GetReportQuery.Report? = null,
    val isEditingTitle: Boolean = false,
    val editedTitle: String = "",
    val isSavingTitle: Boolean = false,
    val showTransactionForm: Boolean = false,
    val transactionForm: TransactionFormState = TransactionFormState(),
    val isSavingTransaction: Boolean = false,
    val showDeleteReport: Boolean = false,
    val isDeletingReport: Boolean = false,
    val transactionToDelete: GetReportQuery.Transaction? = null,
    val isDeletingTransaction: Boolean = false,
)

val EXPENSE_CATEGORIES = listOf(
    "Rent",
    "Utilities",
    "Groceries",
    "Dining Out",
    "Transport",
    "Health",
    "Entertainment",
    "Shopping",
    "Investment",
    "Insurance",
    "Loan",
    "Other",
)

val INCOME_CATEGORIES = listOf(
    "Salary", "Freelance", "Investment", "Gift", "Other"
)

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var reportId: String = ""

    fun init(reportId: String) {
        this.reportId = reportId
        loadReport()
    }

    fun loadReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            reportRepository.getReport(reportId).fold(
                onSuccess = { report ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        report = report,
                        editedTitle = report.title,
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

    // Title editing
    fun startEditTitle() {
        _uiState.value = _uiState.value.copy(
            isEditingTitle = true,
            editedTitle = _uiState.value.report?.title ?: "",
        )
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(editedTitle = title)
    }

    fun saveTitle() {
        val title = _uiState.value.editedTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingTitle = true)
            reportRepository.updateReport(reportId, title).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSavingTitle = false,
                        isEditingTitle = false,
                    )
                    loadReport()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isSavingTitle = false)
                }
            )
        }
    }

    fun cancelEditTitle() {
        _uiState.value = _uiState.value.copy(isEditingTitle = false)
    }

    // Transaction form
    fun showAddTransaction() {
        _uiState.value = _uiState.value.copy(
            showTransactionForm = true,
            transactionForm = TransactionFormState(),
        )
    }

    fun showEditTransaction(tx: GetReportQuery.Transaction) {
        _uiState.value = _uiState.value.copy(
            showTransactionForm = true,
            transactionForm = TransactionFormState(
                id = tx.id,
                type = tx.type.rawValue,
                amount = tx.amount.toString(),
                description = tx.description,
                category = tx.category,
                date = toInputDate(tx.date),
            ),
        )
    }

    fun dismissTransactionForm() {
        _uiState.value = _uiState.value.copy(showTransactionForm = false)
    }

    fun onTransactionTypeChange(type: String) {
        val defaultCategory = if (type == "INCOME") INCOME_CATEGORIES.first() else EXPENSE_CATEGORIES.first()
        _uiState.value = _uiState.value.copy(
            transactionForm = _uiState.value.transactionForm.copy(
                type = type,
                category = defaultCategory,
                error = null,
            )
        )
    }

    fun onTransactionAmountChange(amount: String) {
        _uiState.value = _uiState.value.copy(
            transactionForm = _uiState.value.transactionForm.copy(amount = amount, error = null)
        )
    }

    fun onTransactionDescriptionChange(desc: String) {
        _uiState.value = _uiState.value.copy(
            transactionForm = _uiState.value.transactionForm.copy(description = desc, error = null)
        )
    }

    fun onTransactionCategoryChange(cat: String) {
        _uiState.value = _uiState.value.copy(
            transactionForm = _uiState.value.transactionForm.copy(category = cat, error = null)
        )
    }

    fun onTransactionDateChange(date: String) {
        _uiState.value = _uiState.value.copy(
            transactionForm = _uiState.value.transactionForm.copy(date = date, error = null)
        )
    }

    fun saveTransaction() {
        val form = _uiState.value.transactionForm
        val amount = form.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(
                transactionForm = form.copy(error = "Enter a valid amount")
            )
            return
        }
        if (form.description.isBlank()) {
            _uiState.value = _uiState.value.copy(
                transactionForm = form.copy(error = "Description is required")
            )
            return
        }
        val type = if (form.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingTransaction = true)
            val result = if (form.id == null) {
                transactionRepository.createTransaction(
                    reportId = reportId,
                    type = type,
                    amount = amount,
                    description = form.description.trim(),
                    category = form.category,
                    date = form.date,
                )
            } else {
                transactionRepository.updateTransaction(
                    id = form.id,
                    type = type,
                    amount = amount,
                    description = form.description.trim(),
                    category = form.category,
                    date = form.date,
                )
            }
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSavingTransaction = false,
                        showTransactionForm = false,
                    )
                    loadReport()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSavingTransaction = false,
                        transactionForm = form.copy(error = e.message ?: "Failed to save"),
                    )
                }
            )
        }
    }

    // Delete transaction
    fun confirmDeleteTransaction(tx: GetReportQuery.Transaction) {
        _uiState.value = _uiState.value.copy(transactionToDelete = tx)
    }

    fun dismissDeleteTransaction() {
        _uiState.value = _uiState.value.copy(transactionToDelete = null)
    }

    fun deleteTransaction() {
        val tx = _uiState.value.transactionToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingTransaction = true)
            transactionRepository.deleteTransaction(tx.id).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isDeletingTransaction = false,
                        transactionToDelete = null,
                    )
                    loadReport()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isDeletingTransaction = false)
                }
            )
        }
    }

    fun toggleLock() {
        val current = _uiState.value.report ?: return
        viewModelScope.launch {
            val result = if (current.isLocked) {
                reportRepository.unlockReport(reportId)
            } else {
                reportRepository.lockReport(reportId)
            }
            result.onSuccess { loadReport() }
        }
    }

    // Delete report
    fun showDeleteReport() {
        _uiState.value = _uiState.value.copy(showDeleteReport = true)
    }

    fun dismissDeleteReport() {
        _uiState.value = _uiState.value.copy(showDeleteReport = false)
    }

    fun deleteReport(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingReport = true)
            reportRepository.deleteReport(reportId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeletingReport = false)
                    onDeleted()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isDeletingReport = false)
                }
            )
        }
    }
}
