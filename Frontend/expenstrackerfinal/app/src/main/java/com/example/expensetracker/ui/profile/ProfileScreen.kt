package com.example.expensetracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.MainViewModel

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Picture",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // User Info
        Text("Ojas Lad", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("ojasladofficial150@gmail.com", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard("Income", "₹85,000", Color(0xFF4CAF50), Modifier.weight(1f))
            StatCard("Expense", "₹45,230", Color(0xFFFF6B6B), Modifier.weight(1f))
            StatCard("EMIs", "₹18,500", Color(0xFFFFC107), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Settings Options
        ProfileOption(icon = Icons.Default.Settings, title = "Account Settings")
        ProfileOption(icon = Icons.Default.Notifications, title = "Notifications")
        ProfileOption(icon = Icons.Default.Person, title = "Privacy")

        Spacer(modifier = Modifier.height(32.dp))

        // Logout Button
        Button(
            onClick = { /* TODO: Handle logout */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Logout", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ProfileOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = Color.White)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp)
    }
}
