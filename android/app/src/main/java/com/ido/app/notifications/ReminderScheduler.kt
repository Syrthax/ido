package com.ido.app.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ido.app.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * ReminderScheduler - Core of the two-step notification system
 * 
 * Architecture (CRITICAL):
 * 1. When a task with reminder is created/updated:
 *    - Schedule a PRE-CHECK worker 2 minutes BEFORE reminder time
 *    
 * 2. At pre-check time:
 *    - Sync with Google Drive to get latest task state
 *    - Validate task still needs notification (exists, not deleted, not done, same reminder time)
 *    - If valid: Schedule the final notification
 *    - If invalid: Cancel silently
 *    
 * 3. If reminder time changed during pre-check:
 *    - Cancel current jobs
 *    - Schedule new pre-check for the NEW reminder time
 *    - This is recursive rescheduling (expected behavior)
 * 
 * This guarantees:
 * - No notifications for deleted/completed tasks from other devices
 * - Correct handling of reminder time changes
 * - One active pre-check per task, one notification per task
 */
class ReminderScheduler(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule reminder for a task
     * 
     * This schedules a PRE-CHECK worker, not the notification directly.
     * The pre-check will validate and then schedule the actual notification.
     */
    fun scheduleReminder(taskId: String, taskText: String, reminderTime: Instant) {
        val now = Instant.now()
        val preCheckTime = reminderTime.minus(Duration.ofMinutes(PRE_CHECK_OFFSET_MINUTES))
        
        // If reminder time already passed, don't schedule
        if (reminderTime.isBefore(now)) {
            Log.d(TAG, "Reminder time already passed for task $taskId, not scheduling")
            return
        }
        
        // If pre-check time already passed but reminder hasn't, schedule immediate pre-check
        val delayMillis = if (preCheckTime.isBefore(now)) {
            0L
        } else {
            Duration.between(now, preCheckTime).toMillis()
        }
        
        Log.d(TAG, "Scheduling pre-check for task $taskId in ${delayMillis}ms (reminder at $reminderTime)")
        
        // Cancel any existing work for this task first
        cancelReminder(taskId)
        
        val data = workDataOf(
            KEY_TASK_ID to taskId,
            KEY_TASK_TEXT to taskText,
            KEY_REMINDER_TIME to reminderTime.toString()
        )
        
        // For pre-checks less than 5 minutes away, don't require network
        // This ensures notifications still fire even without connectivity
        val constraints = if (delayMillis < 5 * 60 * 1000L) {
            Constraints.Builder().build() // No constraints for imminent reminders
        } else {
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }
        
        val preCheckWork = OneTimeWorkRequestBuilder<ReminderPreCheckWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(getPreCheckTag(taskId))
            .addTag(TAG_ALL_REMINDERS)
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniqueWork(
            getPreCheckWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            preCheckWork
        )
    }
    
    /**
     * Cancel all reminder-related work for a task
     */
    fun cancelReminder(taskId: String) {
        Log.d(TAG, "Cancelling all reminder work for task $taskId")
        workManager.cancelAllWorkByTag(getPreCheckTag(taskId))
        workManager.cancelAllWorkByTag(getNotificationTag(taskId))
    }
    
    /**
     * Cancel all reminders (used on sign out)
     */
    fun cancelAllReminders() {
        Log.d(TAG, "Cancelling all reminder work")
        workManager.cancelAllWorkByTag(TAG_ALL_REMINDERS)
    }
    
    /**
     * Schedule the actual notification (called by PreCheckWorker after validation)
     */
    internal fun scheduleActualNotification(taskId: String, taskText: String, reminderTime: Instant) {
        val now = Instant.now()
        val delay = Duration.between(now, reminderTime)
        
        if (delay.isNegative) {
            // Reminder time passed during pre-check, fire immediately
            Log.d(TAG, "Reminder time passed during pre-check, firing immediately for task $taskId")
            val notificationManager = TaskNotificationManager(context)
            notificationManager.showNotification(taskId, taskText, false)
            return
        }
        
        Log.d(TAG, "Scheduling actual notification for task $taskId in ${delay.toMillis()}ms")
        
        val data = workDataOf(
            KEY_TASK_ID to taskId,
            KEY_TASK_TEXT to taskText
        )
        
        val notificationWork = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(getNotificationTag(taskId))
            .addTag(TAG_ALL_REMINDERS)
            .build()
        
        workManager.enqueueUniqueWork(
            getNotificationWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )
    }
    
    private fun getPreCheckWorkName(taskId: String) = "reminder_precheck_$taskId"
    private fun getNotificationWorkName(taskId: String) = "reminder_notification_$taskId"
    private fun getPreCheckTag(taskId: String) = "precheck_$taskId"
    private fun getNotificationTag(taskId: String) = "notification_$taskId"
    
    companion object {
        private const val TAG = "ReminderScheduler"
        
        /** Pre-check happens 2 minutes before reminder time */
        const val PRE_CHECK_OFFSET_MINUTES = 2L
        
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TEXT = "task_text"
        const val KEY_REMINDER_TIME = "reminder_time"
        
        const val TAG_ALL_REMINDERS = "all_reminders"
    }
}

