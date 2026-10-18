package com.tetrova_flowledger.repository

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
)

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun register(name: String, email: String, password: String): Result<AuthUser>
    suspend fun loginWithGoogle(idToken: String): Result<AuthUser>
    suspend fun sendPasswordReset(email: String): Result<Unit>
}
