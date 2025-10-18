package com.example.expensetracker.ui.emi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.dashboard.AppColors
import java.time.LocalDate


data class EMIItem(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val icon: ImageVector,
    val bank: String = "",
    val account: String = "",
    val nextDueIso: String = LocalDate.now().toString(),
    val periodMonths: Int? = null
)

@Composable
fun EmiScreen(viewModel: MainViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshEmisForToday() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming EMIs",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add EMI", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Add EMI", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        val emis = viewModel.upcomingEmis.toList()
        if (emis.isEmpty()) {
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(emis) { emi ->
                    EmiCard(emi = emi, onPaidClick = { viewModel.payEmi(emi) })
                }
            }
        }
    }

    // Add EMI dialog
    if (showAddDialog) {
        AddEmiDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title: String, amount: Double, dueIso: String, periodMonths: Int? ->
                viewModel.addEmi(
                    title = title,
                    amount = amount,
                    nextDueIso = dueIso,
                    periodMonths = periodMonths
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EmiCard(emi: EMIItem, onPaidClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        emi.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text("Due: ${emi.date}", fontSize = 12.sp, color = Color.Gray)
                }
                Text(
                    "₹${String.format("%,.0f", emi.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Tertiary
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onPaidClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Text("Mark as Paid", color = Color.White)
            }
        }
    }
}

@Composable
fun AddEmiDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double, dueIso: String, periodMonths: Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueIso by remember { mutableStateOf(LocalDate.now().plusMonths(1).toString()) }
    var periodText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add EMI", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = Color.Gray) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d*\$"))) amountText = input
                    },
                    label = { Text("Amount", color = Color.Gray) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dueIso,
                    onValueChange = { dueIso = it },
                    label = { Text("Due Date (yyyy-MM-dd)", color = Color.Gray) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = periodText,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\$"))) periodText = input
                    },
                    label = { Text("Repeat every N months (optional)", color = Color.Gray) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                val months = periodText.toIntOrNull()
                val dateIso = try {
                    LocalDate.parse(dueIso).toString()
                } catch (e: Exception) {
                    LocalDate.now().plusMonths(1).toString()
                }
                if (title.isNotBlank() && amt > 0) {
                    onAdd(title, amt, dateIso, months)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) {
                Text("Add", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
