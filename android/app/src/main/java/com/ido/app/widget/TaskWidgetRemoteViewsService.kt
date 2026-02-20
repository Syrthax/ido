package com.ido.app.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.appwidget.AppWidgetManager
import com.ido.app.R
import com.ido.app.data.local.LocalDataSource
import com.ido.app.data.model.Task
import com.ido.app.data.model.activeTasks
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * RemoteViewsService for the task list widget.
 * Provides the data for the ListView in the widget.
 */
class TaskWidgetRemoteViewsService : RemoteViewsService() {
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskWidgetRemoteViewsFactory(applicationContext, intent)
    }
}

/**
 * Factory that creates RemoteViews for each task item in the widget.
 */
class TaskWidgetRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    
    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    
    private var tasks: List<Task> = emptyList()
    private val localDataSource = LocalDataSource(context)
    
    override fun onCreate() {
        // Initial load
        loadTasks()
    }
    
    override fun onDataSetChanged() {
        // Reload tasks when data changes
        loadTasks()
    }
    
    private fun loadTasks() {
        // Load tasks from local storage synchronously
        tasks = runBlocking {
            try {
                val allTasks = localDataSource.loadTasks().activeTasks()
                val section = TaskWidgetProvider.getSection(context, appWidgetId)
                
                android.util.Log.d("TaskWidgetFactory", 
                    "loadTasks: widgetId=$appWidgetId, section=$section, totalTasks=${allTasks.size}")
                
                // Filter by section and exclude completed tasks
                val filteredTasks = allTasks.filter { task ->
                    !task.done && when (section) {
                        TaskWidgetProvider.SECTION_PRIORITY -> task.priority
                        TaskWidgetProvider.SECTION_TODAY -> {
                            // Show non-priority tasks that are due today or have no due date
                            !task.priority && isTaskDueToday(task)
                        }
                        else -> true
                    }
                }
                
                android.util.Log.d("TaskWidgetFactory", 
                    "loadTasks: filteredTasks=${filteredTasks.size} for section=$section")
                
                // Sort: priority first, then by due date
                filteredTasks.sortedWith(
                    compareByDescending<Task> { it.priority }
                        .thenBy { it.getDueDateInstant() ?: Instant.MAX }
                )
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    private fun isTaskDueToday(task: Task): Boolean {
        val dueInstant = task.getDueDateInstant() ?: return true // No due date = show in Today
        val dueDate = dueInstant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return dueDate <= today
    }
    
    override fun onDestroy() {
        tasks = emptyList()
    }
    
    override fun getCount(): Int = tasks.size
    
    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= tasks.size) {
            return RemoteViews(context.packageName, R.layout.widget_task_item)
        }
        
        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        
        // Set task text
        views.setTextViewText(R.id.widget_task_text, task.text)
        
        // Set checkbox state
        if (task.done) {
            views.setViewVisibility(R.id.widget_checkbox_unchecked, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_checkbox_checked, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_checkbox_unchecked, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_checkbox_checked, android.view.View.GONE)
        }
        
        // Set priority star visibility
        views.setViewVisibility(
            R.id.widget_task_priority,
            if (task.priority) android.view.View.VISIBLE else android.view.View.GONE
        )
        
        // Set fill-in intent for clicking the task
        val fillInIntent = Intent().apply {
            putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_task_item, fillInIntent)
        
        return views
    }
    
    override fun getLoadingView(): RemoteViews? = null
    
    override fun getViewTypeCount(): Int = 1
    
    override fun getItemId(position: Int): Long {
        return if (position < tasks.size) {
            tasks[position].id.hashCode().toLong()
        } else {
            position.toLong()
        }
    }
    
    override fun hasStableIds(): Boolean = true
}
