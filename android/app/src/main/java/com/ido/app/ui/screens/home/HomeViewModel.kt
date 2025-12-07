package com.ido.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ido.app.data.model.Task
import com.ido.app.data.model.sortedByPriority
import com.ido.app.data.repository.SyncStatus
import com.ido.app.data.repository.TaskRepository
import com.ido.app.notifications.TaskNotificationManager
import com.ido.app.sync.SyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel for Home screen
 */
class HomeViewModel(
    private val repository: TaskRepository,
    private val syncManager: SyncManager,
    private val notificationManager: TaskNotificationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _isSignedIn = MutableStateFlow(repository.isSignedIn())
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()
    
    private val _signedInAccount = MutableStateFlow<GoogleSignInAccount?>(repository.getSignedInAccount())
    val signedInAccount: StateFlow<GoogleSignInAccount?> = _signedInAccount.asStateFlow()
    
    init {
        loadTasks()
        observeSyncStatus()
    }
    
    /**
     * Load tasks from repository
     */
    private fun loadTasks() {
        viewModelScope.launch {
            repository.tasks.collect { tasks ->
                _uiState.update { it.copy(
                    tasks = tasks.sortedByPriority(),
                    isLoading = false
                ) }
            }
        }
    }
    
    /**
     * Observe sync status
     */
    private fun observeSyncStatus() {
        viewModelScope.launch {
            repository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
    }
    
    /**
     * Toggle task completion
     */
    fun toggleTaskDone(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            repository.updateTask(id = taskId, done = !task.done)
            syncManager.requestDebouncedSync()
        }
    }
    
    /**
     * Toggle task priority
     */
    fun toggleTaskPriority(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            repository.updateTask(id = taskId, priority = !task.priority)
            syncManager.requestDebouncedSync()
        }
    }
    
    /**
     * Delete task
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                // Cancel notification if scheduled
                notificationManager.cancelNotification(taskId)
            }
            repository.deleteTask(taskId)
            syncManager.requestDebouncedSync()
        }
    }
    
    /**
     * Refresh data (pull from Drive)
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            syncManager.requestSync()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    /**
     * Show create task sheet
     */
    fun showCreateTask() {
        _uiState.update { it.copy(showEditSheet = true, editingTask = null) }
    }
    
    /**
     * Show edit task sheet
     */
    fun showEditTask(task: Task) {
        _uiState.update { it.copy(showEditSheet = true, editingTask = task) }
    }
    
    /**
     * Hide edit sheet
     */
    fun hideEditSheet() {
        _uiState.update { it.copy(showEditSheet = false, editingTask = null) }
    }
    
    /**
     * Save task (create or update)
     */
    fun saveTask(text: String, priority: Boolean, dueDate: String?, reminderTime: String?) {
        viewModelScope.launch {
            val currentTask = _uiState.value.editingTask
            
            if (currentTask == null) {
                // Create new task
                val task = repository.createTask(
                    text = text,
                    priority = priority,
                    dueDate = dueDate,
                    reminderTime = reminderTime
                )
                
                // Schedule notification if reminder is set
                if (reminderTime != null) {
                    val instant = Instant.parse(reminderTime)
                    notificationManager.scheduleNotification(task.id, task.text, instant)
                }
            } else {
                // Update existing task
                repository.updateTask(
                    id = currentTask.id,
                    text = text,
                    priority = priority,
                    dueDate = dueDate,
                    reminderTime = reminderTime
                )
                
                // Update notification
                if (reminderTime != null) {
                    val instant = Instant.parse(reminderTime)
                    notificationManager.scheduleNotification(currentTask.id, text, instant)
                } else {
                    notificationManager.cancelNotification(currentTask.id)
                }
            }
            
            syncManager.requestDebouncedSync()
            hideEditSheet()
        }
    }
    
    /**
     * Handle sign in
     */
    fun handleSignIn(account: GoogleSignInAccount) {
        repository.initializeDrive(account)
        _isSignedIn.value = true
        _signedInAccount.value = account
        viewModelScope.launch {
            syncManager.requestSync()
            syncManager.schedulePeriodicSync()
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        repository.signOut()
        syncManager.cancelSync()
        _isSignedIn.value = false
        _signedInAccount.value = null
        // Update UI state to reflect sign-out
        _uiState.update { it.copy(
            syncStatus = "Not signed in"
        ) }
    }
    
    /**
     * Check if signed in
     */
    fun isSignedIn(): Boolean = repository.isSignedIn()
    
    /**
     * Get signed in account
     */
    fun getSignedInAccount() = repository.getSignedInAccount()
}

/**
 * UI state for Home screen
 */
data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val showEditSheet: Boolean = false,
    val editingTask: Task? = null
)
