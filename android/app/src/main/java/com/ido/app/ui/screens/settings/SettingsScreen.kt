package com.ido.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ido.app.data.repository.SyncStatus
import com.ido.app.notifications.NotificationPermissionHandler
import com.ido.app.notifications.NotificationPreferences
import com.ido.app.notifications.PermissionState
import com.ido.app.notifications.SyncInterval
import com.ido.app.ui.screens.home.HomeViewModel
import com.ido.app.ui.theme.ThemeMode
import com.ido.app.ui.theme.ThemePreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    notificationPermissionHandler: NotificationPermissionHandler,
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences.getInstance(context) }
    val notificationPreferences = remember { NotificationPreferences.getInstance(context) }
    val currentTheme by themePreferences.themeMode.collectAsState()
    val currentSyncInterval by notificationPreferences.syncInterval.collectAsState()
    val permissionState by notificationPermissionHandler.permissionState.collectAsState()
    
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val account by viewModel.signedInAccount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // Profile Card
            if (isSignedIn && account != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        if (account?.photoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(account?.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = account?.displayName?.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account?.displayName ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = account?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Not signed in
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Appearance Section
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (currentTheme) {
                                    ThemeMode.AUTO -> "Auto (system)"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Theme selector buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentTheme == mode,
                                onClick = { themePreferences.setThemeMode(mode) },
                                label = { 
                                    Text(
                                        when (mode) {
                                            ThemeMode.AUTO -> "Auto"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sync Section
            Text(
                text = "SYNC",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Last Synced Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (uiState.syncStatus) {
                                    is SyncStatus.Synced -> Icons.Default.CheckCircle
                                    is SyncStatus.Error -> Icons.Default.Error
                                    else -> Icons.Default.Sync
                                },
                                contentDescription = null,
                                tint = when (uiState.syncStatus) {
                                    is SyncStatus.Synced -> MaterialTheme.colorScheme.primary
                                    is SyncStatus.Error -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Last synced",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = lastSyncTime?.let { formatLastSyncTime(it) } ?: "Never",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Sync Now button
                    OutlinedButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isSignedIn && uiState.syncStatus !is SyncStatus.Syncing
                    ) {
                        if (uiState.syncStatus is SyncStatus.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync now")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Background sync interval
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Background sync interval",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = currentSyncInterval.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Sync interval selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncInterval.entries.forEach { interval ->
                            FilterChip(
                                selected = currentSyncInterval == interval,
                                onClick = { 
                                    notificationPreferences.setSyncInterval(interval)
                                    viewModel.updateSyncInterval(interval)
                                },
                                label = { 
                                    Text(
                                        when (interval) {
                                            SyncInterval.FIFTEEN_MINUTES -> "15m"
                                            SyncInterval.THIRTY_MINUTES -> "30m"
                                            SyncInterval.ONE_HOUR -> "1h"
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Info text
                    Text(
                        text = "iDo syncs directly with Google Drive. Background sync keeps your reminders accurate across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Notifications Section
            Text(
                text = "NOTIFICATIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (permissionState == PermissionState.GRANTED) 
                                    Icons.Default.NotificationsActive 
                                else 
                                    Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (permissionState == PermissionState.GRANTED)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Task reminders",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (permissionState) {
                                        PermissionState.GRANTED -> "Enabled"
                                        PermissionState.NOT_ASKED -> "Not configured"
                                        PermissionState.DENIED_SETTINGS -> "Disabled"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (permissionState == PermissionState.GRANTED)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    if (permissionState != PermissionState.GRANTED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = {
                                // Open app notification settings
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable notifications")
                        }
                    }
                    
                    // Test notification button (always visible for diagnostics)
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var testNotificationResult by remember { mutableStateOf<String?>(null) }
                    
                    OutlinedButton(
                        onClick = {
                            // Refresh permission state first
                            notificationPermissionHandler.refreshState()
                            
                            if (notificationPermissionHandler.areNotificationsEnabled()) {
                                // Fire test notification immediately
                                val notificationManager = com.ido.app.notifications.TaskNotificationManager(context)
                                notificationManager.showNotification(
                                    taskId = "test_${System.currentTimeMillis()}",
                                    taskText = "🎉 Test notification successful! Your reminders will work.",
                                    isOverdue = false
                                )
                                testNotificationResult = "✅ Notification sent!"
                            } else {
                                testNotificationResult = "❌ Notifications are disabled. Please enable them in system settings."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification")
                    }
                    
                    // Show result message
                    if (testNotificationResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testNotificationResult!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testNotificationResult!!.startsWith("✅"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Account Section
            if (isSignedIn) {
                Text(
                    text = "ACCOUNT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Log out",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App version (bottom)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "iDo v2.0.5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Build with ♥ by Sarthak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Format last sync time for display
 */
private fun formatLastSyncTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
