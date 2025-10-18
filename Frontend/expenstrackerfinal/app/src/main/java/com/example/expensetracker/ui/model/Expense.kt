package com.example.expensetracker.ui.models

data class Expense(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val receiver: String = "",
    val category: String = "Other",
    val date: String = "",
    val bank: String = "",
    val account: String = "",
    val idempotencyKey: String? = null
)
