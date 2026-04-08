package com.mywallet.android.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.mywallet.android.graphql.CreateReportMutation
import com.mywallet.android.graphql.DeleteReportMutation
import com.mywallet.android.graphql.LockReportMutation
import com.mywallet.android.graphql.UnlockReportMutation
import com.mywallet.android.graphql.GetReportQuery
import com.mywallet.android.graphql.GetReportsQuery
import com.mywallet.android.graphql.GetReportsSummaryQuery
import com.mywallet.android.graphql.UpdateReportMutation
import com.mywallet.android.graphql.type.CreateReportInput
import com.mywallet.android.graphql.type.UpdateReportInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    suspend fun getReports(
        page: Int? = null,
        pageSize: Int? = null,
    ): Result<GetReportsQuery.Reports> {
        return try {
            val response = apollo.query(
                GetReportsQuery(
                    page = Optional.presentIfNotNull(page),
                    pageSize = Optional.presentIfNotNull(pageSize),
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
}
