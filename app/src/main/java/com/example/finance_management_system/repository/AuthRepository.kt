package com.example.finance_management_system.repository

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
)

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun register(name: String, email: String, password: String): Result<AuthUser>
}
