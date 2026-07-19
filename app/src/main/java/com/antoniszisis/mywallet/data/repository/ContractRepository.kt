package com.antoniszisis.mywallet.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.antoniszisis.mywallet.graphql.CreateContractMutation
import com.antoniszisis.mywallet.graphql.DeleteContractMutation
import com.antoniszisis.mywallet.graphql.GetContractsQuery
import com.antoniszisis.mywallet.graphql.UpdateContractMutation
import com.antoniszisis.mywallet.graphql.type.ContractSortField
import com.antoniszisis.mywallet.graphql.type.CreateContractInput
import com.antoniszisis.mywallet.graphql.type.SortOrder
import com.antoniszisis.mywallet.graphql.type.UpdateContractInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    suspend fun getContracts(
        page: Int? = null,
        pageSize: Int? = null,
        expired: Boolean? = null,
        search: String? = null,
        sortBy: ContractSortField? = null,
        sortOrder: SortOrder? = null,
    ): Result<GetContractsQuery.Contracts> {
        return try {
            val response = apollo.query(
                GetContractsQuery(
                    page = Optional.presentIfNotNull(page),
                    pageSize = Optional.presentIfNotNull(pageSize),
                    expired = Optional.presentIfNotNull(expired),
                    search = Optional.presentIfNotNull(search),
                    sortBy = Optional.presentIfNotNull(sortBy),
                    sortOrder = Optional.presentIfNotNull(sortOrder),
                )
            ).execute()
            val data = response.data?.contracts ?: error("No data")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createContract(
        category: String,
        provider: String,
        plan: String?,
        startDate: String?,
        endDate: String?,
        cost: Double?,
    ): Result<CreateContractMutation.CreateContract> {
        return try {
            val response = apollo.mutation(
                CreateContractMutation(
                    input = CreateContractInput(
                        category = category,
                        provider = provider,
                        plan = Optional.presentIfNotNull(plan),
                        startDate = Optional.presentIfNotNull(startDate),
                        endDate = Optional.presentIfNotNull(endDate),
                        cost = Optional.presentIfNotNull(cost),
                    )
                )
            ).execute()
            val data = response.data?.createContract ?: error("Failed to create contract")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateContract(
        id: String,
        category: String,
        provider: String,
        plan: String?,
        startDate: String?,
        endDate: String?,
        cost: Double?,
    ): Result<UpdateContractMutation.UpdateContract> {
        return try {
            val response = apollo.mutation(
                UpdateContractMutation(
                    input = UpdateContractInput(
                        id = id,
                        category = category,
                        provider = provider,
                        plan = Optional.presentIfNotNull(plan),
                        startDate = Optional.presentIfNotNull(startDate),
                        endDate = Optional.presentIfNotNull(endDate),
                        cost = Optional.presentIfNotNull(cost),
                    )
                )
            ).execute()
            val data = response.data?.updateContract ?: error("Failed to update contract")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContract(id: String): Result<Boolean> {
        return try {
            val response = apollo.mutation(DeleteContractMutation(id = id)).execute()
            Result.success(response.data?.deleteContract ?: false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
