package com.example.expensetracker.ui.network

data class Expense(
    val amount: Double,
    val bank: String,
    val account: String,
    val receiver: String,
    val date: String
)
