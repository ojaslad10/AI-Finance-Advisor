package com.example.expensetracker.ui.setting

data class SettingsState(
    val darkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val privacyEnabled: Boolean = false
)