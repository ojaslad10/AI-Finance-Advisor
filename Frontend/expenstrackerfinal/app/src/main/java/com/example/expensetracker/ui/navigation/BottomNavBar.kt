package com.example.expensetracker.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        Screen.Dashboard,
        Screen.EMI,
        Screen.AddTransaction,
        Screen.AIAdvisor,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color(0xFF1E1E1E),
        tonalElevation = 6.dp
    ) {
        items.forEach { screen ->
            val selected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    Column(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val iconImage = when (screen) {
                            Screen.Dashboard -> Icons.Default.Home
                            Screen.EMI -> Icons.Default.List
                            Screen.AddTransaction -> Icons.Default.Add
                            Screen.AIAdvisor -> Icons.Default.Build
                            Screen.Profile -> Icons.Default.Person
                            else -> Icons.Default.Home
                        }

                        Icon(
                            imageVector = iconImage,
                            contentDescription = screen.title,
                            tint = if (selected) Color(0xFF6366F1) else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = if (screen == Screen.AIAdvisor) "Advisor" else screen.title,
                            color = if (selected) Color(0xFF6366F1) else Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                // shrink highlight/indicator to match content
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF2D2D2D),
                    selectedIconColor = Color(0xFF6366F1),
                    selectedTextColor = Color(0xFF6366F1),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
