package com.mywallet.android.di

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.apollographql.apollo.network.http.HttpRequest
import com.apollographql.apollo.network.http.HttpResponse
import com.mywallet.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Singleton

class AuthInterceptor(private val supabase: SupabaseClient) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val token = supabase.auth.currentSessionOrNull()?.accessToken
        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
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
    fun provideApolloClient(supabase: SupabaseClient): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(BuildConfig.GRAPHQL_URL)
            .addHttpInterceptor(AuthInterceptor(supabase))
            .build()
    }
}
