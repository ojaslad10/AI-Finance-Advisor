package com.example.expensetracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.MainViewModel
import kotlin.math.abs
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RecentTransactionsSection(
    viewModel: MainViewModel,
    limit: Int = 5,
    onViewAll: () -> Unit = {}
) {
    val items = viewModel.getRecentTransactions(limit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", fontSize = 18.sp, color = Color.White)
                TextButton(onClick = onViewAll) {
                    Text("View All", fontSize = 12.sp, color = Color(0xFF6366F1))
                }
            }

            if (items.isEmpty()) {
                Text("No recent transactions", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { tx ->
                        TransactionItem(tx)
                    }
                }
            }
        }
    }
}


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
