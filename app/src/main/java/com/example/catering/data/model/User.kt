package com.example.catering.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: Role = Role.CLIENT,
    val affiliation: String? = null,
    val address: String = "",
    val profileDescription: String = "",
    val joinedAt: Long = 0L
)
