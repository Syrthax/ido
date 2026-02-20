package com.ido.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ido.app.data.model.OptionalString
import com.ido.app.data.model.Task
import com.ido.app.data.model.sortedByPriority
import com.ido.app.data.repository.SyncStatus
import com.ido.app.data.repository.TaskRepository
import com.ido.app.data.repository.TaskSection
import com.ido.app.notifications.ReminderScheduler
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
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _isSignedIn = MutableStateFlow(repository.isSignedIn())
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()
    
    private val _signedInAccount = MutableStateFlow<GoogleSignInAccount?>(repository.getSignedInAccount())
    val signedInAccount: StateFlow<GoogleSignInAccount?> = _signedInAccount.asStateFlow()
    
    // Task sections for TasksScreen
    private val _taskSections = MutableStateFlow<Map<TaskSection, List<Task>>>(emptyMap())
    val taskSections: StateFlow<Map<TaskSection, List<Task>>> = _taskSections.asStateFlow()
    
    // Last sync time for SettingsScreen
    private val _lastSyncTime = MutableStateFlow<Instant?>(null)
    val lastSyncTime: StateFlow<Instant?> = _lastSyncTime.asStateFlow()
    
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
                // Update sectioned tasks for TasksScreen
                _taskSections.value = repository.getTasksBySection()
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
                // Update last sync time when sync completes successfully
                if (status is SyncStatus.Synced) {
                    _lastSyncTime.value = Instant.now()
                }
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
                // Cancel reminder if scheduled
                reminderScheduler.cancelReminder(taskId)
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
     * Force sync now (for cloud icon click)
     */
    fun syncNow() {
        viewModelScope.launch {
            syncManager.requestSync()
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
     * Save existing task (for CalendarItemSheet)
     */
    fun saveTask(task: Task) {
        viewModelScope.launch {
            Log.d(TAG, "saveTask(Task): id=${task.id}, dueDate=${task.dueDate}, reminderTime=${task.reminderTime}")
            
            repository.updateTask(
                id = task.id,
                text = task.text,
                done = task.done,
                priority = task.priority,
                dueDate = OptionalString.Provided(task.dueDate),
                reminderTime = OptionalString.Provided(task.reminderTime)
            )
            
            // Update reminder scheduling
            if (task.reminderTime != null && !task.done) {
                val instant = Instant.parse(task.reminderTime)
                if (instant.isAfter(Instant.now())) {
                    reminderScheduler.scheduleReminder(task.id, task.text, instant)
                }
            } else {
                reminderScheduler.cancelReminder(task.id)
            }
            
            syncManager.requestDebouncedSync()
        }
    }
    
    /**
     * Create task with Instant parameters (for calendar create task)
     */
    fun createTask(text: String, priority: Boolean, dueDate: Instant?, reminderTime: Instant?) {
        viewModelScope.launch {
            val task = repository.createTask(
                text = text,
                priority = priority,
                dueDate = dueDate?.toString(),
                reminderTime = reminderTime?.toString()
            )
            
            // Schedule reminder if set
            if (reminderTime != null && reminderTime.isAfter(Instant.now())) {
                reminderScheduler.scheduleReminder(task.id, task.text, reminderTime)
            }
            
            syncManager.requestDebouncedSync()
        }
    }
    
    /**
     * Save task (create or update)
     * 
     * CRITICAL FIX: Now uses OptionalString.Provided to explicitly pass dueDate/reminderTime
     * This ensures edited values are persisted correctly (fixes 7am->7pm bug)
     */
    fun saveTask(text: String, priority: Boolean, dueDate: String?, reminderTime: String?) {
        viewModelScope.launch {
            val currentTask = _uiState.value.editingTask
            
            Log.d(TAG, "saveTask: currentTask=${currentTask?.id}, dueDate=$dueDate, reminderTime=$reminderTime")
            
            if (currentTask == null) {
                // Create new task
                val task = repository.createTask(
                    text = text,
                    priority = priority,
                    dueDate = dueDate,
                    reminderTime = reminderTime
                )
                
                Log.d(TAG, "saveTask: CREATED task id=${task.id}, dueDate=${task.dueDate}, reminderTime=${task.reminderTime}")
                
                // Schedule reminder if set
                if (reminderTime != null) {
                    val instant = Instant.parse(reminderTime)
                    if (instant.isAfter(Instant.now())) {
                        reminderScheduler.scheduleReminder(task.id, task.text, instant)
                    }
                }
            } else {
                // Update existing task - MUST use OptionalString.Provided for explicit values
                Log.d(TAG, "saveTask: UPDATING task id=${currentTask.id}")
                Log.d(TAG, "saveTask: BEFORE - currentTask.dueDate=${currentTask.dueDate}, currentTask.reminderTime=${currentTask.reminderTime}")
                Log.d(TAG, "saveTask: NEW VALUES - dueDate=$dueDate, reminderTime=$reminderTime")
                
                repository.updateTask(
                    id = currentTask.id,
                    text = text,
                    priority = priority,
                    dueDate = OptionalString.Provided(dueDate),
                    reminderTime = OptionalString.Provided(reminderTime)
                )
                
                // Update reminder scheduling
                if (reminderTime != null) {
                    val instant = Instant.parse(reminderTime)
                    if (instant.isAfter(Instant.now())) {
                        reminderScheduler.scheduleReminder(currentTask.id, text, instant)
                    }
                } else {
                    reminderScheduler.cancelReminder(currentTask.id)
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
        // Initialize Drive service first
        repository.initializeDrive(account)
        _isSignedIn.value = true
        _signedInAccount.value = account
        
        // Trigger immediate sync
        viewModelScope.launch {
            repository.syncWithDrive()
            syncManager.schedulePeriodicSync()
        }
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        // Cancel all reminders before signing out
        syncManager.cancelAllReminders()
        repository.signOut()
        syncManager.cancelSync()
        _isSignedIn.value = false
        _signedInAccount.value = null
    }
    
    /**
     * Update sync interval
     */
    fun updateSyncInterval(interval: com.ido.app.notifications.SyncInterval) {
        syncManager.updateSyncInterval(interval)
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
