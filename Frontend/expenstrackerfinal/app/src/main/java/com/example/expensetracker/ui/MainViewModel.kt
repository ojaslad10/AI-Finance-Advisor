package com.example.expensetracker.ui

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ThumbUp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.expensetracker.App
import com.example.expensetracker.ui.ai.ChatMessage
import com.example.expensetracker.ui.dashboard.DashboardSummary
import com.example.expensetracker.ui.dashboard.Transaction
import com.example.expensetracker.ui.emi.EMIItem
import com.example.expensetracker.ui.network.Expense
import com.example.expensetracker.ui.network.ExpenseRequest
import com.example.expensetracker.ui.network.LoginRequest
import com.example.expensetracker.ui.network.RetrofitClient
import com.example.expensetracker.ui.network.SignupRequest
import com.example.expensetracker.ui.network.User
import com.example.expensetracker.ui.session.SessionManager
import com.example.expensetracker.ui.sms.NotificationHelper
import com.example.expensetracker.ui.sms.SmsParser
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.expensetracker.ui.dashboard.CategoryExpense
import com.example.expensetracker.ui.dashboard.defaultCategoryExpenses


data class ParsedSms(
    val amount: Double,
    val bank: String,
    val account: String,
    val receiver: String,
    val date: String
)

class MainViewModel(

) : ViewModel() {

    var bankBalance = mutableStateOf(0.0)
        private set

    var todaysExpense = mutableStateOf(0.0)
        private set

    val transactions = mutableStateListOf<Transaction>()
    val upcomingEmis = mutableStateListOf<EMIItem>()
    var currentUser: User? = null
    private val allEmis = mutableListOf<EMIItem>()




    private val uiDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val emiUiFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
    private val dmy2Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH)
    private val dmy4Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
    // Dashboard state exposed to UI
    private val _dashboardState = MutableStateFlow<DashboardSummary?>(null)
    val dashboardState: StateFlow<DashboardSummary?> = _dashboardState
    private val repository = com.example.expensetracker.ui.network.ExpenseRepository()
    private val _localTransactions = MutableStateFlow<List<com.example.expensetracker.ui.network.Expense>>(emptyList())
    val localTransactions: StateFlow<List<com.example.expensetracker.ui.network.Expense>> = _localTransactions.asStateFlow()

    val _categoryExpenses = MutableStateFlow<List<com.example.expensetracker.ui.dashboard.CategoryExpense>>(defaultCategoryExpenses())
    val categoryExpenses: StateFlow<List<com.example.expensetracker.ui.dashboard.CategoryExpense>> = _categoryExpenses.asStateFlow()


    init {
        transactions.add(Transaction("1", "Initial Balance", 0.0, LocalDate.now().format(uiDateFormatter), Icons.Default.ShoppingCart, true))
        recomputeTodaysExpense()
    }

    private fun parseDateFlexible(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val s = dateStr.trim()
        val attempts = listOf<(String) -> LocalDate?>(
            { str -> try { LocalDate.parse(str, isoFormatter) } catch (_: Exception) { null } },
            { str -> try { LocalDate.parse(str, dmy4Formatter) } catch (_: Exception) { null } },
            { str -> try { LocalDate.parse(str, dmy2Formatter) } catch (_: Exception) { null } },
            { str -> try { LocalDate.parse(str, uiDateFormatter) } catch (_: Exception) { null } },
            { str -> try { LocalDate.parse(str) } catch (_: Exception) { null } }
        )
        for (t in attempts) {
            val res = t(s)
            if (res != null) return res
        }
        return null
    }



    private fun formatToUi(date: LocalDate): String = date.format(uiDateFormatter)

    fun addLocalTransaction(title: String, amount: Double, date: String, category: String? = null) {
        val exp = Expense(
            amount = amount,
            bank = "",
            account = "",
            receiver = title,
            date = date
        )

        viewModelScope.launch {
            try {
                repository.saveLocalExpense(exp)

                val newest = repository.getLocalExpenses()
                _localTransactions.value = newest

                transactions.add(
                    Transaction(
                        id = (transactions.size + 1).toString(),
                        title = title,
                        amount = amount,
                        date = date,
                        icon = androidx.compose.material.icons.Icons.Default.ShoppingCart,
                        isIncome = amount > 0
                    )
                )

                fetchDashboardData()
                recomputeTodaysExpense()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to save local transaction", e)
            }
        }
    }




    private fun sendInternalExpenseBroadcast(
        idempotency: String?,
        amount: Double,
        receiver: String,
        category: String,
        date: String,
        direction: String
    ) {
        try {
            val ctx = App.context ?: return
            val intent = android.content.Intent(NotificationHelper.ACTION_EXPENSE_ADDED)
            intent.putExtra(NotificationHelper.EXTRA_IDEMPOTENCY, idempotency)
            intent.putExtra(NotificationHelper.EXTRA_AMOUNT, amount)
            intent.putExtra(NotificationHelper.EXTRA_RECEIVER, receiver)
            intent.putExtra(NotificationHelper.EXTRA_CATEGORY, category)
            intent.putExtra(NotificationHelper.EXTRA_DATE, date)
            intent.putExtra(NotificationHelper.EXTRA_DIRECTION, direction)
            ctx.sendBroadcast(intent)
            Log.d("MainViewModel", "Internal expense broadcast sent")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to send internal broadcast: ${e.message}", e)
        }
    }


    private fun recomputeTodaysExpense() {
        val today = LocalDate.now()
        val sum = transactions.asSequence()
            .mapNotNull { tx -> parseDateFlexible(tx.date)?.let { Pair(tx, it) } }
            .filter { (_, date) -> date == today }
            .map { (tx, _) -> if (tx.amount < 0) abs(tx.amount) else 0.0 }
            .sum()
        todaysExpense.value = sum
        Log.d("MainViewModel", "recomputeTodaysExpense -> $sum")
    }


    fun computeDashboardSummary(): com.example.expensetracker.ui.dashboard.DashboardSummary {
        try {
            val now = LocalDate.now()
            val month = now.month
            val year = now.year

            var totalIncomeMonth = 0.0
            var totalExpenseMonth = 0.0
            var todaysExpenseLocal = 0.0

            val recent = mutableListOf<com.example.expensetracker.ui.dashboard.Transaction>()

            transactions.forEach { tx ->
                val parsed = parseDateFlexible(tx.date)
                if (parsed != null && parsed.month == month && parsed.year == year) {
                    if (tx.amount > 0) totalIncomeMonth += tx.amount else totalExpenseMonth += kotlin.math.abs(tx.amount)
                }
                if (parsed != null && parsed == now && tx.amount < 0) {
                    todaysExpenseLocal += kotlin.math.abs(tx.amount)
                }
                recent.add(tx)
            }

            val sortedRecent = recent.sortedWith(
                compareByDescending<com.example.expensetracker.ui.dashboard.Transaction> { parseDateFlexible(it.date) ?: LocalDate.MIN }
                    .thenComparator { a, b ->
                        val na = a.id.toLongOrNull()
                        val nb = b.id.toLongOrNull()
                        if (na != null && nb != null) nb.compareTo(na) else b.id.compareTo(a.id)
                    }
            ).take(5)

            return com.example.expensetracker.ui.dashboard.DashboardSummary(
                bankBalance = bankBalance.value,
                totalExpenseThisMonth = totalExpenseMonth,
                totalIncomeThisMonth = totalIncomeMonth,
                todaysExpense = todaysExpenseLocal,
                recentTransactions = sortedRecent
            )
        } catch (e: Exception) {
            // fallback
            return com.example.expensetracker.ui.dashboard.DashboardSummary(
                bankBalance = bankBalance.value,
                totalExpenseThisMonth = 0.0,
                totalIncomeThisMonth = 0.0,
                todaysExpense = 0.0,
                recentTransactions = transactions.takeLast(5).reversed()
            )
        }
    }


    fun refreshTodaysExpense() = recomputeTodaysExpense()

    private fun parseExpenseListFromResponse(raw: Any?): List<Map<String, Any>> {
        val out = mutableListOf<Map<String, Any>>()
        if (raw == null) return out

        when (raw) {
            is List<*> -> raw.filterIsInstance<Map<*, *>>().forEach { m ->
                out.add(m.mapKeys { it.key.toString() }.mapValues { it.value as Any })
            }
            is Map<*, *> -> {
                val keysToCheck = listOf("expenses", "data", "result", "payload")
                for (k in keysToCheck) {
                    val v = raw[k]
                    if (v is List<*>) {
                        v.filterIsInstance<Map<*, *>>().forEach { m ->
                            out.add(m.mapKeys { it.key.toString() }.mapValues { it.value as Any })
                        }
                        if (out.isNotEmpty()) return out
                    } else if (v is Map<*, *>) {
                        out.add(v.mapKeys { it.key.toString() }.mapValues { it.value as Any })
                        return out
                    }
                }

                if (raw.containsKey("amount") || raw.containsKey("receiver") || raw.containsKey("date") || raw.containsKey("createdAt")) {
                    out.add(raw.mapKeys { it.key.toString() }.mapValues { it.value as Any })
                } else {
                    // try nested lists anywhere
                    raw.values.forEach { v ->
                        if (v is List<*>) {
                            v.filterIsInstance<Map<*, *>>().forEach { m ->
                                out.add(m.mapKeys { it.key.toString() }.mapValues { it.value as Any })
                            }
                        }
                    }
                }
            }
        }
        return out
    }

    fun setBankBalance(amount: Double) {
        bankBalance.value = amount
        App.context?.let { ctx ->
            val token = SessionManager(ctx).getToken()
            if (token.isNullOrEmpty()) {
                Log.d("MainViewModel", "No token available - skipping setBalance network call")
                return@let
            }
            viewModelScope.launch {
                try {
                    RetrofitClient.api.setBalance("Bearer $token", mapOf("balance" to amount))
                    Log.d("MainViewModel", "setBalance called on backend")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to set balance on backend", e)
                }
            }
        }
    }


    fun addExpenseFromSms(message: String, sender: String = "unknown") {
        val parsed = SmsParser.parse(message, sender, null)
        if (parsed == null) {
            Log.d("MainViewModel", "SMS parse failed for: $message")
            return
        }

        viewModelScope.launch {
            try {
                val signedAmount = when (parsed.direction.lowercase()) {
                    "credit" -> kotlin.math.abs(parsed.amount)
                    "debit"  -> -kotlin.math.abs(parsed.amount)
                    else     -> -kotlin.math.abs(parsed.amount) // default treat as debit
                }

                bankBalance.value = bankBalance.value + signedAmount

                val displayDate = parseDateFlexible(parsed.date)?.let { formatToUi(it) }
                    ?: LocalDate.now().format(uiDateFormatter)

                val title = if (signedAmount >= 0) "From ${parsed.receiver}" else "To ${parsed.receiver}"

                addLocalTransaction(title, signedAmount, displayDate)

                App.context?.let { ctx ->
                    val token = SessionManager(ctx).getToken()
                    if (!token.isNullOrEmpty()) {
                        try {
                            RetrofitClient.api.addExpense(
                                "Bearer $token",
                                ExpenseRequest(
                                    amount = signedAmount, // positive = credit, negative = debit
                                    bank = parsed.bank,
                                    account = parsed.account,
                                    receiver = parsed.receiver,
                                    date = parsed.date
                                )
                            )
                            Log.d("MainViewModel", "Expense from SMS sent to backend (idempotency=${parsed.idempotencyKey})")
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error saving SMS expense to backend", e)
                        }
                    } else {
                        Log.d("MainViewModel", "No token: SMS expense kept locally")
                    }
                }
                recomputeTodaysExpense()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error in addExpenseFromSms", e)
            }
        }
    }


    fun addManualExpense(
        title: String,
        amount: Double,
        date: String = LocalDate.now().format(uiDateFormatter),
        category: String? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val outgoing = abs(amount)

                bankBalance.value = (bankBalance.value - outgoing)

                addLocalTransaction(title, -outgoing, date, category)

                fetchDashboardData()

                App.context?.let { ctx ->
                    val token = SessionManager(ctx).getToken()
                    if (!token.isNullOrEmpty()) {
                        try {
                            // If your backend ExpenseRequest supports category, send it
                            RetrofitClient.api.addExpense(
                                "Bearer $token",
                                ExpenseRequest(
                                    amount = outgoing,   // positive outgoing amount (backend logic)
                                    bank = "",
                                    account = "",
                                    receiver = title,
                                    category = category ?: "Other", // include category if backend accepts it
                                    date = date
                                )
                            )
                            Log.d("MainViewModel", "Manual expense sent to backend")
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error saving manual expense to backend", e)
                        }
                    } else {
                        Log.d("MainViewModel", "No token: manual expense stored locally only")
                    }
                }

                onResult?.invoke(true, "Expense added")
                sendInternalExpenseBroadcast(idempotency = null, amount = outgoing, receiver = title, category = category ?: "Other", date = date, direction = "debit")
            } catch (e: Exception) {
                onResult?.invoke(false, e.message)
            }
        }
    }


    fun refreshEmisForToday() {
        Log.d("MainViewModel", "refreshEmisForToday: current upcomingEmis size=${upcomingEmis.size}")
        val today = LocalDate.now()
        val iterator = upcomingEmis.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            val parsed = try { LocalDate.parse(e.date, emiUiFormatter) } catch (ex: Exception) { null }
            if (parsed != null && parsed.isBefore(today)) {

            }
        }
    }

    fun payEmi(emi: com.example.expensetracker.ui.emi.EMIItem, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                upcomingEmis.removeAll { it.id == emi.id }

                // add a local transaction (expense)
                addLocalTransaction(emi.title, -emi.amount, LocalDate.now().format(uiDateFormatter))
                bankBalance.value -= emi.amount

                val idx = allEmis.indexOfFirst { it.id == emi.id }
                if (emi.periodMonths != null && emi.periodMonths > 0) {
                    val parsed = parseDateFlexible(emi.nextDueIso) ?: LocalDate.now()
                    val next = parsed.plusMonths(emi.periodMonths.toLong())
                    val updated = emi.copy(nextDueIso = next.toString(), date = formatToUi(next))
                    if (idx >= 0) allEmis[idx] = updated else allEmis.add(updated)
                    // note: we intentionally DO NOT add it back to upcomingEmis until the due date arrives
                } else {
                    if (idx >= 0) allEmis.removeAt(idx)
                }

                // sync to backend if logged in (post expense)
                App.context?.let { ctx ->
                    val token = SessionManager(ctx).getToken()
                    if (!token.isNullOrEmpty()) {
                        try {
                            RetrofitClient.api.addExpense(
                                "Bearer $token",
                                com.example.expensetracker.ui.network.ExpenseRequest(
                                    amount = emi.amount,
                                    bank = emi.bank,
                                    account = emi.account,
                                    receiver = emi.title,
                                    date = LocalDate.now().toString()
                                )
                            )
                            Log.d("MainViewModel", "EMI expense sent to backend")
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error saving EMI expense to backend", e)
                        }
                    }
                }

                // recompute today's totals
                refreshTodaysExpense()
                onResult?.invoke(true, "EMI handled")
            } catch (e: Exception) {
                onResult?.invoke(false, e.message)
            }
        }
    }


    fun loadEmisFromBackendOrPersisted(listFromBackend: List<EMIItem>) {
        allEmis.clear()
        allEmis.addAll(listFromBackend)
        refreshEmisForToday()
    }


    fun fetchExpensesAndBalance(tokenHeader: String) {
        viewModelScope.launch {
            try {
                // Balance
                try {
                    val balanceResp: Map<String, Any> = RetrofitClient.api.getBalance(tokenHeader)
                    Log.d("MainViewModel", "getBalance raw: $balanceResp")
                    val balAny = balanceResp["balance"] ?: (balanceResp["data"] as? Map<*, *>)?.get("balance")
                    val parsedBalance = when (balAny) {
                        is Number -> balAny.toDouble()
                        is String -> balAny.toDoubleOrNull()
                        else -> null
                    }
                    parsedBalance?.let {
                        bankBalance.value = it
                        Log.d("MainViewModel", "bankBalance set from server: $it")
                    }
                } catch (be: Exception) {
                    Log.w("MainViewModel", "Failed to fetch balance: ${be.message}")
                }

                try {
                    val expensesRaw: Any? = RetrofitClient.api.getExpenseSummary(tokenHeader)
                    Log.d("MainViewModel", "getExpenseSummary raw: $expensesRaw")
                    val list = parseExpenseListFromResponse(expensesRaw)
                    transactions.clear()
                    // optional starter
                    transactions.add(Transaction("1", "Initial Balance", 0.0, LocalDate.now().format(uiDateFormatter), Icons.Default.ShoppingCart, true))

                    list.forEach { e ->
                        val title = e["receiver"]?.toString() ?: e["title"]?.toString() ?: "Expense"
                        val amountNum = (e["amount"] as? Number)?.toDouble()
                            ?: (e["amount"] as? String)?.toDoubleOrNull()
                            ?: 0.0
                        val dateRaw = e["date"]?.toString() ?: e["createdAt"]?.toString() ?: LocalDate.now().toString()
                        val parsedDate = parseDateFlexible(dateRaw) ?: LocalDate.now()
                        val dateUi = formatToUi(parsedDate)

                        transactions.add(
                            Transaction(
                                id = (transactions.size + 1).toString(),
                                title = title,
                                amount = -abs(amountNum),
                                date = dateUi,
                                icon = Icons.Default.ShoppingCart,
                                isIncome = false
                            )
                        )
                    }

                    recomputeTodaysExpense()
                    Log.d("MainViewModel", "Loaded ${transactions.size} transactions from server; today's expense=${todaysExpense.value}")
                } catch (ee: Exception) {
                    Log.e("MainViewModel", "Failed to fetch expenses: ${ee.message}", ee)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "fetchExpensesAndBalance error", e)
            }
        }
    }

    fun restoreSession(onResult: (Boolean) -> Unit) {
        App.context?.let { ctx ->
            val token = SessionManager(ctx).getToken()
            if (token.isNullOrEmpty()) { onResult(false); return }
            viewModelScope.launch {
                try {
                    val userResp: com.example.expensetracker.ui.network.User = RetrofitClient.api.getCurrentUser("Bearer $token")
                    // set current user
                    currentUser = userResp

                    try {
                        val balanceResp = RetrofitClient.api.getBalance("Bearer $token")
                        val parsed = parseBalanceFromResponse(balanceResp)
                        if (parsed != null) {
                            bankBalance.value = parsed
                            Log.d("MainViewModel", "restoreSession: bankBalance restored = $parsed")
                        } else {
                            Log.d("MainViewModel", "restoreSession: balance present but could not parse: $balanceResp")
                        }
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "restoreSession: failed to fetch balance: ${e.message}")
                    }

                    fetchExpensesFromBackendAndPopulate()

                    onResult(true)
                } catch (e: Exception) {
                    // if backend returns 401/invalid token, clear stored token
                    SessionManager(ctx).clearToken()
                    Log.e("MainViewModel", "restoreSession error", e)
                    onResult(false)
                }
            }
        } ?: run { onResult(false) }
    }





    fun signup(name: String, email: String, phone: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.signup(SignupRequest(name, email, phone, password))
                if (r.success) onResult(true, "Signup successful") else onResult(false, r.message)
            } catch (e: Exception) {
                Log.e("MainViewModel", "signup error", e)
                onResult(false, e.message)
            }
        }
    }
    // in ViewModel

    fun fetchDashboardData() {
        viewModelScope.launch {
            try {
                // locally compute summary instead of calling repository
                val summary = computeDashboardSummary()
                _dashboardState.value = summary
                Log.d("MainViewModel", "Dashboard data updated locally")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to refresh dashboard data", e)
            }
        }
    }



    private fun parseBalanceFromResponse(resp: Map<String, Any>?): Double? {
        if (resp == null) return null
        try {
            (resp["balance"] as? Number)?.let { return it.toDouble() }
            (resp["balance"] as? String)?.toDoubleOrNull()?.let { return it }

            val data = resp["data"]
            if (data is Map<*, *>) {
                (data["balance"] as? Number)?.let { return it.toDouble() }
                (data["balance"] as? String)?.toDoubleOrNull()?.let { return it }
            }

            val user = resp["user"]
            if (user is Map<*, *>) {
                (user["balance"] as? Number)?.let { return it.toDouble() }
                (user["balance"] as? String)?.toDoubleOrNull()?.let { return it }
            }

            resp.values.forEach { v ->
                when (v) {
                    is Number -> return v.toDouble()
                    is String -> v.toDoubleOrNull()?.let { return it }
                    is Map<*, *> -> {
                        v.values.forEach { nested ->
                            when (nested) {
                                is Number -> return nested.toDouble()
                                is String -> nested.toDoubleOrNull()?.let { return it }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "parseBalanceFromResponse error", e)
        }
        return null
    }


    fun handleIncomingExpense(
        idempotencyKey: String,
        receiverName: String,
        amount: Double,
        date: String,
        category: String,
        direction: String
    ) {
        viewModelScope.launch {
            try {
                val title = if (receiverName.isNotBlank()) receiverName else category
                val signedAmount = if (direction.equals("credit", ignoreCase = true)) {
                    kotlin.math.abs(amount)
                } else {
                    -kotlin.math.abs(amount)
                }

                // 1️⃣ Add to local transactions immediately
                val displayDate = date.ifBlank { LocalDate.now().toString() }
                transactions.add(
                    com.example.expensetracker.ui.dashboard.Transaction(
                        id = (transactions.size + 1).toString(),
                        title = title,
                        amount = signedAmount,
                        date = displayDate,
                        icon = androidx.compose.material.icons.Icons.Default.ShoppingCart,
                        isIncome = signedAmount > 0
                    )
                )

                bankBalance.value += signedAmount
                recomputeTodaysExpense()
                updateDashboardState()
                fetchDashboardData()

                Log.d("MainViewModel", "handleIncomingExpense: Added '$title' ₹$amount ($direction) -> Dashboard updated")

            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to handle incoming expense", e)
            }
        }
    }


    private fun updateDashboardState() {
        _dashboardState.value = DashboardSummary(
            bankBalance = bankBalance.value,
            totalExpenseThisMonth = getTotalExpenseThisMonth(),
            todaysExpense = getTodaysExpense(),
            recentTransactions = getRecentTransactions()
        )
    }




    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.login(LoginRequest(email, password))
                if (!(resp.success && resp.token != null)) {
                    onResult(false, resp.message ?: "Login failed")
                    return@launch
                }

                currentUser = resp.user
                App.context?.let { ctx ->
                    SessionManager(ctx).saveToken(resp.token)
                    Log.d("MainViewModel", "Saved token after login: ${resp.token.take(40)}...")
                }

                try {
                    try {
                        val balanceResp = RetrofitClient.api.getBalance("Bearer ${resp.token}")
                        val parsedBalance = parseBalanceFromResponse(balanceResp)
                        parsedBalance?.let {
                            bankBalance.value = it
                            Log.d("MainViewModel", "Bank balance restored: $it")
                        }
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "Non-fatal: error fetching balance after login", e)
                    }

                    try {
                        val listResp = RetrofitClient.api.getExpenses("Bearer ${resp.token}")
                        Log.d("MainViewModel", "getExpenses response: $listResp")

                        val raw = listResp["expenses"] ?: listResp["data"] ?: listResp
                        val expenseList = when (raw) {
                            is List<*> -> raw.filterIsInstance<Map<String, Any>>()
                            else -> emptyList()
                        }

                        transactions.clear()
                        transactions.add(
                            Transaction(
                                id = "1",
                                title = "Initial Balance",
                                amount = 0.0,
                                date = LocalDate.now().format(uiDateFormatter),
                                icon = Icons.Default.ShoppingCart,
                                isIncome = true
                            )
                        )

                        expenseList.forEach { e ->
                            try {
                                val title = e["receiver"]?.toString() ?: e["title"]?.toString() ?: "Expense"
                                val amount = (e["amount"] as? Number)?.toDouble()
                                    ?: (e["amount"] as? String)?.toDoubleOrNull()
                                    ?: 0.0
                                val dateRaw = e["date"]?.toString() ?: e["createdAt"]?.toString() ?: LocalDate.now().toString()
                                val parsedDate = parseDateFlexible(dateRaw) ?: LocalDate.now()
                                val dateUi = parsedDate.format(uiDateFormatter)

                                transactions.add(
                                    Transaction(
                                        id = (transactions.size + 1).toString(),
                                        title = title,
                                        amount = -kotlin.math.abs(amount),
                                        date = dateUi,
                                        icon = Icons.Default.ShoppingCart,
                                        isIncome = false
                                    )
                                )
                            } catch (inner: Exception) {
                                // log per-item error but continue processing remaining items
                                Log.e("MainViewModel", "Failed to parse one expense item: $e", inner)
                            }
                        }

                        refreshTodaysExpense()
                        Log.d("MainViewModel", "Transactions loaded after login: ${transactions.size}")
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Non-fatal: error fetching/processing expenses after login", e)
                        // keep UI stable even on error
                    }
                } catch (fatal: Throwable) {
                    Log.e("MainViewModel", "Unexpected fatal error while processing login response", fatal)
                }

                onResult(true, "Login successful")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Login error", e)
                onResult(false, e.message ?: "An error occurred")
            }
        }
    }

    fun logout(onResult: (() -> Unit)? = null) {
        viewModelScope.launch {
            currentUser = null
            transactions.clear()
            upcomingEmis.clear()
            App.context?.let { SessionManager(it).clearToken() }
            bankBalance.value = 0.0
            todaysExpense.value = 0.0
            Log.d("MainViewModel", "User logged out")
            onResult?.invoke()
        }
    }


    fun getTotalIncomeThisMonth(): Double {
        val now = LocalDate.now()
        val (month, year) = now.month to now.year
        return transactions.asSequence()
            .mapNotNull { tx -> parseDateFlexible(tx.date)?.let { Pair(tx, it) } }
            .filter { (_, date) -> date.month == month && date.year == year }
            .map { (tx, _) -> if (tx.amount > 0) tx.amount else 0.0 }
            .sum()
    }

    fun getTotalExpenseThisMonth(): Double {
        val now = LocalDate.now()
        val (month, year) = now.month to now.year
        return transactions.asSequence()
            .mapNotNull { tx -> parseDateFlexible(tx.date)?.let { Pair(tx, it) } }
            .filter { (_, date) -> date.month == month && date.year == year }
            .map { (tx, _) -> if (tx.amount < 0) abs(tx.amount) else 0.0 }
            .sum()
    }

    fun getTodaysExpense(): Double {
        return todaysExpense.value
    }

    fun formatRupee(amount: Double): String = "₹${String.format("%,.0f", amount)}"

    fun fetchExpensesFromBackendAndPopulate() {
        viewModelScope.launch {
            try {
                App.context?.let { ctx ->
                    val token = SessionManager(ctx).getToken()
                    if (token.isNullOrEmpty()) {
                        Log.d("MainViewModel", "No token found – cannot fetch expenses")
                        return@launch
                    }

                    val resp = RetrofitClient.api.getExpenses("Bearer $token")
                    Log.d("MainViewModel", "getExpenses raw response: $resp")

                    val rawList = (resp["expenses"] ?: resp["data"] ?: resp)

                    val expenseList = when (rawList) {
                        is List<*> -> rawList.filterIsInstance<Map<String, Any>>()
                        else -> emptyList()
                    }

                    transactions.clear()

                    transactions.add(Transaction("1", "Initial Balance", 0.0, LocalDate.now().format(uiDateFormatter), Icons.Default.ShoppingCart, true))

                    expenseList.forEach { e ->
                        val title = e["receiver"]?.toString() ?: e["title"]?.toString() ?: "Expense"
                        val amt = (e["amount"] as? Number)?.toDouble() ?: (e["amount"] as? String)?.toDoubleOrNull() ?: 0.0
                        val date = e["date"]?.toString() ?: LocalDate.now().format(uiDateFormatter)

                        transactions.add(
                            Transaction(
                                id = (transactions.size + 1).toString(),
                                title = title,
                                amount = -kotlin.math.abs(amt),
                                date = date,
                                icon = Icons.Default.ShoppingCart,
                                isIncome = false
                            )
                        )
                    }

                    refreshTodaysExpense()
                    Log.d("MainViewModel", "Loaded ${transactions.size} transactions from backend")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching expenses from backend", e)
            }
        }
    }

    fun getRecentTransactions(limit: Int = 5): List<Transaction> {
        val snap = transactions.toList()

        return snap
            .map { tx ->
                val date = parseDateOrNull(tx.date)
                Pair(tx, date)
            }
            .sortedWith(
                compareByDescending<Pair<Transaction, LocalDate?>> { it.second ?: LocalDate.MIN }
                    .thenComparator { a, b ->
                        val ida = a.first.id
                        val idb = b.first.id
                        val na = ida.toLongOrNull()
                        val nb = idb.toLongOrNull()
                        when {
                            na != null && nb != null -> nb.compareTo(na) // descending numeric
                            else -> idb.compareTo(ida) // descending lexicographic
                        }
                    }
            )
            .map { it.first }
            .take(limit)
    }

    private fun <T> compareByDescending(
        selector: (T) -> Comparable<*>?
    ): Comparator<T> = Comparator { a, b ->
        val va = selector(a) as Comparable<Any>?
        val vb = selector(b) as Comparable<Any>?
        when {
            va == null && vb == null -> 0
            va == null -> 1
            vb == null -> -1
            else -> vb.compareTo(va)
        }
    }

    private fun parseDateOrNull(dateStr: String): LocalDate? {
        if (dateStr.isBlank()) return null
        try {
            return LocalDate.parse(dateStr, uiDateFormatter)
        } catch (ignored: Exception) { }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (ignored: Exception) { }
        try {
            val isoDateOnly = dateStr.split('T', ' ')[0]
            return LocalDate.parse(isoDateOnly, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (ignored: Exception) { }
        return null
    }
    private fun <T> Comparator<T>.thenComparator(other: (T, T) -> Int): Comparator<T> {
        val self = this
        return Comparator { a, b ->
            val r = self.compare(a, b)
            if (r != 0) r else other(a, b)
        }
    }

    fun computeCategoryExpenses(categoriesOrder: List<String>): List<CategoryExpense> {
        try {
            val totals = mutableMapOf<String, Double>()
            categoriesOrder.forEach { totals[it] = 0.0 }

            transactions.forEach { tx ->
                val cat = run {
                    val regex = Regex("""\[(.+?)\]""")
                    val m = regex.find(tx.title)
                    if (m != null) m.groupValues[1] else "Other"
                }

                val amt = tx.amount
                if (amt < 0) {
                    totals[cat] = (totals[cat] ?: 0.0) + kotlin.math.abs(amt)
                }
            }

            val totalAll = totals.values.sum().let { if (it.isNaN()) 0.0 else it }

            val colorList = com.example.expensetracker.ui.dashboard.AppColors.CategoryColors
            return totals.entries.mapIndexed { idx, (name, amt) ->
                val percent = if (totalAll <= 0.0) 0.0f else ((amt / totalAll) * 100.0).toFloat()
                com.example.expensetracker.ui.dashboard.CategoryExpense(
                    category = name,
                    amount = amt,
                    percentage = percent,
                    color = colorList[idx % colorList.size]
                )
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "computeCategoryExpenses error", e)
            return emptyList()
        }
    }



    fun addEmi(title: String, amount: Double, nextDueIso: String, periodMonths: Int? = null) {
        val id = (upcomingEmis.size + 1 + (System.currentTimeMillis() % 1000)).toString()
        // display-friendly date
        val displayDate = try {
            LocalDate.parse(nextDueIso).format(emiUiFormatter)
        } catch (e: Exception) {
            // fallback to today formatted
            LocalDate.now().format(emiUiFormatter)
        }

        val emi = EMIItem(
            id = id,
            title = title,
            amount = amount,
            date = displayDate,
            icon = androidx.compose.material.icons.Icons.Default.ThumbUp, // pick default or extend dialog to choose icon
            bank = "",
            account = "",
        )

        upcomingEmis.add(emi)
        Log.d("MainViewModel", "addEmi -> added: $emi")
    }

    val chatMessages = mutableStateListOf<ChatMessage>()
    val chatSending = mutableStateOf(false)


    fun sendChatMessageFromUi(text: String, onError: ((String)->Unit)? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val low = trimmed.lowercase()
        val greetingRegex = Regex("""\b(hi|hello|hey|yo|sup|gm|gn|good\s*(morning|evening|afternoon)|howdy|hiya|heyo)\b""")
        if (low.isNotBlank() && greetingRegex.containsMatchIn(low)) {
            chatMessages.add(ChatMessage(trimmed, isUser = true))

            val friendlyReplies = listOf(
                "Hey there! 👋 How are you doing today?",
                "Hi! 😊 Hope your day’s going well — want me to check your spending?",
                "Hello! 👋 Great to see you. Shall I summarize your recent expenses?",
                "Hey! 😄 Need help with budgeting or a quick spending overview?"
            )
            val reply = friendlyReplies.random()
            chatMessages.add(ChatMessage(reply, isUser = false))
            chatSending.value = false
            return
        }


        chatMessages.add(ChatMessage(trimmed, isUser = true))
        chatSending.value = true

        viewModelScope.launch {
            try {
                val token = App.context?.let { SessionManager(it).getToken() } ?: ""
                Log.d("ChatDebug", "sendChatMessageFromUi - token length: ${token.length} token (first40): ${if (token.length>40) token.substring(0,40) + "..." else token}")
                if (token.isEmpty()) {
                    chatMessages.add(ChatMessage("I couldn't find your login token. Please log in again.", isUser = false))
                    chatSending.value = false
                    onError?.invoke("No auth token")
                    return@launch
                }

                if (currentUser == null) {
                    try {
                        val meResp: com.example.expensetracker.ui.network.User = RetrofitClient.api.getCurrentUser("Bearer $token")
                        android.util.Log.d("ChatDebug", "meResp raw (User): $meResp")
                        currentUser = meResp
                        android.util.Log.d("ChatDebug", "Restored currentUser from server: id=${meResp.id}")

                    } catch (ex: Exception) {
                        android.util.Log.e("ChatDebug", "Exception while restoring user: ${ex.message}", ex)
                    }
                }
                android.util.Log.d("ChatDebug", "sendChatMessageFromUi - currentUser object: $currentUser")
                val currentUserId = currentUser?.id ?: ""
                android.util.Log.d("ChatDebug", "sendChatMessageFromUi - currentUserId: '$currentUserId' (length=${currentUserId.length})")
                if (currentUserId.isBlank()) {
                    chatMessages.add(ChatMessage("I couldn't identify your account. Please reopen the app or login again.", isUser = false))
                    chatSending.value = false
                    onError?.invoke("currentUser missing")
                    return@launch
                }

                val userIdToSend = currentUser?.id ?: ""
                if (userIdToSend.isBlank()) {
                    chatMessages.add(ChatMessage("Sorry — missing user id. Please log in again.", isUser = false))
                    onError?.invoke("Missing user id")
                    return@launch
                }

                val req = com.example.expensetracker.ui.network.ChatRequest(userId = userIdToSend, message = trimmed, window = 3)
                val resp = RetrofitClient.api.chat("Bearer $token", req)
                Log.d("ChatDebug", "chat resp.success=${resp.success}, reply=${resp.reply}")

                if (resp.success) {
                    val rawReply = resp.reply ?: "Sorry, no reply"
                    val step1 = rawReply.replace(Regex("""\$\s*([0-9][0-9,]*(?:\.[0-9]+)?)""")) { m ->
                        "₹" + m.groupValues[1]
                    }
                    val step2 = step1.replace(Regex("""\bRs\.?\s*([0-9][0-9,]*(?:\.[0-9]+)?)\b""", RegexOption.IGNORE_CASE)) { m ->
                        "₹" + m.groupValues[1]
                    }
                    val finalReply = step2.replace("$", "₹").replace(Regex("""\bRs\b""", RegexOption.IGNORE_CASE), "₹")

                    chatMessages.add(ChatMessage(finalReply, isUser = false))
                } else {
                    val raw = resp.reply ?: "Sorry, I couldn't process that."
                    val finalErr = raw.replace("$", "₹").replace(Regex("""\bRs\b""", RegexOption.IGNORE_CASE), "₹")
                    chatMessages.add(ChatMessage(finalErr, isUser = false))
                    onError?.invoke(resp.reply ?: "Chat failed")
                }

            } catch (e: Exception) {
                chatMessages.add(ChatMessage("Error: ${e.message}", isUser = false))
                onError?.invoke(e.message ?: "Unknown error")
            } finally {
                chatSending.value = false
            }
        }
    }

    fun addManualCredit(title: String, amount: Double, date: String = LocalDate.now().format(uiDateFormatter), onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val incoming = kotlin.math.abs(amount) // ensure positive
                bankBalance.value += incoming

                addLocalTransaction(title, incoming, date)

                App.context?.let { ctx ->
                    val token = SessionManager(ctx).getToken()
                    if (!token.isNullOrEmpty()) {
                        try {
                            RetrofitClient.api.addExpense(
                                "Bearer $token",
                                ExpenseRequest(
                                    amount = incoming,
                                    bank = "",
                                    account = "",
                                    receiver = title,
                                    date = LocalDate.now().toString()
                                )
                            )
                            Log.d("MainViewModel", "Manual credit sent to backend")
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error sending manual credit to backend", e)
                        }
                    } else {
                        Log.d("MainViewModel", "No token: manual credit stored locally only")
                    }
                }

                onResult?.invoke(true, "Credit added")
            } catch (e: Exception) {
                onResult?.invoke(false, e.message)
            }
        }
    }
}
