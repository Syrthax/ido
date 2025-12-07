package com.ido.app.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ido.app.data.local.LocalDataSource
import com.ido.app.data.model.Task
import com.ido.app.data.model.activeTasks
import com.ido.app.data.remote.DriveDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Task repository implementing offline-first architecture
 * 
 * Data flow:
 * 1. Load local data immediately
 * 2. Sync with Drive in background
 * 3. Merge using conflict resolution
 * 4. Update local cache
 */
class TaskRepository(context: Context) {
    
    private val localDataSource = LocalDataSource(context)
    private val driveDataSource = DriveDataSource(context)
    
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: Flow<List<Task>> = _tasks.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    /**
     * Initialize repository - load local data first
     */
    suspend fun initialize() {
        _syncStatus.value = SyncStatus.Loading
        val localTasks = localDataSource.loadTasks()
        _tasks.value = localTasks.activeTasks()
        _syncStatus.value = SyncStatus.Idle
    }
    
    /**
     * Get all active tasks (non-deleted)
     */
    fun getActiveTasks(): List<Task> {
        return _tasks.value
    }
    
    /**
     * Get task by ID
     */
    fun getTaskById(id: String): Task? {
        return _tasks.value.find { it.id == id }
    }
    
    /**
     * Create new task
     */
    suspend fun createTask(
        text: String,
        priority: Boolean = false,
        dueDate: String? = null,
        reminderTime: String? = null
    ): Task {
        val task = Task.create(
            text = text,
            priority = priority,
            dueDate = dueDate,
            reminderTime = reminderTime
        )
        
        val updatedTasks = _tasks.value + task
        _tasks.value = updatedTasks
        
        // Save locally
        saveTasksLocally(updatedTasks)
        
        return task
    }
    
    /**
     * Update existing task
     */
    suspend fun updateTask(
        id: String,
        text: String? = null,
        done: Boolean? = null,
        priority: Boolean? = null,
        dueDate: String? = null,
        reminderTime: String? = null,
        notified: Boolean? = null
    ): Task? {
        val task = getTaskById(id) ?: return null
        
        val updated = task.update(
            text = text,
            done = done,
            priority = priority,
            dueDate = dueDate,
            reminderTime = reminderTime,
            notified = notified
        )
        
        val updatedTasks = _tasks.value.map { 
            if (it.id == id) updated else it 
        }
        _tasks.value = updatedTasks
        
        // Save locally
        saveTasksLocally(updatedTasks)
        
        return updated
    }
    
    /**
     * Delete task (soft delete)
     */
    suspend fun deleteTask(id: String): Boolean {
        val task = getTaskById(id) ?: return false
        
        val deleted = task.markDeleted()
        val updatedTasks = _tasks.value.map { 
            if (it.id == id) deleted else it 
        }
        
        // Update local view (hide deleted)
        _tasks.value = updatedTasks.filter { !it.deleted }
        
        // Save to local file (including deleted for sync)
        saveTasksLocally(updatedTasks)
        
        return true
    }
    
    /**
     * Save tasks to local storage
     */
    private suspend fun saveTasksLocally(tasks: List<Task>) {
        localDataSource.saveTasks(tasks)
    }
    
    /**
     * Sync with Google Drive
     */
    suspend fun syncWithDrive(): SyncResult {
        if (!driveDataSource.isSignedIn()) {
            return SyncResult.NotSignedIn
        }
        
        _syncStatus.value = SyncStatus.Syncing
        
        try {
            // Load local tasks (including deleted)
            val localTasks = localDataSource.loadTasks()
            
            // Load remote tasks
            val remoteTasks = driveDataSource.loadTasks()
            
            if (remoteTasks == null) {
                // First sync or network error - upload local data
                val uploaded = driveDataSource.saveTasks(localTasks)
                _syncStatus.value = SyncStatus.Synced
                return if (uploaded) SyncResult.Success else SyncResult.Error("Failed to upload")
            }
            
            // Merge local and remote tasks
            val merged = mergeTasks(localTasks, remoteTasks)
            
            // Save merged data locally
            localDataSource.saveTasks(merged)
            
            // Upload merged data to Drive
            driveDataSource.saveTasks(merged)
            
            // Update in-memory cache (active tasks only)
            _tasks.value = merged.activeTasks()
            
            _syncStatus.value = SyncStatus.Synced
            return SyncResult.Success
            
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
            return SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Merge local and remote tasks using conflict resolution
     * Strategy: Last write wins (based on updatedAt)
     */
    private fun mergeTasks(local: List<Task>, remote: List<Task>): List<Task> {
        val taskMap = mutableMapOf<String, Task>()
        
        // Add all local tasks
        local.forEach { task ->
            taskMap[task.id] = task
        }
        
        // Merge remote tasks
        remote.forEach { remoteTask ->
            val localTask = taskMap[remoteTask.id]
            
            if (localTask != null) {
                // Conflict - use last write wins
                taskMap[remoteTask.id] = mergeTaskVersions(localTask, remoteTask)
            } else {
                // New remote task
                taskMap[remoteTask.id] = remoteTask
            }
        }
        
        return taskMap.values.toList()
    }
    
    /**
     * Merge two versions of the same task
     * Uses updatedAt timestamp - newer wins
     */
    private fun mergeTaskVersions(local: Task, remote: Task): Task {
        return try {
            val localTime = Instant.parse(local.updatedAt)
            val remoteTime = Instant.parse(remote.updatedAt)
            
            if (remoteTime.isAfter(localTime)) remote else local
        } catch (e: Exception) {
            // If timestamps can't be parsed, prefer remote
            remote
        }
    }
    
    /**
     * Initialize Drive service after sign-in
     */
    fun initializeDrive(account: GoogleSignInAccount) {
        driveDataSource.initializeDriveService(account)
    }
    
    /**
     * Check if signed in to Drive
     */
    fun isSignedIn(): Boolean {
        return driveDataSource.isSignedIn()
    }
    
    /**
     * Get signed in account
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return driveDataSource.getSignedInAccount()
    }
    
    /**
     * Get Drive sign-in options
     */
    fun getSignInOptions() = driveDataSource.getSignInOptions()
    
    /**
     * Sign out from Drive
     */
    fun signOut() {
        driveDataSource.signOut()
    }
    
    /**
     * Clear all local data
     */
    suspend fun clearAllData() {
        localDataSource.clearAllData()
        _tasks.value = emptyList()
    }
}

/**
 * Sync status states
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Loading : SyncStatus()
    object Syncing : SyncStatus()
    object Synced : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

/**
 * Sync result
 */
sealed class SyncResult {
    object Success : SyncResult()
    object NotSignedIn : SyncResult()
    data class Error(val message: String) : SyncResult()
}
