package com.example.expensetracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen() {
    var darkModeEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var privacyEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        SettingsToggleItem(
            icon = Icons.Default.Info,
            title = "Dark Mode",
            checked = darkModeEnabled,
            onCheckedChange = { darkModeEnabled = it }
        )

        SettingsToggleItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            checked = notificationsEnabled,
            onCheckedChange = { notificationsEnabled = it }
        )

        SettingsToggleItem(
            icon = Icons.Default.Warning,
            title = "Privacy",
            checked = privacyEnabled,
            onCheckedChange = { privacyEnabled = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsActionItem(
            icon = Icons.Default.Share,
            title = "Export Data",
            onClick = { /* TODO: Export functionality */ }
        )

        SettingsActionItem(
            icon = Icons.Default.Delete,
            title = "Clear All Data",
            onClick = { /* TODO: Clear data logic */ }
        )
    }
}


@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontSize = 16.sp, color = Color.White)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF6366F1),
                    uncheckedThumbColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun SettingsActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 16.sp, color = Color.White)
        }
    }
}
