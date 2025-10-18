package com.example.expensetracker.ui.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.expensetracker.ui.session.SessionManager

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive called - action=${intent?.action}, contextNull=${context == null}")
        if (context == null || intent == null) return

        if (intent.action == "com.example.expensetracker.TEST_SMS") {
            val msg = intent.getStringExtra("msg") ?: ""
            val sender = intent.getStringExtra("sender") ?: "TEST-SENDER"
            Log.d(TAG, "TEST_SMS simulated: from=$sender body=$msg")

            val parsed = SmsParser.parse(msg, sender, null)
            if (parsed != null) {
                val idempotencyKey = parsed.idempotencyKey
                val notificationId = idempotencyKey.hashCode()

                val shown = NotificationHelper.buildAndShowNotification(
                    context.applicationContext,
                    notificationId,
                    idempotencyKey,
                    parsed.amount,
                    parsed.categorySuggestion ?: "Other",
                    parsed.receiver ?: "Unknown",
                    sender,
                    parsed.date,
                    parsed.direction ?: "debit"
                )
                if (!shown) Log.w(TAG, "Test notification not shown (permission/disabled)")
            } else {
                Log.d(TAG, "TEST_SMS parse failed")
            }
            return
        }

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isEmpty()) {
            Log.d(TAG, "No SMS parts found")
            return
        }

        val fullBody = parts.joinToString(separator = "") { it.messageBody ?: "" }
        val sender = parts.firstOrNull()?.originatingAddress ?: "unknown"
        Log.d(TAG, "SMS Received from $sender: $fullBody")

        val parsed = SmsParser.parse(fullBody, sender, null)
        if (parsed == null) {
            Log.d(TAG, "SmsParser failed to parse message")
            return
        }

        Log.d(
            TAG,
            "Parsed SMS -> amount=${parsed.amount}, bank=${parsed.bank}, account=${parsed.account}, receiver=${parsed.receiver}, date=${parsed.date}, direction=${parsed.direction}, idempotency=${parsed.idempotencyKey}, confidence=${parsed.confidence}"
        )

        val allowedDirections = setOf("debit", "credit")
        if (!allowedDirections.contains(parsed.direction.lowercase()) || parsed.amount <= 0.0) {
            Log.d(TAG, "Not a transaction SMS, skipping notification.")
            return
        }

        val idempotencyKey = parsed.idempotencyKey
        val notificationId = idempotencyKey.hashCode()

        val shown = NotificationHelper.buildAndShowNotification(
            context.applicationContext,
            notificationId,
            idempotencyKey,
            parsed.amount,
            parsed.categorySuggestion ?: "Other",
            parsed.receiver ?: "Unknown",
            sender,
            parsed.date,
            parsed.direction
        )

        if (!shown) {
            Log.w(TAG, "Notification not shown (permission/disabled)")
        } else {
            Log.d(TAG, "Notification shown (idempotency=$idempotencyKey)")
        }
    }
}
