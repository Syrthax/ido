package com.ido.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.ido.app.data.repository.TaskRepository
import com.ido.app.notifications.TaskNotificationManager
import com.ido.app.sync.SyncManager
import com.ido.app.ui.screens.home.HomeScreen
import com.ido.app.ui.screens.home.HomeViewModel
import com.ido.app.ui.theme.IDoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var repository: TaskRepository
    private lateinit var syncManager: SyncManager
    private lateinit var notificationManager: TaskNotificationManager
    private lateinit var viewModel: HomeViewModel
    
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult()
            viewModel.handleSignIn(account)
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
        viewModel = HomeViewModel(repository, syncManager, notificationManager)
        
        // Load data
        lifecycleScope.launch {
            repository.initialize()
            
            // Initialize Drive service if user is already signed in
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (account != null) {
                repository.initializeDrive(account)
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
                    AppNavigation(
                        viewModel = viewModel,
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

@Composable
fun AppNavigation(
    viewModel: HomeViewModel,
    onSignInClick: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("home") }
    
    when (currentScreen) {
        "home" -> HomeScreen(
            viewModel = viewModel,
            onNavigateToSettings = { currentScreen = "settings" }
        )
        "settings" -> SettingsScreen(
            viewModel = viewModel,
            onSignInClick = onSignInClick,
            onNavigateBack = { currentScreen = "home" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onSignInClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val account by viewModel.signedInAccount.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isSignedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Signed in as",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = account?.email ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.signOut() }
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            } else {
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.refresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync Now")
            }
        }
    }
}
