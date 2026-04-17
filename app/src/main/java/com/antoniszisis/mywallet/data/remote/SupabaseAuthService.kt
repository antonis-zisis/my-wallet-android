package com.antoniszisis.mywallet.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.antoniszisis.mywallet.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    private val dataStore: DataStore<Preferences>,
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
                saveSession(session)
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
            clearPersistedSession()
            Result.success(Unit)
        } catch (e: Exception) {
            currentSession = null
            clearPersistedSession()
            Result.success(Unit) // sign out locally even if network fails
        }
    }

    suspend fun tryRestoreSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = dataStore.data.first()
            val refreshToken = prefs[REFRESH_TOKEN_KEY] ?: return@withContext false
            refreshSession(refreshToken).fold(
                onSuccess = { session ->
                    currentSession = session
                    saveSession(session)
                    true
                },
                onFailure = {
                    clearPersistedSession()
                    false
                }
            )
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun refreshSession(refreshToken: String): Result<AuthSession> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("refresh_token", refreshToken)
                }.toString().toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$baseUrl/auth/v1/token?grant_type=refresh_token")
                    .post(body)
                    .addHeader("apikey", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Token refresh failed (${response.code})"))
                }

                val json = JSONObject(responseBody)
                Result.success(
                    AuthSession(
                        accessToken = json.getString("access_token"),
                        refreshToken = json.getString("refresh_token"),
                        userEmail = json.optJSONObject("user")?.optString("email"),
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
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

    private suspend fun saveSession(session: AuthSession) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = session.accessToken
            prefs[REFRESH_TOKEN_KEY] = session.refreshToken
            session.userEmail?.let { prefs[EMAIL_KEY] = it }
        }
    }

    private suspend fun clearPersistedSession() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(EMAIL_KEY)
        }
    }

    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("auth_access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("auth_refresh_token")
        val EMAIL_KEY = stringPreferencesKey("auth_email")
    }
}
