package com.ido.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.ido.app.data.remote.CalendarDataSource
import com.ido.app.data.repository.TaskRepository
import com.ido.app.notifications.TaskNotificationManager
import com.ido.app.sync.SyncManager
import com.ido.app.ui.navigation.Tab
import com.ido.app.ui.screens.calendar.CalendarScreen
import com.ido.app.ui.screens.calendar.CalendarViewModel
import com.ido.app.ui.screens.home.HomeViewModel
import com.ido.app.ui.screens.settings.SettingsScreen
import com.ido.app.ui.screens.tasks.TasksScreen
import com.ido.app.ui.theme.IDoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var repository: TaskRepository
    private lateinit var syncManager: SyncManager
    private lateinit var notificationManager: TaskNotificationManager
    private lateinit var calendarDataSource: CalendarDataSource
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
        
        // Initialize components
        repository = TaskRepository(applicationContext)
        syncManager = SyncManager(applicationContext, repository)
        notificationManager = TaskNotificationManager(applicationContext)
        calendarDataSource = CalendarDataSource(applicationContext)
        viewModel = HomeViewModel(repository, syncManager, notificationManager)
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
                        onSignInClick = { startSignIn() }
                    )
                }
            }
        }
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
@Composable
fun AppWithBottomNavigation(
    viewModel: HomeViewModel,
    calendarViewModel: CalendarViewModel,
    onSignInClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(Tab.TASKS) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { 
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                Tab.TASKS -> TasksScreen(viewModel = viewModel)
                Tab.CALENDAR -> CalendarScreen(
                    viewModel = viewModel,
                    calendarViewModel = calendarViewModel
                )
                Tab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onSignInClick = onSignInClick
                )
            }
        }
    }
}
