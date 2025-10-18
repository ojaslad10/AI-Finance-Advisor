package com.example.expensetracker.ui.network

import android.util.Log
import androidx.compose.material.icons.filled.ShoppingCart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.expensetracker.ui.models.DashboardSummary
import com.example.expensetracker.ui.dashboard.Transaction
import androidx.compose.material.icons.Icons
import java.time.LocalDate


class ExpenseRepository {

    private val mutex = Mutex()
    private val localStore = mutableListOf<Expense>()

    suspend fun saveLocalExpense(expense: Expense) {
        mutex.withLock {
            // keep newest at front
            localStore.add(0, expense)
            Log.d("ExpenseRepository", "Saved local expense: ${expense.receiver} ₹${expense.amount} on ${expense.date}")
        }
    }

    suspend fun getLocalExpenses(): List<Expense> {
        return mutex.withLock { localStore.toList() }
    }


    suspend fun getDashboardSummary(userId: String): DashboardSummary {
        return try {
            val now = LocalDate.now()
            val month = now.month
            val year = now.year

            val recent = mutableListOf<Transaction>()
            mutex.withLock {
                localStore.forEachIndexed { idx, e ->
                    // Expense.amount and other fields are non-nullable per your Expense data class
                    val amt = e.amount
                    val title = e.receiver
                    val dateRaw = e.date // keep as-is; viewmodel will format for UI
                    recent.add(
                        Transaction(
                            id = (idx + 1).toString(),
                            title = title,
                            amount = amt,
                            date = dateRaw,
                            icon = Icons.Default.ShoppingCart,
                            isIncome = (amt > 0)
                        )
                    )
                }
            }

            val totalExpenseThisMonth = localStore
                .filter { entry -> parseYearMonth(entry.date) == Pair(year, month) }
                .sumOf { if (it.amount < 0) kotlin.math.abs(it.amount) else 0.0 }

            val todaysExpense = localStore
                .filter { entry -> entry.date.startsWith(now.toString()) } // simplistic; adapt formatting
                .sumOf { if (it.amount < 0) kotlin.math.abs(it.amount) else 0.0 }

            DashboardSummary(
                bankBalance = 0.0,
                totalExpenseThisMonth = totalExpenseThisMonth,
                todaysExpense = todaysExpense,
                recentTransactions = recent.take(5)
            )
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "getDashboardSummary error", e)
            DashboardSummary.empty()
        }
    }

    private fun parseYearMonth(dateStr: String?): Pair<Int, java.time.Month>? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val ld = LocalDate.parse(dateStr)
            Pair(ld.year, ld.month)
        } catch (_: Exception) {
            null
        }
    }
}
