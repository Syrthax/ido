package com.ido.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.ido.app.MainActivity
import com.ido.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * App Widget Provider for iDo task list widget.
 * 
 * Features:
 * - Shows Priority or Today tasks (toggleable)
 * - Circular checkboxes matching app design
 * - Sync button with last sync time
 * - Quick add button to open app
 * - Task completion directly from widget
 */
class TaskWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_TOGGLE_TASK = "com.ido.app.widget.ACTION_TOGGLE_TASK"
        const val ACTION_SYNC = "com.ido.app.widget.ACTION_SYNC"
        const val ACTION_TOGGLE_SECTION = "com.ido.app.widget.ACTION_TOGGLE_SECTION"
        const val EXTRA_TASK_ID = "task_id"
        
        private const val PREFS_NAME = "com.ido.app.widget.TaskWidgetProvider"
        private const val PREF_SECTION_PREFIX = "widget_section_"
        private const val PREF_LAST_SYNC_PREFIX = "widget_last_sync_"
        
        // Section values
        const val SECTION_PRIORITY = 0
        const val SECTION_TODAY = 1
        
        /**
         * Update all widgets from any context
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TaskWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
        
        /**
         * Get the current section for a widget
         */
        fun getSection(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt("$PREF_SECTION_PREFIX$appWidgetId", SECTION_PRIORITY)
        }
        
        /**
         * Set the current section for a widget.
         * Uses commit() instead of apply() to ensure synchronous write,
         * preventing race condition where title reads stale state.
         */
        fun setSection(context: Context, appWidgetId: Int, section: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt("$PREF_SECTION_PREFIX$appWidgetId", section).commit()
        }
        
        /**
         * Set the last sync time for a widget
         */
        fun setLastSyncTime(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong("$PREF_LAST_SYNC_PREFIX$appWidgetId", System.currentTimeMillis()).apply()
        }
        
        /**
         * Get the last sync time formatted string
         */
        fun getLastSyncTimeString(context: Context, appWidgetId: Int): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastSync = prefs.getLong("$PREF_LAST_SYNC_PREFIX$appWidgetId", 0)
            
            if (lastSync == 0L) {
                return "Not synced yet"
            }
            
            val now = System.currentTimeMillis()
            val diff = now - lastSync
            
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000} min ago"
                diff < 86400_000 -> "${diff / 3600_000} hr ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                    sdf.format(Date(lastSync))
                }
            }
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_TOGGLE_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (taskId != null) {
                    // Toggle task completion via WidgetTaskService
                    WidgetTaskService.toggleTaskCompletion(context, taskId)
                }
            }
            ACTION_SYNC -> {
                // Trigger sync via WidgetTaskService
                WidgetTaskService.syncNow(context)
            }
            ACTION_TOGGLE_SECTION -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val currentSection = getSection(context, appWidgetId)
                    val newSection = if (currentSection == SECTION_PRIORITY) SECTION_TODAY else SECTION_PRIORITY
                    
                    // CRITICAL: setSection uses commit() for synchronous write
                    // This ensures the state is available before updateAppWidget reads it
                    setSection(context, appWidgetId, newSection)
                    
                    // Verify the write succeeded (debug)
                    val verifiedSection = getSection(context, appWidgetId)
                    android.util.Log.d("TaskWidgetProvider", 
                        "Section toggled: $currentSection -> $newSection (verified: $verifiedSection)")
                    
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    
                    // CRITICAL FIX: First, immediately update just the title using partiallyUpdateAppWidget
                    // This ensures the title changes are visible immediately without waiting for full update
                    val titleViews = RemoteViews(context.packageName, R.layout.widget_task_list)
                    val newTitle = if (newSection == SECTION_PRIORITY) {
                        context.getString(R.string.section_priority)
                    } else {
                        context.getString(R.string.section_today)
                    }
                    titleViews.setTextViewText(R.id.widget_title, newTitle)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, titleViews)
                    
                    android.util.Log.d("TaskWidgetProvider", 
                        "Partial update sent: title=$newTitle for widgetId=$appWidgetId")
                    
                    // Then do a full update to ensure all state is consistent
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                    
                    // Notify the ListView adapter to reload data
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
                }
            }
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Clean up preferences for deleted widgets
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for (appWidgetId in appWidgetIds) {
            prefs.remove("$PREF_SECTION_PREFIX$appWidgetId")
            prefs.remove("$PREF_LAST_SYNC_PREFIX$appWidgetId")
        }
        prefs.apply()
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val section = getSection(context, appWidgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_task_list)
        
        // Set title based on section - SINGLE SOURCE OF TRUTH
        // The section variable determines BOTH title AND data filter
        val sectionTitle = if (section == SECTION_PRIORITY) {
            context.getString(R.string.section_priority)
        } else {
            context.getString(R.string.section_today)
        }
        views.setTextViewText(R.id.widget_title, sectionTitle)
        
        android.util.Log.d("TaskWidgetProvider", 
            "updateAppWidget: widgetId=$appWidgetId, section=$section, title=$sectionTitle")
        
        // Set last sync time
        val lastSyncStr = "Last sync: ${getLastSyncTimeString(context, appWidgetId)}"
        views.setTextViewText(R.id.widget_last_sync, lastSyncStr)
        
        // Setup RemoteViewsService for list
        // CRITICAL: Include section in the URI to force Android to recreate the adapter
        // when section changes. Without this, Android may cache and reuse the old adapter.
        val listIntent = Intent(context, TaskWidgetRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // Include section and timestamp in data URI to ensure uniqueness
            data = Uri.parse("${context.packageName}://widget/$appWidgetId/section/$section")
        }
        views.setRemoteAdapter(R.id.widget_task_list, listIntent)
        views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_text)
        
        // Setup click intent template for list items
        val clickIntent = Intent(context, TaskWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val clickPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_task_list, clickPendingIntent)
        
        // Setup sync button
        val syncIntent = Intent(context, TaskWidgetProvider::class.java).apply {
            action = ACTION_SYNC
        }
        val syncPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 1000,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_sync_button, syncPendingIntent)
        
        // Setup add button - opens app to create task
        val addIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "create_task")
        }
        val addPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 2000,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)
        
        // Setup title click to toggle section
        val toggleSectionIntent = Intent(context, TaskWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_SECTION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val toggleSectionPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 3000,
            toggleSectionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, toggleSectionPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_logo, toggleSectionPendingIntent)
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
