package com.example.expensetracker.ui.network

// Simple generic wrapper for backend responses that return success / message / data
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val user: T? = null
)

