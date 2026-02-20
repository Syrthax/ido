package com.ido.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.ido.app.data.remote.CalendarDataSource
import com.ido.app.data.repository.TaskRepository
import com.ido.app.notifications.NotificationPermissionHandler
import com.ido.app.notifications.PermissionState
import com.ido.app.notifications.ReminderScheduler
import com.ido.app.sync.SyncManager
import com.ido.app.ui.components.FloatingDock
import com.ido.app.ui.navigation.Tab
import com.ido.app.ui.screens.calendar.CalendarMonthView
import com.ido.app.ui.screens.calendar.CalendarViewModel
import com.ido.app.ui.screens.home.HomeViewModel
import com.ido.app.ui.screens.settings.SettingsScreenRevamped
import com.ido.app.ui.screens.tasks.TasksScreenRevamped
import com.ido.app.ui.theme.IDoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var repository: TaskRepository
    private lateinit var syncManager: SyncManager
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var calendarDataSource: CalendarDataSource
    private lateinit var notificationPermissionHandler: NotificationPermissionHandler
    private lateinit var viewModel: HomeViewModel
    private lateinit var calendarViewModel: CalendarViewModel
    
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult()
            viewModel.handleSignIn(account)
            // Initialize calendar service with the signed-in account
            calendarDataSource.initializeCalendarService(account)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge before setContent
        enableEdgeToEdge()
        
        // Initialize components
        repository = TaskRepository(applicationContext)
        syncManager = SyncManager(applicationContext, repository)
        reminderScheduler = ReminderScheduler(applicationContext)
        calendarDataSource = CalendarDataSource(applicationContext)
        notificationPermissionHandler = NotificationPermissionHandler.getInstance(applicationContext)
        viewModel = HomeViewModel(repository, syncManager, reminderScheduler)
        calendarViewModel = CalendarViewModel(repository, calendarDataSource)
        
        // Load data
        lifecycleScope.launch {
            repository.initialize()
            
            // Initialize Drive and Calendar services if user is already signed in
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account != null) {
                repository.initializeDrive(account)
                calendarDataSource.initializeCalendarService(account)
                syncManager.requestSync()
                syncManager.schedulePeriodicSync()
            }
        }
        
        setContent {
            IDoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWithBottomNavigation(
                        viewModel = viewModel,
                        calendarViewModel = calendarViewModel,
                        notificationPermissionHandler = notificationPermissionHandler,
                        onSignInClick = { startSignIn() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh permission state when returning from settings
        notificationPermissionHandler.refreshState()
    }
    
    private fun startSignIn() {
        val signInIntent = GoogleSignIn.getClient(
            this,
            repository.getSignInOptions()
        ).signInIntent
        
        signInLauncher.launch(signInIntent)
    }
}

/**
 * Main app composable with bottom navigation
 * Implements 3-tab layout: Tasks / Calendar / Settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWithBottomNavigation(
    viewModel: HomeViewModel,
    calendarViewModel: CalendarViewModel,
    notificationPermissionHandler: NotificationPermissionHandler,
    onSignInClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(Tab.TASKS) }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    
    // Notification permission state
    val permissionState by notificationPermissionHandler.permissionState.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionHandler.markAsked()
        notificationPermissionHandler.refreshState()
    }
    
    // Check if we should prompt for permission on first launch
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            notificationPermissionHandler.shouldPromptForPermission()) {
            showPermissionDialog = true
        }
    }
    
    // Notification permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPermissionDialog = false
                notificationPermissionHandler.markAsked()
            },
            icon = { Text("🔔", style = MaterialTheme.typography.headlineLarge) },
            title = { 
                Text(
                    "Enable Notifications",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    "iDo would like to send you task reminders. " +
                    "You can change this anytime in Settings.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPermissionDialog = false
                        notificationPermissionHandler.markAsked()
                    }
                ) {
                    Text("Not now")
                }
            }
        )
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content
            when (selectedTab) {
                Tab.TASKS -> TasksScreenRevamped(viewModel = viewModel)
                Tab.CALENDAR -> CalendarMonthView(
                    viewModel = viewModel,
                    calendarViewModel = calendarViewModel
                )
                Tab.SETTINGS -> SettingsScreenRevamped(
                    viewModel = viewModel,
                    notificationPermissionHandler = notificationPermissionHandler,
                    onSignInClick = onSignInClick
                )
            }
            
            // Floating Dock at the bottom
            FloatingDock(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedTab = tab
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
