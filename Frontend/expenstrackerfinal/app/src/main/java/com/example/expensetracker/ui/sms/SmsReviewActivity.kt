package com.example.expensetracker.ui.sms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.expensetracker.App
import com.example.expensetracker.ui.network.ExpenseRequest
import com.example.expensetracker.ui.network.RetrofitClient
import com.example.expensetracker.ui.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.example.expensetracker.ui.sms.CategoryMemory


class SmsReviewActivity : ComponentActivity() {

    private val ALL_CATEGORIES = listOf(
        "Food", "Transport", "Shopping", "Bills", "Grocery",
        "Stationery", "Dairy", "Entertainment", "Medical", "Utilities", "Other"
    )


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val idempotency = intent.getStringExtra(NotificationHelper.EXTRA_IDEMPOTENCY) ?: ""
        val amount = intent.getDoubleExtra(NotificationHelper.EXTRA_AMOUNT, 0.0)
        val suggestedCategory = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: "Other"
        val suggestedReceiver = intent.getStringExtra(NotificationHelper.EXTRA_RECEIVER) ?: ""
        val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE) ?: ""
        val direction = intent.getStringExtra(NotificationHelper.EXTRA_DIRECTION) ?: "debit"

        setContent {
            var selectedCategory by remember { mutableStateOf(suggestedCategory) }
            var title by remember { mutableStateOf(if (suggestedReceiver.isNotBlank()) suggestedReceiver else suggestedCategory) }
            var saving by remember { mutableStateOf(false) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Review Transaction") }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Amount: ₹${String.format("%,.2f", amount)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Pick Category:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .fillMaxWidth()
                    ) {
                        items(ALL_CATEGORIES.size) { idx ->
                            val cat = ALL_CATEGORIES[idx]
                            ListItem(
                                headlineContent = { Text(cat) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = cat == selectedCategory,
                                        onClick = { selectedCategory = cat }
                                    ),
                                trailingContent = {
                                    if (cat == selectedCategory) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected"
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = {
                            if (!saving) {
                                saving = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val token = SessionManager(this@SmsReviewActivity).getToken()

                                        // ✅ SIGN FIX: negative for debit (expense), positive for credit (income)
                                        val signedForBackend =
                                            if (direction.equals("credit", true)) kotlin.math.abs(amount)
                                            else -kotlin.math.abs(amount)

                                        if (!token.isNullOrBlank()) {
                                            try {
                                                val req = ExpenseRequest(
                                                    amount = signedForBackend, // <-- send signed amount to backend
                                                    bank = "",
                                                    account = "",
                                                    receiver = title.ifBlank { suggestedReceiver.ifBlank { "Expense" } },
                                                    date = if (dateIso.isNotBlank()) dateIso else java.time.LocalDate.now().toString(),
                                                    category = selectedCategory
                                                )
                                                RetrofitClient.api.addExpense("Bearer $token", req)
                                            } catch (ex: Exception) {
                                                // ignore, we will still broadcast local change
                                                Log.e("SmsReview", "Backend addExpense failed: ${ex.message}", ex)
                                            }
                                        }

                                        // Remember the user’s chosen category for this merchant/title
                                        CategoryMemory.remember(
                                            this@SmsReviewActivity,
                                            title.ifBlank { suggestedReceiver.ifBlank { "Expense" } },
                                            selectedCategory
                                        )


                                        // Broadcast local change so UI updates (keep amount positive; direction carries semantics)
                                        val b = Intent(NotificationHelper.ACTION_EXPENSE_ADDED).apply {
                                            putExtra(NotificationHelper.EXTRA_IDEMPOTENCY, idempotency)
                                            putExtra(NotificationHelper.EXTRA_AMOUNT, kotlin.math.abs(amount))
                                            putExtra(
                                                NotificationHelper.EXTRA_RECEIVER,
                                                title.ifBlank { suggestedReceiver.ifBlank { "Expense" } }
                                            )
                                            putExtra(NotificationHelper.EXTRA_CATEGORY, selectedCategory)
                                            putExtra(NotificationHelper.EXTRA_DATE, dateIso)
                                            putExtra(NotificationHelper.EXTRA_DIRECTION, direction)
                                        }
                                        sendBroadcast(b)
                                    } finally {
                                        runOnUiThread { finish() }
                                    }
                                }
                            }
                        }) { Text("Save") }

                        OutlinedButton(onClick = { finish() }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

}
