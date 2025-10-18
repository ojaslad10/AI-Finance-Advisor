package com.example.expensetracker.ui.network

data class ChatRequest(
    val userId: String,
    val message: String,
    val window: Int = 3
)

data class ChatResponse(
    val success: Boolean,
    val reply: String?,
    val debug: Map<String, Any>? = null
)