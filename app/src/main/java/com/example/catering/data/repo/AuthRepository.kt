package com.example.catering.data.repo

import com.example.catering.data.model.Role
import com.example.catering.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        email: String,
        password: String,
        role: Role,
        displayName: String,
        affiliation: String?,
        address: String
    ): Result<User>

    suspend fun logout()
}
