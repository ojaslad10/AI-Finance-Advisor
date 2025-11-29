package com.example.expensetracker.ui.network

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val user: T? = null
)

