package com.mywallet.android.data.remote

import com.mywallet.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userEmail: String?,
)

@Singleton
class SupabaseAuthService @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val baseUrl = BuildConfig.SUPABASE_URL
    private val apiKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val jsonMediaType = "application/json".toMediaType()

    var currentSession: AuthSession? = null
        private set

    suspend fun signIn(email: String, password: String): Result<AuthSession> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }.toString().toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$baseUrl/auth/v1/token?grant_type=password")
                    .post(body)
                    .addHeader("apikey", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val msg = runCatching {
                        JSONObject(responseBody).optString("error_description")
                            .ifBlank { JSONObject(responseBody).optString("message") }
                    }.getOrDefault("Sign in failed (${response.code})")
                    return@withContext Result.failure(Exception(msg))
                }

                val json = JSONObject(responseBody)
                val session = AuthSession(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.getString("refresh_token"),
                    userEmail = json.optJSONObject("user")?.optString("email"),
                )
                currentSession = session
                Result.success(session)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = currentSession?.accessToken
            if (token != null) {
                val body = "{}".toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$baseUrl/auth/v1/logout")
                    .post(body)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                okHttpClient.newCall(request).execute().close()
            }
            currentSession = null
            Result.success(Unit)
        } catch (e: Exception) {
            currentSession = null
            Result.success(Unit) // sign out locally even if network fails
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = currentSession?.accessToken
            ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val body = JSONObject().apply {
                put("password", newPassword)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/user")
                .put(body)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val msg = runCatching {
                    JSONObject(response.body?.string() ?: "").optString("message", "Failed to update password")
                }.getOrDefault("Failed to update password")
                Result.failure(Exception(msg))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean = currentSession != null
    fun currentUserEmail(): String? = currentSession?.userEmail
}
