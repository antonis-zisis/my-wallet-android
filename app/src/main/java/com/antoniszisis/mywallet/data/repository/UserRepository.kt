package com.antoniszisis.mywallet.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.antoniszisis.mywallet.graphql.GetMeQuery
import com.antoniszisis.mywallet.graphql.UpdateMeMutation
import com.antoniszisis.mywallet.graphql.type.UpdateUserInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    suspend fun getMe(): Result<GetMeQuery.Me> {
        return try {
            val response = apollo.query(GetMeQuery()).execute()
            val data = response.data?.me ?: error("User not found")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMe(fullName: String?): Result<UpdateMeMutation.UpdateMe> {
        return try {
            val response = apollo.mutation(
                UpdateMeMutation(
                    input = UpdateUserInput(
                        fullName = Optional.presentIfNotNull(fullName)
                    )
                )
            ).execute()
            val data = response.data?.updateMe ?: error("Failed to update profile")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
