package com.antoniszisis.mywallet.di

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.antoniszisis.mywallet.BuildConfig
import com.antoniszisis.mywallet.data.remote.SupabaseAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
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

class TokenRefreshAuthenticator(private val authService: SupabaseAuthService) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount() > 1) return null
        val refreshResult = runBlocking { authService.refreshCurrentSession() }
        val newToken = authService.currentSession?.accessToken ?: return null
        return if (refreshResult.isSuccess) {
            response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        } else null
    }

    private fun Response.responseCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApolloClient(authService: SupabaseAuthService): ApolloClient {
        val apolloOkHttpClient = OkHttpClient.Builder()
            .authenticator(TokenRefreshAuthenticator(authService))
            .build()
        return ApolloClient.Builder()
            .serverUrl(BuildConfig.GRAPHQL_URL)
            .httpEngine(DefaultHttpEngine(apolloOkHttpClient))
            .addInterceptor(AuthInterceptor(authService))
            .build()
    }
}
