package com.example.expensetracker.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.MainViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: MainViewModel) {
    var showSetBalanceSheet by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showAddCreditSheet by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Manage Transactions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Show current bank balance
        Text(
            text = "Bank Balance: ${viewModel.formatRupee(viewModel.bankBalance.value)}",
            fontSize = 18.sp,
            color = Color.White
        )

        // Set Bank Balance button
        Button(
            onClick = { showSetBalanceSheet = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ThumbUp, contentDescription = "Set Balance", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Set Bank Balance", color = Color.White, fontWeight = FontWeight.Bold)
        }

        // Add Expense button
        Button(
            onClick = { showAddExpenseSheet = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Add Expense", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add Expense", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.setBankBalance(viewModel.bankBalance.value + 1000.0)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Put Cash", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Put Cash +₹1,000", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { showAddCreditSheet = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Credit", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add Credit Amount", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    if (showSetBalanceSheet) {
        SetBankBalanceDialog(
            initial = viewModel.bankBalance.value,
            onDismiss = { showSetBalanceSheet = false },
            onSet = { amount ->
                viewModel.setBankBalance(amount)
                showSetBalanceSheet = false
            }
        )
    }

    if (showAddExpenseSheet) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseSheet = false },
            onAdd = { title, amount, category ->
                val combinedTitle = if (category.isBlank()) title else "[$category] $title"
                viewModel.addManualExpense(combinedTitle, amount) { success, msg ->
                }
                showAddExpenseSheet = false
            }
        )
    }
    if (showAddCreditSheet) {
        AddCreditDialog(
            onDismiss = { showAddCreditSheet = false },
            onAdd = { title, amount, note ->
                val combinedTitle = if (note.isBlank()) title else "$title — $note"
                viewModel.addManualCredit(combinedTitle, amount) { success, msg ->
                }
                showAddCreditSheet = false
            }
        )
    }
}

@Composable
fun SetBankBalanceDialog(initial: Double, onDismiss: () -> Unit, onSet: (Double) -> Unit) {
    var value by remember { mutableStateOf(if (initial == 0.0) "" else initial.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Bank Balance", color = Color.White) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Amount", color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    cursorColor = Color.White
                )
            )
        },
        confirmButton = {
            Button(onClick = {
                val amt = value.toDoubleOrNull() ?: 0.0
                onSet(amt)
            }) {
                Text("Set")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onAdd: (String, Double, String) -> Unit) {
    val categories = listOf(
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
        "Other (type manually)"
    )

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var manualCategory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g., Grocery run)", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        cursorColor = Color.White
                    )
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedCategory,
                        onValueChange = { },
                        label = { Text("Category", color = Color.Gray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            cursorColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category, color = Color.White) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedCategory == "Other (type manually)") {
                    OutlinedTextField(
                        value = manualCategory,
                        onValueChange = { manualCategory = it },
                        label = { Text("Enter category", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E),
                            cursorColor = Color.White
                        )
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            amount = input
                        }
                    },
                    label = { Text("Amount", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        cursorColor = Color.White
                    )
                )

                // Quick chips row (horizontal scroll)
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Food", "Grocery", "Transport", "Stationery").forEach { quick ->
                        AssistChip(
                            onClick = {
                                selectedCategory = quick
                                manualCategory = ""
                            },
                            label = { Text(quick) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF2C2C2C),
                                labelColor = Color.White
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val categoryFinal = if (selectedCategory == "Other (type manually)") {
                    manualCategory.ifBlank { "Other" }
                } else selectedCategory

                val combinedTitle = if (title.isBlank()) {
                    categoryFinal
                } else {
                    "[$categoryFinal] $title"
                }

                val amt = amount.toDoubleOrNull() ?: 0.0
                onAdd(combinedTitle, amt, categoryFinal)
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) {
                Text("Add", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCreditDialog(onDismiss: () -> Unit, onAdd: (String, Double, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Credit", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g., Salary, Repaid loan)", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        cursorColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            amount = input
                        }
                    },
                    label = { Text("Amount", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        cursorColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional note", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        cursorColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (title.isBlank()) {
                    // fallback title if empty
                    onAdd("Credit", amt, note)
                } else {
                    onAdd(title, amt, note)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))) {
                Text("Add Credit", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
