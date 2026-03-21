package com.mywallet.android.data.repository

import com.mywallet.android.data.remote.SupabaseAuthService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authService: SupabaseAuthService,
) {
    fun isLoggedIn(): Boolean = authService.isLoggedIn()

    suspend fun signIn(email: String, password: String): Result<Unit> =
        authService.signIn(email, password).map { }

    suspend fun signOut(): Result<Unit> = authService.signOut()

    suspend fun updatePassword(newPassword: String): Result<Unit> =
        authService.updatePassword(newPassword)

    fun currentUserEmail(): String? = authService.currentUserEmail()
}
