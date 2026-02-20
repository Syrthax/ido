package com.ido.app.ui.screens.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ido.app.data.repository.SyncStatus
import com.ido.app.notifications.NotificationPermissionHandler
import com.ido.app.notifications.NotificationPreferences
import com.ido.app.notifications.PermissionState
import com.ido.app.ui.screens.home.HomeViewModel
import com.ido.app.ui.theme.ThemeMode
import com.ido.app.ui.theme.ThemePreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Revamped Settings Screen matching iDo design specs
 * 
 * Features:
 * - Clean header with user profile card
 * - Sync status section with toggle
 * - Appearance settings (dark mode, OLED)
 * - General settings with icon badges
 * - About section with version info
 * - Log out button
 * - No Scaffold - edge-to-edge design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenRevamped(
    viewModel: HomeViewModel,
    notificationPermissionHandler: NotificationPermissionHandler,
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences.getInstance(context) }
    val notificationPreferences = remember { NotificationPreferences.getInstance(context) }
    val currentTheme by themePreferences.themeMode.collectAsState()
    val permissionState by notificationPermissionHandler.permissionState.collectAsState()
    
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val account by viewModel.signedInAccount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()
    val hapticFeedback = LocalHapticFeedback.current
    
    // State for toggles
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var oledModeEnabled by remember { mutableStateOf(true) }
    
    // Appearance dropdown state
    var showThemeDropdown by remember { mutableStateOf(false) }
    val themeOptions = listOf("Follow System", "Light Mode", "Dark Mode")
    val selectedThemeIndex = when (currentTheme) {
        ThemeMode.AUTO -> 0
        ThemeMode.LIGHT -> 1
        ThemeMode.DARK -> 2
    }
    
    // Sync error state
    var showSyncErrorDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp) // Space for dock
        ) {
            // Header
            SettingsHeader(isDark = isDark)
            
            // Profile Card
            ProfileCard(
                displayName = account?.displayName ?: "Guest User",
                email = account?.email,
                photoUrl = account?.photoUrl?.toString(),
                isSignedIn = isSignedIn,
                onSignInClick = onSignInClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sync Status Section
            if (isSignedIn) {
                SectionLabel("SYNC STATUS")
                
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Auto-sync toggle
                    SettingsToggleItem(
                        icon = Icons.Outlined.Sync,
                        iconColor = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        title = "Auto-sync",
                        subtitle = "Sync data across all devices",
                        isChecked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    // Last synced
                    SettingsInfoItem(
                        icon = Icons.Outlined.CloudDone,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                        title = "Last Synced",
                        value = formatLastSyncTime(lastSyncTime),
                        action = {
                            TextButton(onClick = { viewModel.syncNow() }) {
                                Text(
                                    "Sync Now",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Appearance Section - show for all modes
            SectionLabel("APPEARANCE")
            
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Theme dropdown
                SettingsDropdownItem(
                    icon = Icons.Outlined.DarkMode,
                    iconColor = if (isDark) Color.White else Color.Black,
                    iconBackground = if (isDark) Color.Black else Color(0xFFF0F0F0),
                    title = "Theme",
                    selectedValue = themeOptions[selectedThemeIndex],
                    options = themeOptions,
                    expanded = showThemeDropdown,
                    onExpandedChange = { showThemeDropdown = it },
                    onOptionSelected = { index ->
                        val mode = when (index) {
                            0 -> ThemeMode.AUTO
                            1 -> ThemeMode.LIGHT
                            else -> ThemeMode.DARK
                        }
                        themePreferences.setThemeMode(mode)
                        showThemeDropdown = false
                    }
                )
                
                if (isDark) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Outlined.Contrast,
                        iconColor = Color.White,
                        iconBackground = Color.Black,
                        title = "True Black (OLED)",
                        isChecked = oledModeEnabled,
                        onCheckedChange = { oledModeEnabled = it }
                    )
                }
            }
            
            if (isDark) {
                Text(
                    text = "Use pure black background to save battery on OLED screens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Account Section
            SectionLabel("ACCOUNT")
            
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsNavigationItem(
                    icon = Icons.Outlined.Notifications,
                    iconColor = Color(0xFF3B82F6), // Blue
                    iconBackground = Color(0xFF3B82F6).copy(alpha = 0.2f),
                    title = "Notifications",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Send demo notification
                        sendDemoNotification(context)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // General Section (dark mode design)
            if (isDark) {
                SectionLabel("GENERAL")
                
                SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsNavigationItem(
                        icon = Icons.Outlined.Notifications,
                        iconColor = Color(0xFF3B82F6),
                        iconBackground = Color(0xFF3B82F6).copy(alpha = 0.2f),
                        title = "Notifications",
                        onClick = { sendDemoNotification(context) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Outlined.Sync,
                        iconColor = Color(0xFFF59E0B), // Orange
                        iconBackground = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        title = "Sync",
                        value = if (autoSyncEnabled) "On" else "Off",
                        onClick = { viewModel.syncNow() }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    SettingsNavigationItem(
                        icon = Icons.Outlined.Lock,
                        iconColor = Color(0xFF10B981), // Green
                        iconBackground = Color(0xFF10B981).copy(alpha = 0.2f),
                        title = "Privacy",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sarthakg.tech/ido/privacy-policy"))
                            context.startActivity(intent)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // About Section
            SectionLabel("ABOUT IDO")
            
            SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsInfoItem(
                    icon = Icons.Outlined.Info,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    title = "App Version",
                    value = "v2.4.0"
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                
                SettingsNavigationItem(
                    icon = Icons.Outlined.Description,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    title = "Privacy Policy",
                    showExternalIcon = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sarthakg.tech/ido/privacy-policy"))
                        context.startActivity(intent)
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                
                SettingsNavigationItem(
                    icon = Icons.Outlined.Star,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    title = "Star Project",
                    showExternalIcon = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Syrthax/ido"))
                        context.startActivity(intent)
                    }
                )
            }
            
            // Log Out Button
            if (isSignedIn) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Log Out",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Footer
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App icon
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "iDo v2.4.0",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "Made with ❤️ by KrispLabs & Sarthak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsHeader(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 56.dp, bottom = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ProfileCard(
    displayName: String,
    email: String?,
    photoUrl: String?,
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (photoUrl != null && isSignedIn) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Initials avatar
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (isSignedIn && email != null) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!isSignedIn) {
                    Text(
                        text = "Sign in to sync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Chevron or sign in
            IconButton(
                onClick = if (isSignedIn) { {} } else onSignInClick,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Edit profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = iconBackground
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Toggle
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759), // iOS green
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun SettingsDropdownItem(
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    selectedValue: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = iconBackground
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Dropdown
        Box {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = option,
                                color = if (option == selectedValue) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { onOptionSelected(index) },
                        leadingIcon = if (option == selectedValue) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    value: String,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = iconBackground
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (action != null) {
            action()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    value: String? = null,
    showExternalIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = iconBackground
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Icon(
            if (showExternalIcon) Icons.Outlined.OpenInNew else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatLastSyncTime(lastSyncTime: java.time.Instant?): String {
    if (lastSyncTime == null) return "Never"
    
    val now = java.time.Instant.now()
    val diff = now.toEpochMilli() - lastSyncTime.toEpochMilli()
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} mins ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            lastSyncTime.atZone(ZoneId.systemDefault()).format(formatter)
        }
    }
}

private const val NOTIFICATION_CHANNEL_ID = "ido_test_channel"
private const val NOTIFICATION_ID = 12345

private fun sendDemoNotification(context: Context) {
    // Check notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "Notifications are disabled. Please enable them in system settings.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
    }
    
    // Create notification channel for Android 8.0+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "iDo Test Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Test notifications from iDo"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    // Build and send notification
    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("ido test notification")
        .setContentText("Notifications are working correctly")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    
    try {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Toast.makeText(
            context,
            "Notifications are disabled. Please enable them in system settings.",
            Toast.LENGTH_LONG
        ).show()
    }
}
