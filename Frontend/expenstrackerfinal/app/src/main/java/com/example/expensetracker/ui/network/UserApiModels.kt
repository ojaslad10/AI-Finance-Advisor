package com.example.expensetracker.ui.network

data class SignupRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)
data class User(
    val id: String?,
    val name: String,
    val email: String,
    val phone: String,
)
