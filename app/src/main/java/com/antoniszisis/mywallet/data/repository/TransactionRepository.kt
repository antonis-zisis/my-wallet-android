package com.antoniszisis.mywallet.data.repository

import com.apollographql.apollo.ApolloClient
import com.antoniszisis.mywallet.graphql.CreateTransactionMutation
import com.antoniszisis.mywallet.graphql.DeleteTransactionMutation
import com.antoniszisis.mywallet.graphql.UpdateTransactionMutation
import com.antoniszisis.mywallet.graphql.type.CreateTransactionInput
import com.antoniszisis.mywallet.graphql.type.TransactionType
import com.antoniszisis.mywallet.graphql.type.UpdateTransactionInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    suspend fun createTransaction(
        reportId: String,
        type: TransactionType,
        amount: Double,
        description: String,
        category: String,
        date: String,
    ): Result<CreateTransactionMutation.CreateTransaction> {
        return try {
            val response = apollo.mutation(
                CreateTransactionMutation(
                    input = CreateTransactionInput(
                        reportId = reportId,
                        type = type,
                        amount = amount,
                        description = description,
                        category = category,
                        date = date,
                    )
                )
            ).execute()
            val data = response.data?.createTransaction ?: error("Failed to create transaction")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        id: String,
        type: TransactionType,
        amount: Double,
        description: String,
        category: String,
        date: String,
    ): Result<UpdateTransactionMutation.UpdateTransaction> {
        return try {
            val response = apollo.mutation(
                UpdateTransactionMutation(
                    input = UpdateTransactionInput(
                        id = id,
                        type = type,
                        amount = amount,
                        description = description,
                        category = category,
                        date = date,
                    )
                )
            ).execute()
            val data = response.data?.updateTransaction ?: error("Failed to update transaction")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(id: String): Result<Boolean> {
        return try {
            val response = apollo.mutation(DeleteTransactionMutation(id = id)).execute()
            Result.success(response.data?.deleteTransaction ?: false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
