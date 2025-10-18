package com.example.expensetracker.ui.models

import com.example.expensetracker.ui.dashboard.Transaction


data class DashboardSummary(
    val bankBalance: Double = 0.0,
    val totalExpenseThisMonth: Double = 0.0,
    val totalIncomeThisMonth: Double = 0.0,
    val todaysExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList()
) {
    companion object {
        fun empty(): DashboardSummary {
            return DashboardSummary(
                bankBalance = 0.0,
                totalExpenseThisMonth = 0.0,
                totalIncomeThisMonth = 0.0,
                todaysExpense = 0.0,
                recentTransactions = emptyList()
            )
        }
    }
}

