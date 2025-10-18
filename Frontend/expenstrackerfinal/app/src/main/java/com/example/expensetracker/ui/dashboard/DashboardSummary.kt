package com.example.expensetracker.ui.dashboard


data class DashboardSummary(
    val bankBalance: Double = 0.0,
    val totalExpenseThisMonth: Double = 0.0,
    val totalIncomeThisMonth: Double = 0.0,
    val todaysExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList()
)
