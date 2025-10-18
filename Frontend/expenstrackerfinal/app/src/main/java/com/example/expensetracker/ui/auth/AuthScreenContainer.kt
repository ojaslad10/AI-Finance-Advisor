package com.example.expensetracker.ui.auth

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.navigation.Screen

@Composable
fun AuthScreenContainer(
    navController: NavController,
    viewModel: MainViewModel = viewModel(),
    isLogin: Boolean
) {
    var isLogin by remember { mutableStateOf(isLogin) }
    val context = LocalContext.current

    // Input states
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AuthScreen(
        isLogin = isLogin,
        name = name,
        phone = phone,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        onNameChange = { name = it },
        onPhoneChange = { phone = it },
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onConfirmPasswordChange = { confirmPassword = it },
        onSwitchMode = {
            isLogin = !isLogin
            // clear form when switching
            name = ""
            phone = ""
            email = ""
            password = ""
            confirmPassword = ""
            errorMessage = ""
        },
        onSubmit = {
            if (isLogin) {
                viewModel.login(email, password) { success, msg ->
                    if (success) {
                        navController.navigate(Screen.Dashboard.route) {
                            // Clear back stack to prevent returning to auth screens
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, msg ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                if (name.isBlank() || phone.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.signup(name, email, phone, password) { success, msg ->
                        if (success) {
                            // After signup, navigate to login (clear auth backstack)
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                            Toast.makeText(context, "Signup successful! Please login.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, msg ?: "Signup failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        },
        errorMessage = errorMessage
    )
}
