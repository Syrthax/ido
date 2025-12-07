package com.ido.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.ido.app.R
import com.ido.app.data.repository.TaskRepository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Notification manager for task reminders
 * Uses WorkManager to schedule notifications
 */
class TaskNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val workManager = WorkManager.getInstance(context)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel for reminders
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for task reminders"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * Schedule notification for task reminder
     */
    fun scheduleNotification(taskId: String, taskText: String, reminderTime: Instant) {
        val now = Instant.now()
        val delay = Duration.between(now, reminderTime)
        
        if (delay.isNegative) {
            // Reminder time has passed, don't schedule
            return
        }
        
        val data = workDataOf(
            KEY_TASK_ID to taskId,
            KEY_TASK_TEXT to taskText
        )
        
        val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(getWorkTag(taskId))
            .build()
        
        workManager.enqueueUniqueWork(
            getWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )
    }
    
    /**
     * Cancel scheduled notification for task
     */
    fun cancelNotification(taskId: String) {
        workManager.cancelAllWorkByTag(getWorkTag(taskId))
    }
    
    /**
     * Show notification immediately
     */
    fun showNotification(taskId: String, taskText: String, isOverdue: Boolean = false) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isOverdue) "⚠️ Task Overdue!" else "🔔 Task Reminder")
            .setContentText(taskText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(taskId.hashCode(), notification)
    }
    
    private fun getWorkName(taskId: String) = "task_notification_$taskId"
    private fun getWorkTag(taskId: String) = "task_reminder_$taskId"
    
    companion object {
        private const val CHANNEL_ID = "task_reminders"
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TEXT = "task_text"
    }
}

/**
 * Worker to show notification at scheduled time
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        val taskId = inputData.getString(TaskNotificationManager.KEY_TASK_ID) ?: return Result.failure()
        val taskText = inputData.getString(TaskNotificationManager.KEY_TASK_TEXT) ?: return Result.failure()
        
        val notificationManager = TaskNotificationManager(applicationContext)
        notificationManager.showNotification(taskId, taskText)
        
        // Mark task as notified
        // This would need to be done through repository
        
        return Result.success()
    }
}
