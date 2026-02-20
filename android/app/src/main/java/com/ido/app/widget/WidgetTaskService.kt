package com.ido.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ido.app.R
import com.ido.app.data.local.LocalDataSource
import com.ido.app.sync.EnhancedSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Service layer for widget operations.
 * Handles task toggling and sync operations from the widget.
 * 
 * CRITICAL: This operates independently of the main app's repository
 * to avoid complex dependency injection in widget context.
 * All changes here update local storage and trigger WorkManager sync.
 */
object WidgetTaskService {
    
    private const val TAG = "WidgetTaskService"
    
    /**
     * Toggle task completion status from widget.
     * 
     * Flow:
     * 1. Load tasks from local storage
     * 2. Toggle the done status
     * 3. Update updatedAt timestamp
     * 4. Save to local storage
     * 5. Trigger sync via WorkManager
     * 6. Refresh widget display
     */
    fun toggleTaskCompletion(context: Context, taskId: String) {
        Log.d(TAG, "toggleTaskCompletion called for taskId=$taskId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localDataSource = LocalDataSource(context)
                val tasks = localDataSource.loadTasks()
                
                val taskIndex = tasks.indexOfFirst { it.id == taskId }
                if (taskIndex != -1) {
                    val task = tasks[taskIndex]
                    val updatedTask = task.copy(
                        done = !task.done,
                        updatedAt = Instant.now().toString()
                    )
                    
                    val updatedTasks = tasks.toMutableList()
                    updatedTasks[taskIndex] = updatedTask
                    
                    localDataSource.saveTasks(updatedTasks)
                    
                    Log.d(TAG, "Toggled task ${task.id}: done=${task.done} -> ${updatedTask.done}")
                    
                    // Trigger sync via WorkManager
                    triggerSync(context)
                    
                    // Refresh all widgets immediately
                    refreshAllWidgets(context)
                } else {
                    Log.w(TAG, "Task not found: $taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle task: $taskId", e)
            }
        }
    }
    
    /**
     * Trigger sync from widget.
     * 
     * This actually triggers the WorkManager sync worker,
     * ensuring data is synced to Google Drive.
     */
    fun syncNow(context: Context) {
        Log.d(TAG, "syncNow called from widget")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update last sync time for all widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TaskWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                for (appWidgetId in appWidgetIds) {
                    TaskWidgetProvider.setLastSyncTime(context, appWidgetId)
                }
                
                // Trigger actual sync via WorkManager
                triggerSync(context)
                
                Log.d(TAG, "Sync triggered from widget")
                
                // Refresh widgets to show updated sync time
                refreshAllWidgets(context)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger sync from widget", e)
            }
        }
    }
    
    /**
     * Trigger sync via WorkManager.
     * Uses EnhancedSyncWorker with REPLACE policy to ensure immediate execution.
     */
    private fun triggerSync(context: Context) {
        try {
            val syncRequest = OneTimeWorkRequestBuilder<EnhancedSyncWorker>()
                .addTag("widget_sync")
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "widget_immediate_sync",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            
            Log.d(TAG, "WorkManager sync enqueued")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue WorkManager sync", e)
        }
    }
    
    /**
     * Refresh all widget displays.
     * Notifies AppWidgetManager that data has changed.
     */
    fun refreshAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TaskWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "No widgets to refresh")
                return
            }
            
            // Notify widgets that data changed (refresh ListView)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list)
            
            // Also trigger full widget update
            TaskWidgetProvider.updateAllWidgets(context)
            
            Log.d(TAG, "Refreshed ${appWidgetIds.size} widgets")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh widgets", e)
        }
    }
}
