package com.example.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.add.AddTransactionScreen
import com.example.expensetracker.ui.dashboard.DashboardScreen
import com.example.expensetracker.ui.emi.EmiScreen
import com.example.expensetracker.ui.ai.AiAdvisorScreen
import com.example.expensetracker.ui.profile.ProfileScreen
import com.example.expensetracker.ui.budget.BudgetScreen
import com.example.expensetracker.ui.auth.AuthScreenContainer

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") {
            AuthScreenContainer(navController, viewModel, isLogin = true)
        }

        composable("signup") {
            AuthScreenContainer(navController, viewModel, isLogin = false)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel)
        }

        composable(Screen.EMI.route) {
            EmiScreen(viewModel)
        }

        composable(Screen.AIAdvisor.route) {
            AiAdvisorScreen()
        }

        composable(Screen.Budget.route) {
            BudgetScreen(viewModel)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(viewModel)
        }

        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(viewModel)
        }
    }
}
