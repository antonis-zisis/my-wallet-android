package com.mywallet.android.di

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.mywallet.android.BuildConfig
import com.mywallet.android.data.remote.SupabaseAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

class AuthInterceptor(private val authService: SupabaseAuthService) : ApolloInterceptor {
    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> {
        val token = authService.currentSession?.accessToken
        val newRequest = if (token != null) {
            request.newBuilder()
                .addHttpHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(newRequest)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApolloClient(authService: SupabaseAuthService): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(BuildConfig.GRAPHQL_URL)
            .addInterceptor(AuthInterceptor(authService))
            .build()
    }
}
