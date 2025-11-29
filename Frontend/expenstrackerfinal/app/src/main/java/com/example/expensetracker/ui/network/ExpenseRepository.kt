package com.example.expensetracker.ui.network

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ExpenseRepository {

    private val mutex = Mutex()
    private val localStore = mutableListOf<Expense>()

    suspend fun saveLocalExpense(expense: Expense) {
        mutex.withLock {
            localStore.add(0, expense)
            Log.d("ExpenseRepository", "Saved local expense: ${expense.receiver} ₹${expense.amount} on ${expense.date}")
        }
    }

    suspend fun getLocalExpenses(): List<Expense> {
        return mutex.withLock { localStore.toList() }
    }

}
