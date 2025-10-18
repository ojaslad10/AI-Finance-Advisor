package com.example.expensetracker.ui.network

data class ExpenseRequest(
    val amount: Double,
    val bank: String = "",
    val account: String = "",
    val receiver: String = "",
    val date: String = "",
    val category: String? = null,
    val idempotencyKey: String? = null
)

