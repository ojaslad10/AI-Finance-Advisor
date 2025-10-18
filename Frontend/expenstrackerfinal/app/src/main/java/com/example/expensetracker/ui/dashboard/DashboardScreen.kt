package com.example.expensetracker.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.App.Companion.context
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.sms.NotificationHelper
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.time.LocalDate
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


object AppColors {
    val Primary = Color(0xFF6366F1)
    val Success = Color(0xFF22C55E)
    val Error = Color(0xFFEF4444)
    val Tertiary = Color(0xFFF59E0B)

    val CategoryColors = listOf(
        Color(0xFF8B5CF6),
        Color(0xFF06B6D4),
        Color(0xFFF97316),
        Color(0xFFEC4899),
        Color(0xFF84CC16)
    )
}

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val icon: ImageVector,
    val isIncome: Boolean = false
)

data class CategoryExpense(val category: String, val amount: Double, val percentage: Float, val color: Color)
data class MonthlyExpense(val month: String, val amount: Double)
data class UpcomingEMI(val title: String, val amount: Double, val dueDate: String, val icon: ImageVector)

val defaultCategoriesOrder = listOf(
    "Food",
    "Transport",
    "Shopping",
    "Bills",
    "Grocery",
    "Stationery",
    "Dairy",
    "Entertainment",
    "Medical",
    "Utilities",
    "Other"
)

