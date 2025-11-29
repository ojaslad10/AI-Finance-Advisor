package com.example.expensetracker.ui.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.expensetracker.ui.network.ExpenseRequest
import com.example.expensetracker.ui.network.RetrofitClient
import com.example.expensetracker.ui.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionRecv"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "context or intent null - ignoring")
            return
        }

        val action = intent.action ?: ""
        val idempotency = intent.getStringExtra(NotificationHelper.EXTRA_IDEMPOTENCY) ?: ""
        Log.d(TAG, "onReceive action=$action idempotency=$idempotency")

        when (action) {
            NotificationHelper.ACTION_IGNORE -> {
                Log.d(TAG, "User ignored parsed SMS (idempotency=$idempotency)")
                NotificationHelper.cancelNotification(context, idempotency.hashCode())
            }

            NotificationHelper.ACTION_CONFIRM -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val inlineTitle = remoteInput?.getCharSequence(NotificationHelper.KEY_REMOTE_INPUT)?.toString()?.trim()

                val amountRaw = intent.getDoubleExtra(NotificationHelper.EXTRA_AMOUNT, 0.0)
                val categoryExtra = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: ""
                val receiverExtra = intent.getStringExtra(NotificationHelper.EXTRA_RECEIVER) ?: ""
                val senderExtra = intent.getStringExtra(NotificationHelper.EXTRA_SENDER) ?: ""
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE) ?: LocalDate.now().toString()
                val direction = intent.getStringExtra(NotificationHelper.EXTRA_DIRECTION) ?: "debit"

// 1) Final title (what user sees)
                val finalTitle = when {
                    !inlineTitle.isNullOrBlank() -> inlineTitle
                    receiverExtra.isNotBlank() -> receiverExtra
                    categoryExtra.isNotBlank() -> categoryExtra
                    else -> if (direction.equals("credit", ignoreCase = true)) "Income" else "Expense"
                }

// 2) Pick category with precedence: history > guess from edited title > suggested > Other
                val history = CategoryMemory.getCategoryFor(context, finalTitle)
                val guessedFromTitle = SmsParser.guessCategoryFromText(finalTitle, receiverExtra)
                val finalCategory = when {
                    !history.isNullOrBlank() -> history!!
                    !guessedFromTitle.isNullOrBlank() && guessedFromTitle != "Other" -> guessedFromTitle
                    categoryExtra.isNotBlank() -> categoryExtra
                    else -> "Other"
                }

// 3) Compute signed amount (your previous fix)
                val finalAmount = when (direction.lowercase()) {
                    "credit" -> -kotlin.math.abs(amountRaw)   // your backend treats positive as income; you already reconciled this path
                    else -> kotlin.math.abs(amountRaw)
                }

// 4) Remember the user’s *implicit* choice (title→category) so next time it’s sticky
                CategoryMemory.remember(context, finalTitle, finalCategory)

// ... proceed with your existing network call & local broadcast using finalTitle/finalCategory/finalAmount ...


                Log.d(
                    TAG,
                    "Confirm clicked. title='$finalTitle' amountRaw=$amountRaw finalAmount=$finalAmount direction=$direction category=$finalCategory date=$dateIso"
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val token = SessionManager(context).getToken()
                        if (token.isNullOrBlank()) {
                            Log.w(TAG, "No auth token available — broadcasting locally and queuing for sync later.")
                            sendExpenseAddedBroadcast(
                                context = context,
                                idempotency = idempotency,
                                amount = amountRaw, // keep raw for UI broadcast; ViewModel handles direction
                                receiver = finalTitle,
                                category = finalCategory,
                                dateIso = dateIso,
                                direction = direction
                            )
                        } else {
                            try {
                                val req = com.example.expensetracker.ui.network.ExpenseRequest(
                                    amount = finalAmount,      // ✅ backend receives signed amount
                                    bank = "",
                                    account = "",
                                    receiver = finalTitle,
                                    category = finalCategory,
                                    date = dateIso,
                                    idempotencyKey = idempotency
                                )

                                Log.d(TAG, "Posting confirmed expense to backend: $req")
                                val resp = com.example.expensetracker.ui.network.RetrofitClient.api
                                    .addExpense("Bearer $token", req)
                                Log.d(TAG, "Backend addExpense response: success=${resp.success}, message=${resp.message}")

                                sendExpenseAddedBroadcast(
                                    context = context,
                                    idempotency = idempotency,
                                    amount = amountRaw, // positive for UI path
                                    receiver = finalTitle,
                                    category = finalCategory,
                                    dateIso = dateIso,
                                    direction = direction
                                )
                            } catch (ex: Exception) {
                                Log.e(TAG, "Failed to call backend addExpense: ${ex.message}", ex)
                                sendExpenseAddedBroadcast(
                                    context = context,
                                    idempotency = idempotency,
                                    amount = amountRaw,
                                    receiver = finalTitle,
                                    category = finalCategory,
                                    dateIso = dateIso,
                                    direction = direction
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in notification confirm handling: ${e.message}", e)
                    } finally {
                        NotificationHelper.cancelNotification(context, idempotency.hashCode())
                    }
                }
            }

            else -> {
                Log.w(TAG, "Unknown action received: $action")
            }
        }
    }


    private fun sendExpenseAddedBroadcast(
        context: Context,
        idempotency: String?,
        amount: Double,
        receiver: String,
        category: String,
        dateIso: String,
        direction: String
    ) {
        val b = Intent(NotificationHelper.ACTION_EXPENSE_ADDED).apply {
            putExtra(NotificationHelper.EXTRA_IDEMPOTENCY, idempotency)
            putExtra(NotificationHelper.EXTRA_AMOUNT, amount)
            putExtra(NotificationHelper.EXTRA_RECEIVER, receiver)
            putExtra(NotificationHelper.EXTRA_CATEGORY, category)
            putExtra(NotificationHelper.EXTRA_DATE, dateIso)
            putExtra(NotificationHelper.EXTRA_DIRECTION, direction)
        }
        context.sendBroadcast(b)
    }

}
