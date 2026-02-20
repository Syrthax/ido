package com.ido.app.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ido.app.data.model.Task
import com.ido.app.data.repository.SyncResult
import com.ido.app.data.repository.TaskRepository
import com.ido.app.notifications.NotificationPreferences
import com.ido.app.notifications.ReminderScheduler
import com.ido.app.notifications.SyncInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Enhanced SyncManager with configurable periodic sync
 * 
 * Features:
 * - Configurable sync interval (15min, 30min, 1hr)
 * - Automatic reminder rescheduling after sync
 * - Network-aware sync constraints
 * - Debounced manual sync
 */
class SyncManager(context: Context, private val repository: TaskRepository) {
    
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val reminderScheduler = ReminderScheduler(appContext)
    private val notificationPreferences = NotificationPreferences.getInstance(appContext)
    
    private val _lastSyncTime = MutableStateFlow<Long>(0)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime
    
    /**
     * Request immediate sync and reschedule reminders
     */
    suspend fun requestSync() {
        if (!repository.isSignedIn()) return
        
        val result = repository.syncWithDrive()
        if (result is SyncResult.Success) {
            _lastSyncTime.value = System.currentTimeMillis()
            // Reschedule all reminders based on synced data
            rescheduleAllReminders()
        }
    }
    
    /**
     * Request debounced sync (waits 2 seconds before syncing)
     */
    suspend fun requestDebouncedSync() {
        if (!repository.isSignedIn()) return
        
        // Cancel any pending sync work
        workManager.cancelAllWorkByTag(SYNC_WORK_TAG)
        
        // Schedule new sync work with delay
        val syncWork = OneTimeWorkRequestBuilder<EnhancedSyncWorker>()
            .setInitialDelay(DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS)
            .addTag(SYNC_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueue(syncWork)
    }
    
    /**
     * Schedule periodic background sync with configurable interval
     */
    fun schedulePeriodicSync() {
        if (!repository.isSignedIn()) return
        
        val interval = notificationPreferences.syncInterval.value
        
        Log.d(TAG, "Scheduling periodic sync with interval: ${interval.displayName}")
        
        val periodicWork = PeriodicWorkRequestBuilder<EnhancedSyncWorker>(
            interval.minutes,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(PERIODIC_SYNC_TAG)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update to use new interval
            periodicWork
        )
    }
    
    /**
     * Update sync interval and reschedule periodic work
     */
    fun updateSyncInterval(interval: SyncInterval) {
        notificationPreferences.setSyncInterval(interval)
        
        // Cancel and reschedule with new interval
        if (repository.isSignedIn()) {
            workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
            schedulePeriodicSync()
        }
    }
    
    /**
     * Cancel all scheduled sync work
     */
    fun cancelSync() {
        workManager.cancelAllWorkByTag(SYNC_WORK_TAG)
        workManager.cancelAllWorkByTag(PERIODIC_SYNC_TAG)
    }
    
    /**
     * Reschedule all reminders based on current task state
     * 
     * Called after sync to ensure reminders match Drive state
     */
    private suspend fun rescheduleAllReminders() {
        try {
            val tasks = repository.getActiveTasks()
            
            for (task in tasks) {
                if (task.reminderTime != null && !task.done && !task.deleted && !task.notified) {
                    val reminderInstant = Instant.parse(task.reminderTime)
                    if (reminderInstant.isAfter(Instant.now())) {
                        reminderScheduler.scheduleReminder(task.id, task.text, reminderInstant)
                    }
                } else {
                    // Cancel any existing reminders for tasks that don't need them
                    reminderScheduler.cancelReminder(task.id)
                }
            }
            
            Log.d(TAG, "Rescheduled reminders for ${tasks.count { it.reminderTime != null }} tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule reminders", e)
        }
    }
    
    /**
     * Cancel all reminders (used on sign out)
     */
    fun cancelAllReminders() {
        reminderScheduler.cancelAllReminders()
    }
    
    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_WORK_TAG = "sync_work"
        private const val PERIODIC_SYNC_TAG = "periodic_sync"
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
        private const val DEBOUNCE_DELAY_MS = 2000L
    }
}

/**
 * Enhanced sync worker that also reschedules reminders
 * 
 * CRITICAL FIX: Must initialize Drive service from GoogleSignIn before syncing
 * because WorkManager creates a fresh TaskRepository instance without Drive initialized.
 */
class EnhancedSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting background sync")
        
        try {
            val repository = TaskRepository(applicationContext)
            
            // CRITICAL FIX: Initialize Drive service from last signed-in account
            // WorkManager creates fresh instances, so driveService would be null
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getLastSignedInAccount(applicationContext)
            
            if (account == null) {
                Log.d(TAG, "No signed-in account found, skipping sync")
                return@withContext Result.success()
            }
            
            // Initialize Drive service with the signed-in account
            repository.initializeDrive(account)
            
            if (!repository.isSignedIn()) {
                Log.d(TAG, "Drive service failed to initialize, skipping sync")
                return@withContext Result.success()
            }
            
            // Load local data first (initialize repository)
            repository.initialize()
            
            // Perform sync
            val result = repository.syncWithDrive()
            Log.d(TAG, "Sync result: $result")
            
            // Update widget after sync
            com.ido.app.widget.TaskWidgetProvider.updateAllWidgets(applicationContext)
            com.ido.app.widget.WidgetTaskService.refreshAllWidgets(applicationContext)
            
            when (result) {
                is SyncResult.Success -> {
                    // Reschedule reminders based on synced data
                    rescheduleRemindersAfterSync(repository)
                    Result.success()
                }
                is SyncResult.Error -> {
                    Log.e(TAG, "Sync error: ${result.message}")
                    Result.retry()
                }
                is SyncResult.NotSignedIn -> Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed", e)
            Result.retry()
        }
    }
    
    private suspend fun rescheduleRemindersAfterSync(repository: TaskRepository) {
        try {
            val reminderScheduler = ReminderScheduler(applicationContext)
            val tasks = repository.getActiveTasks()
            
            for (task in tasks) {
                if (task.reminderTime != null && !task.done && !task.deleted && !task.notified) {
                    val reminderInstant = Instant.parse(task.reminderTime)
                    if (reminderInstant.isAfter(Instant.now())) {
                        reminderScheduler.scheduleReminder(task.id, task.text, reminderInstant)
                    }
                } else {
                    reminderScheduler.cancelReminder(task.id)
                }
            }
            
            Log.d(TAG, "Rescheduled reminders after sync")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule reminders after sync", e)
        }
    }
    
    companion object {
        private const val TAG = "EnhancedSyncWorker"
    }
}