fun defaultCategoryExpenses(): List<CategoryExpense> {
    return defaultCategoriesOrder.mapIndexed { i, name ->
        val color = AppColors.CategoryColors[i % AppColors.CategoryColors.size]
        CategoryExpense(name, amount = 0.0, percentage = 0f, color = color)
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dashboard by viewModel.dashboardState.collectAsState()

    val balance = dashboard?.bankBalance ?: viewModel.bankBalance.value
    val totalExpenseMonth = dashboard?.totalExpenseThisMonth ?: viewModel.getTotalExpenseThisMonth()
    val todaysExpense = dashboard?.todaysExpense ?: viewModel.getTodaysExpense()
    val recent = dashboard?.recentTransactions ?: viewModel.getRecentTransactions()
    val refreshing = remember { mutableStateOf(false) }


    Text(text = "Balance: ₹${String.format("%,.2f", balance)}")
    Text(text = "This month expense: ₹${String.format("%,.2f", totalExpenseMonth)}")
    Text(text = "Today's expense: ₹${String.format("%,.2f", todaysExpense)}")

    // Refresh once when screen loads
    LaunchedEffect(Unit) {
        viewModel.fetchDashboardData()
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(NotificationHelper.ACTION_EXPENSE_ADDED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    val idempotency = intent.getStringExtra(NotificationHelper.EXTRA_IDEMPOTENCY) ?: ""
                    val amount = intent.getDoubleExtra(NotificationHelper.EXTRA_AMOUNT, 0.0)
                    val receiverName = intent.getStringExtra(NotificationHelper.EXTRA_RECEIVER) ?: ""
                    val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: ""
                    val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE)
                        ?: java.time.LocalDate.now().toString()
                    val direction = intent.getStringExtra(NotificationHelper.EXTRA_DIRECTION) ?: "debit"

                    Log.d("DashboardRefresh", "Received ACTION_EXPENSE_ADDED — refreshing data")

                    viewModel.handleIncomingExpense(
                        idempotencyKey = idempotency,
                        receiverName = receiverName,
                        amount = amount,
                        date = dateIso,
                        category = category,
                        direction = direction
                    )

                    scope.launch {
                        try {
                            viewModel.fetchDashboardData()
                        } catch (e: Exception) {
                            Log.e("DashboardRefresh", "Error refreshing dashboard", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DashboardRefresh", "onReceive error", e)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }


    val categories by remember {
        derivedStateOf {
            try {
                viewModel.computeCategoryExpenses(defaultCategoriesOrder)
            } catch (e: Exception) {
                defaultCategoryExpenses()
            }
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = refreshing.value),
        onRefresh = {
            refreshing.value = true
            scope.launch {
                viewModel.fetchDashboardData()
                kotlinx.coroutines.delay(400)
                refreshing.value = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OverviewSection(viewModel) }
            item { CategorySection(viewModel = viewModel) }
            item { MonthlyChartSection() }
            item { TransactionsSection(viewModel) }
            item { EMISection(viewModel) }
        }
    }


}


@Composable
fun OverviewSection(viewModel: MainViewModel) {
    val bankBalance by remember { derivedStateOf { viewModel.bankBalance.value } }
    val totalExpense by remember { derivedStateOf { viewModel.getTotalExpenseThisMonth() } }
    val todaysExpense by remember { derivedStateOf { viewModel.getTodaysExpense() } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Financial Overview",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewCard(
                title = "Bank Balance",
                amount = viewModel.formatRupee(bankBalance),
                subtitle = "Current",
                icon = Icons.Default.ThumbUp,
                color = AppColors.Success,
                modifier = Modifier.weight(1f)
            )

            OverviewCard(
                title = "Total Expense",
                amount = viewModel.formatRupee(totalExpense),
                subtitle = "This month",
                icon = Icons.Default.ThumbUp,
                color = AppColors.Error,
                modifier = Modifier.weight(1f)
            )
        }

        OverviewCard(
            title = "Today's Expense",
            amount = viewModel.formatRupee(todaysExpense),
            subtitle = "Today",
            icon = Icons.Default.ThumbUp,
            color = AppColors.Primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}

@Composable
fun OverviewCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, color = Color.Gray)
                Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}



// <- make sure this import exists

@Composable
fun CategorySection(viewModel: MainViewModel) {
    // Observe the StateFlow from the ViewModel (provide an initial empty list to be safe)
    val categories by viewModel.categoryExpenses.collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Category Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

            val totalAmount = categories.sumOf { it.amount }
            if (totalAmount <= 0.0) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No category data yet", color = Color.Gray)
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        PieChartLarge(categories)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = categories) { c ->
                            CategoryItem(c)
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun PieChartLarge(categories: List<CategoryExpense>) {
    val percentages = categories.map { it.percentage.coerceAtLeast(0f) }
    val totalPercent = percentages.sum().coerceAtLeast(1f)
    val normalized = percentages.map { it / totalPercent * 100f }
    val animated = normalized.map { animateFloatAsState(targetValue = it, animationSpec = tween(800), label = "") }

    Canvas(modifier = Modifier.size(160.dp)) {
        val radius = size.minDimension / 2 * 0.85f
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f

        categories.forEachIndexed { index, cat ->
            val sweep = (animated[index].value / 100f) * 360f
            drawArc(
                color = cat.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            startAngle += sweep
        }

        // donut hole
        drawCircle(
            color = Color(0xFF121212),
            radius = radius * 0.55f,
            center = center
        )
    }
}

@Composable
fun CategoryItem(category: CategoryExpense) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(category.color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(category.category, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text("₹${String.format("%,.0f", category.amount)}", fontSize = 12.sp, color = Color.Gray)
        }
        Text("${category.percentage.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = category.color)
    }
}

@Composable
fun MonthlyChartSection() {
    val monthlyData = listOf(
        MonthlyExpense("Apr", 38000.0),
        MonthlyExpense("May", 42000.0),
        MonthlyExpense("Jun", 39500.0),
        MonthlyExpense("Jul", 46000.0),
        MonthlyExpense("Aug", 41200.0)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Last 5 Months Expense", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            BarChart(monthlyData)
        }
    }
}

@Composable
fun BarChart(data: List<MonthlyExpense>) {
    val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { monthData ->
            val animatedHeight by animateFloatAsState(
                targetValue = (monthData.amount / maxAmount).toFloat(),
                animationSpec = tween(1000),
                label = ""
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "₹${String.format("%.0fK", monthData.amount / 1000)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height((140.dp * animatedHeight))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFF3B82F6))
                            )
                        )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(monthData.month, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TransactionsSection(viewModel: MainViewModel) {
    val recent = remember { derivedStateOf { viewModel.getRecentTransactions(limit = 5) } }.value

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                TextButton(onClick = {}) { Text("View All", fontSize = 12.sp, color = AppColors.Primary) }
            }

            if (recent.isEmpty()) {
                Text("No recent transactions", color = Color.Gray, fontSize = 14.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { tx -> TransactionItem(tx) }
                }
            }
        }
    }
}

@Composable
fun EMISection(viewModel: com.example.expensetracker.ui.MainViewModel) {
    val upcoming by remember { derivedStateOf { viewModel.upcomingEmis.toList() } }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Upcoming EMIs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(Icons.Default.Home, null, tint = AppColors.Tertiary) // optional header icon
            }

            if (upcoming.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No upcoming EMIs", color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to add one.", color = Color.Gray)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(upcoming) { emi ->
                        EMICard(emi = emi, onPaidClick = { viewModel.payEmi(emi) })
                    }
                }
            }
        }
    }
}


@Composable
fun EMICard(
    emi: com.example.expensetracker.ui.emi.EMIItem,
    onPaidClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(12.dp))
            .padding(1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                emi.icon,
                contentDescription = emi.title,
                tint = AppColors.Tertiary,
                modifier = Modifier.size(22.dp)
            )

            Text(
                emi.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )

            Text(
                "₹${String.format("%,.0f", emi.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Tertiary
            )

            Text(
                "Due: ${emi.date}",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPaidClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("Mark Paid", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}