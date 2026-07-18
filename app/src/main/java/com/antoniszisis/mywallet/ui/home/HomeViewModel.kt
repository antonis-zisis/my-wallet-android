package com.antoniszisis.mywallet.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniszisis.mywallet.data.repository.ContractRepository
import com.antoniszisis.mywallet.data.repository.NetWorthRepository
import com.antoniszisis.mywallet.data.repository.ReportRepository
import com.antoniszisis.mywallet.data.repository.SubscriptionRepository
import com.antoniszisis.mywallet.data.repository.UserRepository
import com.antoniszisis.mywallet.graphql.GetContractsQuery
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotsQuery
import com.antoniszisis.mywallet.graphql.GetReportsSummaryQuery
import com.antoniszisis.mywallet.graphql.GetSubscriptionsQuery
import com.antoniszisis.mywallet.graphql.HealthQuery
import com.antoniszisis.mywallet.ui.contracts.CONTRACT_EXPIRING_SOON_DAYS
import com.antoniszisis.mywallet.util.getDaysUntil
import com.apollographql.apollo.ApolloClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val serverHealthy: Boolean? = null,
    val reportsSummary: GetReportsSummaryQuery.Reports? = null,
    val activeSubscriptions: GetSubscriptionsQuery.Subscriptions? = null,
    val expiringContracts: List<GetContractsQuery.Item> = emptyList(),
    val latestSnapshot: GetNetWorthSnapshotsQuery.Item? = null,
    val previousSnapshot: GetNetWorthSnapshotsQuery.Item? = null,
    val recentSnapshots: List<GetNetWorthSnapshotsQuery.Item> = emptyList(),
    val userFullName: String? = null,
    val userEmail: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val netWorthRepository: NetWorthRepository,
    private val contractRepository: ContractRepository,
    private val userRepository: UserRepository,
    private val apollo: ApolloClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            fetchDashboardData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchDashboardData()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun fetchDashboardData() = coroutineScope {
        val healthDeferred = async {
            try {
                val response = apollo.query(HealthQuery()).execute()
                response.data?.health != null
            } catch (e: Exception) {
                false
            }
        }
        val summaryDeferred = async { reportRepository.getReportsSummary() }
        val subscriptionsDeferred = async { subscriptionRepository.getSubscriptions(page = 1, active = true) }
        val snapshotsDeferred = async { netWorthRepository.getSnapshots(page = 1) }
        val contractsDeferred = async { contractRepository.getContracts(page = 1, expired = false) }
        val userDeferred = async { userRepository.getMe() }

        val healthy = healthDeferred.await()
        val summaryResult = summaryDeferred.await()
        val subscriptionsResult = subscriptionsDeferred.await()
        val snapshotsResult = snapshotsDeferred.await()
        val contractsResult = contractsDeferred.await()
        val userResult = userDeferred.await()

        val expiringContracts = (contractsResult.getOrNull()?.items ?: emptyList())
            .mapNotNull { contract ->
                val endDate = contract.endDate ?: return@mapNotNull null
                val days = getDaysUntil(endDate)
                if (days in 0..CONTRACT_EXPIRING_SOON_DAYS) contract to days else null
            }
            .sortedBy { (_, days) -> days }
            .map { (contract, _) -> contract }

        _uiState.value = _uiState.value.copy(
            serverHealthy = healthy,
            reportsSummary = summaryResult.getOrNull(),
            activeSubscriptions = subscriptionsResult.getOrNull(),
            expiringContracts = expiringContracts,
            latestSnapshot = snapshotsResult.getOrNull()?.items?.firstOrNull(),
            previousSnapshot = snapshotsResult.getOrNull()?.items?.getOrNull(1),
            recentSnapshots = snapshotsResult.getOrNull()?.items?.take(6)?.reversed() ?: emptyList(),
            userFullName = userResult.getOrNull()?.fullName,
            userEmail = userResult.getOrNull()?.email,
        )
    }
}
