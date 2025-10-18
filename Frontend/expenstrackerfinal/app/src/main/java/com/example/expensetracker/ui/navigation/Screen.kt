package com.example.expensetracker.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Home")
    object EMI : Screen("emi", "EMI")
    object AddTransaction : Screen("add_transaction", "Add")
    object AIAdvisor : Screen("ai_advisor", "Advisor")
    object Profile : Screen("profile", "Profile")
    object Budget : Screen("budget", "Budget")
    object Settings : Screen("settings", "Settings")
}