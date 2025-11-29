package com.example.expensetracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


fun friendlyDateLabel(dateStr: String): String {
    val parsed = runCatching {
        LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH))
    }.getOrNull() ?: runCatching {
        LocalDate.parse(dateStr)
    }.getOrNull()

    parsed ?: return dateStr

    val today = LocalDate.now()
    return when (parsed) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> parsed.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH))
    }
}


@Composable
fun TransactionItem(transaction: Transaction) {
    val cleanedTitle = transaction.title.replace(Regex("""^\s*(\[[^\]]+\]\s*)+"""), "").trim()

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(
                    if (transaction.isIncome) AppColors.Success.copy(alpha = 0.1f)
                    else AppColors.Error.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                transaction.icon,
                contentDescription = cleanedTitle,
                tint = if (transaction.isIncome) AppColors.Success else AppColors.Error,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(Modifier.weight(1f)) {
            Text(cleanedTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(friendlyDateLabel(transaction.date), fontSize = 12.sp, color = Color.Gray)
        }

        Text(
            text = "${if (transaction.isIncome) "+" else ""}₹${String.format("%,.0f", abs(transaction.amount))}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (transaction.isIncome) AppColors.Success else AppColors.Error
        )
    }
}
