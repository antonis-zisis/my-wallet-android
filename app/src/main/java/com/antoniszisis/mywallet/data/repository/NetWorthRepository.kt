package com.antoniszisis.mywallet.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.antoniszisis.mywallet.graphql.CreateNetWorthSnapshotMutation
import com.antoniszisis.mywallet.graphql.DeleteNetWorthSnapshotMutation
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotQuery
import com.antoniszisis.mywallet.graphql.GetNetWorthSnapshotsQuery
import com.antoniszisis.mywallet.graphql.type.CreateNetWorthSnapshotInput
import com.antoniszisis.mywallet.graphql.type.NetWorthEntryInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetWorthRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    suspend fun getSnapshots(page: Int? = null): Result<GetNetWorthSnapshotsQuery.NetWorthSnapshots> {
        return try {
            val response = apollo.query(
                GetNetWorthSnapshotsQuery(page = Optional.presentIfNotNull(page))
            ).execute()
            val data = response.data?.netWorthSnapshots ?: error("No data")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSnapshot(id: String): Result<GetNetWorthSnapshotQuery.NetWorthSnapshot> {
        return try {
            val response = apollo.query(GetNetWorthSnapshotQuery(id = id)).execute()
            val data = response.data?.netWorthSnapshot ?: error("Snapshot not found")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSnapshot(
        title: String,
        entries: List<NetWorthEntryInput>,
    ): Result<CreateNetWorthSnapshotMutation.CreateNetWorthSnapshot> {
        return try {
            val response = apollo.mutation(
                CreateNetWorthSnapshotMutation(
                    input = CreateNetWorthSnapshotInput(
                        title = title,
                        entries = entries,
                    )
                )
            ).execute()
            val data = response.data?.createNetWorthSnapshot ?: error("Failed to create snapshot")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSnapshot(id: String): Result<Boolean> {
        return try {
            val response = apollo.mutation(DeleteNetWorthSnapshotMutation(id = id)).execute()
            Result.success(response.data?.deleteNetWorthSnapshot ?: false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