/**
 * Pre-check worker - Validates task before firing notification
 * 
 * This worker:
 * 1. Syncs with Google Drive
 * 2. Validates the task state
 * 3. Either schedules notification or cancels
 * 4. Handles recursive rescheduling if reminder time changed
 */
class ReminderPreCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(ReminderScheduler.KEY_TASK_ID) 
            ?: return@withContext Result.failure()
        val originalTaskText = inputData.getString(ReminderScheduler.KEY_TASK_TEXT) 
            ?: return@withContext Result.failure()
        val originalReminderTimeStr = inputData.getString(ReminderScheduler.KEY_REMINDER_TIME) 
            ?: return@withContext Result.failure()
        
        Log.d(TAG, "Pre-check starting for task $taskId")
        
        val repository = TaskRepository(applicationContext)
        val scheduler = ReminderScheduler(applicationContext)
        val permissionHandler = NotificationPermissionHandler.getInstance(applicationContext)
        
        // Check if notifications are enabled
        if (!permissionHandler.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled, skipping reminder for task $taskId")
            return@withContext Result.success()
        }
        
        try {
            // CRITICAL FIX: Initialize the repository to load local tasks first
            // This ensures getTaskById can find the task even if Drive sync fails
            repository.initialize()
            Log.d(TAG, "Repository initialized, loaded ${repository.getActiveTasks().size} tasks")
            
            // Sync with Drive to get latest state
            if (repository.isSignedIn()) {
                val syncResult = repository.syncWithDrive()
                Log.d(TAG, "Pre-check sync result: $syncResult")
            } else {
                Log.d(TAG, "Not signed in, using local data only")
            }
            
            // Get task from repository (now has latest Drive data or local data)
            val task = repository.getTaskById(taskId)
            Log.d(TAG, "Task lookup result: ${task?.let { "found (dueDate=${it.dueDate}, reminderTime=${it.reminderTime})" } ?: "NOT FOUND"}")
            
            // Validation checks
            when {
                task == null -> {
                    Log.d(TAG, "Task $taskId not found, cancelling notification")
                    return@withContext Result.success()
                }
                task.deleted -> {
                    Log.d(TAG, "Task $taskId is deleted, cancelling notification")
                    return@withContext Result.success()
                }
                task.done -> {
                    Log.d(TAG, "Task $taskId is done, cancelling notification")
                    return@withContext Result.success()
                }
                task.reminderTime == null -> {
                    Log.d(TAG, "Task $taskId has no reminder, cancelling notification")
                    return@withContext Result.success()
                }
            }
            
            // Task is valid and non-null at this point
            val validTask = task!!
            
            // Task is valid - check if reminder time matches
            val currentReminderTime = Instant.parse(validTask.reminderTime)
            val originalReminderTime = Instant.parse(originalReminderTimeStr)
            
            if (currentReminderTime != originalReminderTime) {
                // Reminder time changed! Schedule new pre-check for new time
                Log.d(TAG, "Reminder time changed for task $taskId: $originalReminderTime -> $currentReminderTime")
                
                // Only reschedule if new time is in the future
                if (currentReminderTime.isAfter(Instant.now())) {
                    scheduler.scheduleReminder(taskId, validTask.text, currentReminderTime)
                    Log.d(TAG, "Rescheduled pre-check for new reminder time")
                } else {
                    Log.d(TAG, "New reminder time already passed, not rescheduling")
                }
                
                return@withContext Result.success()
            }
            
            // Everything valid - schedule the actual notification
            Log.d(TAG, "Pre-check passed for task $taskId, scheduling notification")
            scheduler.scheduleActualNotification(taskId, validTask.text, currentReminderTime)
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Pre-check failed for task $taskId", e)
            // On failure, still try to schedule notification with original data
            // Better to possibly notify than to silently fail
            try {
                val reminderTime = Instant.parse(originalReminderTimeStr)
                if (reminderTime.isAfter(Instant.now())) {
                    scheduler.scheduleActualNotification(taskId, originalTaskText, reminderTime)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback notification scheduling also failed", e2)
            }
            Result.success() // Don't retry, we've done our best
        }
    }
    
    companion object {
        private const val TAG = "ReminderPreCheckWorker"
    }
}

/**
 * Notification worker - Fires the actual notification
 */
class ReminderNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(ReminderScheduler.KEY_TASK_ID) 
            ?: return@withContext Result.failure()
        val taskText = inputData.getString(ReminderScheduler.KEY_TASK_TEXT) 
            ?: return@withContext Result.failure()
        
        Log.d(TAG, "Firing notification for task $taskId")
        
        val permissionHandler = NotificationPermissionHandler.getInstance(applicationContext)
        
        // Final permission check
        if (!permissionHandler.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled, not showing notification for task $taskId")
            return@withContext Result.success()
        }
        
        val notificationManager = TaskNotificationManager(applicationContext)
        notificationManager.showNotification(taskId, taskText, false)
        
        // Mark task as notified
        try {
            val repository = TaskRepository(applicationContext)
            // CRITICAL FIX: Initialize repository before updating
            repository.initialize()
            repository.updateTask(id = taskId, notified = true)
            Log.d(TAG, "Marked task $taskId as notified")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark task as notified", e)
        }
        
        Result.success()
    }
    
    companion object {
        private const val TAG = "ReminderNotificationWorker"
    }
}
