package com.example.expensetracker.ui.tips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.dashboard.AppColors
import kotlin.math.roundToInt

@Composable
fun TipsScreen(viewModel: MainViewModel) {
    val bankBalance by remember { derivedStateOf { viewModel.bankBalance.value } }
    val monthSpend by remember { derivedStateOf { viewModel.getTotalExpenseThisMonth() } }
    val todaySpend by remember { derivedStateOf { viewModel.getTodaysExpense() } }

    // ---- fake data for now (replace with backend later) ----
    val categorySpend = remember {
        mutableStateMapOf(
            "Food" to 12500.0,
            "Transport" to 4200.0,
            "Shopping" to 9800.0,
            "Bills" to 13450.0,
            "Grocery" to 6100.0,
            "Other" to 1800.0
        )
    }
    val totalBills = remember { 13450.0 }
    val topCategory = categorySpend.maxByOrNull { it.value }?.key ?: "Food"
    val topCategoryAmt = categorySpend[topCategory] ?: 0.0
    val topPct = if (monthSpend > 0) (topCategoryAmt / monthSpend * 100).roundToInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tips & Recommendations", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Smart nudges based on your spending", color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.Build, contentDescription = null, tint = AppColors.Primary)
        }

        // quick metrics row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetricCard(
                title = "This Month",
                value = "₹${fmt(monthSpend)}",
                subtitle = "Total spent",
                tint = AppColors.Error,
                icon = Icons.Default.ThumbUp,
                modifier = Modifier.weight(1f)
            )
            SmallMetricCard(
                title = "Today",
                value = "₹${fmt(todaySpend)}",
                subtitle = "Daily spend",
                tint = AppColors.Primary,
                icon = Icons.Default.Build,
                modifier = Modifier.weight(1f)
            )
        }

        // top insight
        LargeHighlightCard(
            title = "Overspending in $topCategory",
            value = "₹${fmt(topCategoryAmt)} (~$topPct% of month)",
            suggestion = "Set a weekly cap and switch to lower-cost options; freeze non-essentials for 7 days.",
            tint = AppColors.Error
        )

        // category watchlist
        SectionTitle("Category Watchlist", Icons.Default.Warning)
        TipsCardList(
            items = categorySpend.entries
                .sortedByDescending { it.value }
                .map { (name, amt) ->
                    val pct = if (monthSpend > 0) (amt / monthSpend * 100).roundToInt() else 0
                    TipItem(
                        id = name,
                        title = "$name — ₹${fmt(amt)}",
                        detail = "≈ $pct% this month. Consider a weekly budget and swap to cheaper alternatives.",
                        tone = when {
                            pct >= 35 -> TipTone.Warning
                            pct >= 20 -> TipTone.Caution
                            else -> TipTone.Neutral
                        }
                    )
                }
        )

        // bills snapshot
        SectionTitle("Subscriptions & Utilities", Icons.Default.FavoriteBorder)
        SmallMetricCard(
            title = "Monthly Bills",
            value = "₹${fmt(totalBills)}",
            subtitle = "Auto-debits & utilities",
            tint = AppColors.Tertiary,
            icon = Icons.Default.FavoriteBorder,
            modifier = Modifier.fillMaxWidth()
        )
        TipsCardList(
            items = listOf(
                TipItem("b1", "Audit subs", "Cancel unused trials and downgrade plans — typical savings 10–18%.", TipTone.Caution),
                TipItem("b2", "Align due dates", "Cluster due dates after payday to avoid late fees.", TipTone.Positive),
                TipItem("b3", "Negotiate utilities", "Ask for retention discounts on broadband/cable.", TipTone.Positive)
            )
        )

        // simple goals
        SectionTitle("Simple Saving Goals", Icons.Default.CheckCircle)
        TipsCardList(
            items = listOf(
                TipItem("g1", "Autosave 5%", "Move ₹${fmt(bankBalance * 0.05)} to savings every payday.", TipTone.Positive),
                TipItem("g2", "Cut 10% spend", "Target saving ₹${fmt(monthSpend * 0.1)} next month via caps on top 2 categories.", TipTone.Caution),
                TipItem("g3", "Build buffer", "Aim for ₹${fmt(monthSpend * 3)} as a 3-month emergency fund.", TipTone.Neutral)
            )
        )

        // actionable nudges (short list)
        SectionTitle("Actionable Nudges", Icons.Default.Build)
        TipsCardList(
            items = listOf(
                TipItem("n1", "Daily check", "You spent ₹${fmt(todaySpend)} today — try a no-spend day tomorrow.", TipTone.Neutral),
                TipItem("n2", "Lock progress", "Balance ₹${fmt(bankBalance)} — move 5–10% to a separate savings account.", TipTone.Positive)
            )
        )

        Spacer(Modifier.height(4.dp))
    }
}

/* ---------- small building blocks ---------- */

private data class TipItem(
    val id: String,
    val title: String,
    val detail: String,
    val tone: TipTone = TipTone.Neutral
)

private enum class TipTone { Positive, Caution, Warning, Neutral }

@Composable
private fun SectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(icon, null, tint = AppColors.Primary)
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallMetricCard(
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(108.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = tint)
                Text(title, color = Color.Gray, fontSize = 12.sp)
            }
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LargeHighlightCard(
    title: String,
    value: String,
    suggestion: String,
    tint: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ThumbUp, null, tint = tint)
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(value, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(suggestion, color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TipsCardList(items: List<TipItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(items, key = { it.id }) { tip ->
                    TipRow(tip)
                    Divider(color = Color(0xFF2A2A2A))
                }
            }
        }
    }
}

@Composable
private fun TipRow(tip: TipItem) {
    val (icon, tint) = when (tip.tone) {
        TipTone.Positive -> Icons.Default.CheckCircle to AppColors.Success
        TipTone.Caution -> Icons.Default.FavoriteBorder to AppColors.Tertiary
        TipTone.Warning -> Icons.Default.ThumbUp to AppColors.Error
        TipTone.Neutral -> Icons.Default.Build to AppColors.Primary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(tip.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(tip.detail, color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

private fun fmt(v: Double): String = String.format("%,.0f", v)
