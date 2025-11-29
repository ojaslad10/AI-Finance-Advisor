package com.example.expensetracker.ui.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.ui.MainViewModel
import com.example.expensetracker.ui.navigation.Screen
import androidx.compose.material3.NavigationDrawerItem

@Composable
fun DrawerContent(
    navController: NavController,
    viewModel: MainViewModel,
    onDestinationClicked: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF1E1E1E)
    ) {
        Text(
            text = "Hi, ${viewModel.currentUser?.name ?: "Guest"}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = viewModel.currentUser?.email ?: "No email",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, bottom = 24.dp)
        )

        NavigationDrawerItem(
            label = { Text("Dashboard", color = Color.White) },
            selected = false,
            onClick = { onDestinationClicked(Screen.Dashboard.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color.White) }
        )
        NavigationDrawerItem(
            label = { Text("EMI", color = Color.White) },
            selected = false,
            onClick = { onDestinationClicked(Screen.EMI.route) },
            icon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.White) }
        )
        NavigationDrawerItem(
            label = { Text("Budget", color = Color.White) },
            selected = false,
            onClick = { onDestinationClicked(Screen.Budget.route) },
            icon = { Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color.White) }
        )
        NavigationDrawerItem(
            label = { Text("Settings", color = Color.White) },
            selected = false,
            onClick = { onDestinationClicked(Screen.Settings.route) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }
        )
//        NavigationDrawerItem(
//            label = { Text("Profile", color = Color.White) },
//            selected = false,
//            onClick = { onDestinationClicked(Screen.Profile.route) },
//            icon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White) }
//        )

        NavigationDrawerItem(
            label = { Text("Logout", color = Color.White) },
            selected = false,
            onClick = {
                viewModel.logout {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White) }
        )


    }
}


