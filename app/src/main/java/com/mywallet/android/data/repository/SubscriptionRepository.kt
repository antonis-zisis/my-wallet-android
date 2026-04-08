package com.mywallet.android.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.mywallet.android.graphql.CancelSubscriptionMutation
import com.mywallet.android.graphql.CreateSubscriptionMutation
import com.mywallet.android.graphql.DeleteSubscriptionMutation
import com.mywallet.android.graphql.GetSubscriptionsQuery
import com.mywallet.android.graphql.ResumeSubscriptionMutation
import com.mywallet.android.graphql.UpdateSubscriptionMutation
import com.mywallet.android.graphql.type.CreateSubscriptionInput
import com.mywallet.android.graphql.type.ResumeSubscriptionInput
import com.mywallet.android.graphql.type.UpdateSubscriptionInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val apollo: ApolloClient,
) {
    suspend fun getSubscriptions(
        page: Int? = null,
        active: Boolean? = null,
    ): Result<GetSubscriptionsQuery.Subscriptions> {
        return try {
            val response = apollo.query(
                GetSubscriptionsQuery(
                    page = Optional.presentIfNotNull(page),
                    active = Optional.presentIfNotNull(active),
                )
            ).execute()
            val data = response.data?.subscriptions ?: error("No data")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSubscription(
        name: String,
        amount: Double,
        billingCycle: String,
        startDate: String,
        endDate: String?,
    ): Result<CreateSubscriptionMutation.CreateSubscription> {
        return try {
            val response = apollo.mutation(
                CreateSubscriptionMutation(
                    input = CreateSubscriptionInput(
                        name = name,
                        amount = amount,
                        billingCycle = billingCycle,
                        startDate = startDate,
                        endDate = Optional.presentIfNotNull(endDate),
                    )
                )
            ).execute()
            val data = response.data?.createSubscription ?: error("Failed to create subscription")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubscription(
        id: String,
        name: String,
        amount: Double,
        billingCycle: String,
        startDate: String,
        endDate: String?,
    ): Result<UpdateSubscriptionMutation.UpdateSubscription> {
        return try {
            val response = apollo.mutation(
                UpdateSubscriptionMutation(
                    input = UpdateSubscriptionInput(
                        id = id,
                        name = name,
                        amount = amount,
                        billingCycle = billingCycle,
                        startDate = startDate,
                        endDate = Optional.presentIfNotNull(endDate),
                    )
                )
            ).execute()
            val data = response.data?.updateSubscription ?: error("Failed to update subscription")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelSubscription(id: String): Result<CancelSubscriptionMutation.CancelSubscription> {
        return try {
            val response = apollo.mutation(CancelSubscriptionMutation(id = id)).execute()
            val data = response.data?.cancelSubscription ?: error("Failed to cancel subscription")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resumeSubscription(
        id: String,
        startDate: String?,
        amount: Double?,
        billingCycle: String?,
    ): Result<ResumeSubscriptionMutation.ResumeSubscription> {
        return try {
            val response = apollo.mutation(
                ResumeSubscriptionMutation(
                    input = ResumeSubscriptionInput(
                        id = id,
                        startDate = Optional.presentIfNotNull(startDate),
                        amount = Optional.presentIfNotNull(amount),
                        billingCycle = Optional.presentIfNotNull(billingCycle),
                    )
                )
            ).execute()
            val data = response.data?.resumeSubscription ?: error("Failed to resume subscription")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubscription(id: String): Result<Boolean> {
        return try {
            val response = apollo.mutation(DeleteSubscriptionMutation(id = id)).execute()
            Result.success(response.data?.deleteSubscription ?: false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
