package com.ido.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation tabs for the app
 */
enum class Tab(
    val label: String,
    val icon: ImageVector
) {
    TASKS("Tasks", Icons.Default.Checklist),
    CALENDAR("Calendar", Icons.Default.CalendarMonth),
    SETTINGS("Settings", Icons.Default.Settings)
}
