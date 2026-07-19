package com.antoniszisis.mywallet.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.antoniszisis.mywallet.graphql.CreateReportMutation
import com.antoniszisis.mywallet.graphql.DeleteReportMutation
import com.antoniszisis.mywallet.graphql.LeaveSharedReportMutation
import com.antoniszisis.mywallet.graphql.LockReportMutation
import com.antoniszisis.mywallet.graphql.ShareReportMutation
import com.antoniszisis.mywallet.graphql.UnlockReportMutation
import com.antoniszisis.mywallet.graphql.UnshareReportMutation
import com.antoniszisis.mywallet.graphql.UpdateReportShareRoleMutation
import com.antoniszisis.mywallet.graphql.GetReportQuery
import com.antoniszisis.mywallet.graphql.GetReportsQuery
import com.antoniszisis.mywallet.graphql.GetReportsSummaryQuery
import com.antoniszisis.mywallet.graphql.UpdateReportMutation
import com.antoniszisis.mywallet.graphql.type.CreateReportInput
import com.antoniszisis.mywallet.graphql.type.ReportRole
import com.antoniszisis.mywallet.graphql.type.ReportSortField
import com.antoniszisis.mywallet.graphql.type.ShareReportInput
import com.antoniszisis.mywallet.graphql.type.SortOrder
import com.antoniszisis.mywallet.graphql.type.UpdateReportInput
import com.antoniszisis.mywallet.graphql.type.UpdateReportShareRoleInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    suspend fun getReports(
        page: Int? = null,
        pageSize: Int? = null,
        search: String? = null,
        sortBy: ReportSortField? = null,
        sortOrder: SortOrder? = null,
    ): Result<GetReportsQuery.Reports> {
        return try {
            val response = apollo.query(
                GetReportsQuery(
                    page = Optional.presentIfNotNull(page),
                    pageSize = Optional.presentIfNotNull(pageSize),
                    search = Optional.presentIfNotNull(search),
                    sortBy = Optional.presentIfNotNull(sortBy),
                    sortOrder = Optional.presentIfNotNull(sortOrder),
                )
            ).execute()
            val data = response.data?.reports ?: error("No data")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReport(id: String): Result<GetReportQuery.Report> {
        return try {
            val response = apollo.query(GetReportQuery(id = id)).execute()
            val data = response.data?.report ?: error("Report not found")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReportsSummary(): Result<GetReportsSummaryQuery.Reports> {
        return try {
            val response = apollo.query(GetReportsSummaryQuery()).execute()
            val data = response.data?.reports ?: error("No data")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReport(title: String): Result<CreateReportMutation.CreateReport> {
        return try {
            val response = apollo.mutation(
                CreateReportMutation(input = CreateReportInput(title = title))
            ).execute()
            val data = response.data?.createReport ?: error("Failed to create report")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReport(id: String, title: String): Result<UpdateReportMutation.UpdateReport> {
        return try {
            val response = apollo.mutation(
                UpdateReportMutation(input = UpdateReportInput(id = id, title = title))
            ).execute()
            val data = response.data?.updateReport ?: error("Failed to update report")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun lockReport(id: String): Result<Unit> {
        return try {
            apollo.mutation(LockReportMutation(id = id)).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockReport(id: String): Result<Unit> {
        return try {
            apollo.mutation(UnlockReportMutation(id = id)).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReport(id: String): Result<Boolean> {
        return try {
            val response = apollo.mutation(DeleteReportMutation(id = id)).execute()
            Result.success(response.data?.deleteReport ?: false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareReport(
        reportId: String,
        email: String,
        role: ReportRole,
    ): Result<ShareReportMutation.ShareReport> {
        return try {
            val response = apollo.mutation(
                ShareReportMutation(input = ShareReportInput(reportId = reportId, email = email, role = role))
            ).execute()
            val data = response.data?.shareReport
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Failed to share report"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReportShareRole(
        shareId: String,
        role: ReportRole,
    ): Result<UpdateReportShareRoleMutation.UpdateReportShareRole> {
        return try {
            val response = apollo.mutation(
                UpdateReportShareRoleMutation(input = UpdateReportShareRoleInput(id = shareId, role = role))
            ).execute()
            val data = response.data?.updateReportShareRole
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Failed to update role"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unshareReport(shareId: String): Result<UnshareReportMutation.UnshareReport> {
        return try {
            val response = apollo.mutation(UnshareReportMutation(id = shareId)).execute()
            val data = response.data?.unshareReport
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Failed to remove member"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveSharedReport(reportId: String): Result<Boolean> {
        return try {
            val response = apollo.mutation(LeaveSharedReportMutation(reportId = reportId)).execute()
            val data = response.data?.leaveSharedReport
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Failed to leave report"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
