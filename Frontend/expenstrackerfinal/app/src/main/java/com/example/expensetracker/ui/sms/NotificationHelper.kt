// file: app/src/main/java/com/example/expensetracker/ui/sms/NotificationHelper.kt
package com.example.expensetracker.ui.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

object NotificationHelper {
    const val CHANNEL_ID = "sms_expense_channel"
    const val ACTION_CONFIRM = "com.example.expensetracker.ACTION_CONFIRM_SMS"
    const val ACTION_IGNORE = "com.example.expensetracker.ACTION_IGNORE_SMS"
    const val KEY_REMOTE_INPUT = "key_remote_input_title"
    const val EXTRA_IDEMPOTENCY = "extra_idempotency"
    const val EXTRA_AMOUNT = "extra_amount"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_SENDER = "extra_sender"
    const val EXTRA_RECEIVER = "extra_receiver"
    const val EXTRA_DATE = "extra_date"
    const val EXTRA_DIRECTION = "extra_direction"
    const val NOTIF_TAG = "sms_expense"
    const val ACTION_EXPENSE_ADDED = "com.example.expensetracker.ACTION_EXPENSE_ADDED"


    private const val TAG = "NotificationHelper"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Expense Actions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for parsed SMS transactions - choose category / title"
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun buildAndShowNotification(
        context: Context,
        notificationId: Int,
        idempotencyKey: String,
        amount: Double,
        suggestedCategory: String,
        suggestedReceiver: String,
        sender: String,
        isoDate: String,
        direction: String = "debit"
    ): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                if (perm != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS not granted — skipping notification (idempotency=$idempotencyKey)")
                    return false
                }
            }

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Notifications disabled — skipping notification (idempotency=$idempotencyKey)")
                return false
            }

            ensureChannel(context)

            val remoteInput = RemoteInput.Builder(KEY_REMOTE_INPUT)
                .setLabel("Edit title (optional)")
                .build()

            val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_CONFIRM
                putExtra(EXTRA_IDEMPOTENCY, idempotencyKey)
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_CATEGORY, suggestedCategory)
                putExtra(EXTRA_RECEIVER, suggestedReceiver)
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_DATE, isoDate)
                putExtra(EXTRA_DIRECTION, direction)
            }
            val confirmFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE

            val confirmPending = PendingIntent.getBroadcast(
                context,
                (idempotencyKey + "_confirm").hashCode(),
                confirmIntent,
                confirmFlags
            )
            val confirmAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Confirm (edit title)",
                confirmPending
            ).addRemoteInput(remoteInput).build()

            val ignoreIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_IGNORE
                putExtra(EXTRA_IDEMPOTENCY, idempotencyKey)
            }
            val ignorePending = PendingIntent.getBroadcast(
                context,
                (idempotencyKey + "_ignore").hashCode(),
                ignoreIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            val reviewIntent = Intent(context, SmsReviewActivity::class.java).apply {
                putExtra(EXTRA_IDEMPOTENCY, idempotencyKey)
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_CATEGORY, suggestedCategory)
                putExtra(EXTRA_RECEIVER, suggestedReceiver)
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_DATE, isoDate)
                putExtra(EXTRA_DIRECTION, direction)
            }
            val contentPending = PendingIntent.getActivity(
                context,
                (idempotencyKey + "_open").hashCode(),
                reviewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Detected ₹${String.format("%,.2f", amount)} — $suggestedCategory")
                .setContentText("Tap a category below (or open to edit title).")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPending)
                .setAutoCancel(true)
                .addAction(confirmAction)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignore", ignorePending)

            val allQuick = listOf(suggestedCategory, "Food", "Transport", "Shopping", "Bills", "Grocery", "Other")
                .distinct().take(3)
            allQuick.forEach { cat ->
                val catIntent = Intent(context, SmsReviewActivity::class.java).apply {
                    putExtra(EXTRA_IDEMPOTENCY, idempotencyKey)
                    putExtra(EXTRA_AMOUNT, amount)
                    putExtra(EXTRA_CATEGORY, cat) // preselect
                    putExtra(EXTRA_RECEIVER, suggestedReceiver)
                    putExtra(EXTRA_SENDER, sender)
                    putExtra(EXTRA_DATE, isoDate)
                    putExtra(EXTRA_DIRECTION, direction)
                    putExtra("via_quick_cat", true)
                }
                val catPending = PendingIntent.getActivity(
                    context,
                    (idempotencyKey + "_cat_$cat").hashCode(),
                    catIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
                builder.addAction(android.R.drawable.ic_menu_agenda, cat, catPending)
            }

            NotificationManagerCompat.from(context).notify(NOTIF_TAG, notificationId, builder.build())
            Log.d(TAG, "Notification shown for id=$notificationId direction=$direction (idempotency=$idempotencyKey)")
            return true
        } catch (ex: SecurityException) {
            Log.e(TAG, "SecurityException while notifying: ${ex.message}")
            return false
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to build/show notification: ${ex.message}")
            return false
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIF_TAG, notificationId)
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to cancel notification: ${ex.message}")
        }
    }
}
