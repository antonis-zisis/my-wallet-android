package com.mywallet.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mywallet.android.data.repository.NetWorthRepository
import com.mywallet.android.data.repository.ReportRepository
import com.mywallet.android.data.repository.SubscriptionRepository
import com.mywallet.android.data.repository.UserRepository
import com.mywallet.android.graphql.GetNetWorthSnapshotsQuery
import com.mywallet.android.graphql.GetReportsSummaryQuery
import com.mywallet.android.graphql.GetSubscriptionsQuery
import com.mywallet.android.graphql.HealthQuery
import com.apollographql.apollo.ApolloClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val serverHealthy: Boolean? = null,
    val reportsSummary: GetReportsSummaryQuery.Reports? = null,
    val activeSubscriptions: GetSubscriptionsQuery.Subscriptions? = null,
    val latestSnapshot: GetNetWorthSnapshotsQuery.Item? = null,
    val userFullName: String? = null,
    val userEmail: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val netWorthRepository: NetWorthRepository,
    private val userRepository: UserRepository,
    private val apollo: ApolloClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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
            val userDeferred = async { userRepository.getMe() }

            val healthy = healthDeferred.await()
            val summaryResult = summaryDeferred.await()
            val subscriptionsResult = subscriptionsDeferred.await()
            val snapshotsResult = snapshotsDeferred.await()
            val userResult = userDeferred.await()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                serverHealthy = healthy,
                reportsSummary = summaryResult.getOrNull(),
                activeSubscriptions = subscriptionsResult.getOrNull(),
                latestSnapshot = snapshotsResult.getOrNull()?.items?.firstOrNull(),
                userFullName = userResult.getOrNull()?.fullName,
                userEmail = userResult.getOrNull()?.email,
            )
        }
    }
}
