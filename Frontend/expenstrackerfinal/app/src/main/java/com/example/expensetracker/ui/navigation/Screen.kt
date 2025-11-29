package com.example.expensetracker.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Home")
    object EMI : Screen("emi", "EMI")
    object AddTransaction : Screen("add_transaction", "Add")
    object AIAdvisor : Screen("ai_advisor", "Advisor")
    object Tips : Screen("tips", "Tips")              // 👈 replaced Profile with Tips
    object Budget : Screen("budget", "Budget")
    object Settings : Screen("settings", "Settings")
}
