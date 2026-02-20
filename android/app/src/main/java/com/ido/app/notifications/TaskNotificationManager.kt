package com.ido.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ido.app.MainActivity
import com.ido.app.R

/**
 * Notification manager for task reminders
 * 
 * This class handles the presentation of notifications.
 * Scheduling is handled by ReminderScheduler.
 */
class TaskNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel for reminders (required for Android 8+)
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for task reminders"
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * Show notification immediately
     */
    fun showNotification(taskId: String, taskText: String, isOverdue: Boolean = false) {
        val permissionHandler = NotificationPermissionHandler.getInstance(context)
        if (!permissionHandler.areNotificationsEnabled()) {
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (isOverdue) "⚠️ Task Overdue!" else "🔔 Task Reminder")
            .setContentText(taskText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        notificationManager.notify(taskId.hashCode(), notification)
    }
    
    /**
     * Cancel a specific notification
     */
    fun cancelNotification(taskId: String) {
        notificationManager.cancel(taskId.hashCode())
    }
    
    companion object {
        const val CHANNEL_ID = "task_reminders"
        const val EXTRA_TASK_ID = "task_id"
    }
}
