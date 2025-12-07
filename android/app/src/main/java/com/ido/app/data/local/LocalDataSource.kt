package com.ido.app.data.local

import android.content.Context
import com.ido.app.data.model.Task
import com.ido.app.data.model.TaskCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Local JSON file manager for tasks
 * Handles reading, writing, and migration of tasks.json
 */
class LocalDataSource(private val context: Context) {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val tasksFile: File
        get() = File(context.filesDir, TASKS_FILE_NAME)
    
    /**
     * Load tasks from local JSON file
     * Returns empty list if file doesn't exist or is corrupted
     */
    suspend fun loadTasks(): List<Task> = withContext(Dispatchers.IO) {
        try {
            if (!tasksFile.exists()) {
                return@withContext emptyList()
            }
            
            val jsonString = tasksFile.readText()
            val collection = json.decodeFromString<TaskCollection>(jsonString)
            
            // Migrate if needed
            val migrated = migrateTasks(collection.tasks)
            
            // Save migrated data if migration occurred
            if (needsMigration(collection.tasks)) {
                saveTasks(migrated)
            }
            
            migrated
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Save tasks to local JSON file
     */
    suspend fun saveTasks(tasks: List<Task>): Boolean = withContext(Dispatchers.IO) {
        try {
            val collection = TaskCollection(tasks)
            val jsonString = json.encodeToString(collection)
            tasksFile.writeText(jsonString)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Check if tasks need migration
     */
    private fun needsMigration(tasks: List<Task>): Boolean {
        return tasks.any { task ->
            task.id.isEmpty() || 
            task.createdAt.isEmpty() || 
            task.updatedAt.isEmpty()
        }
    }
    
    /**
     * Migrate tasks from old format to new schema
     * This ensures compatibility with web app data
     */
    private fun migrateTasks(tasks: List<Task>): List<Task> {
        return tasks.map { task ->
            if (task.id.isEmpty() || task.createdAt.isEmpty() || task.updatedAt.isEmpty()) {
                migrateTask(task)
            } else {
                task
            }
        }
    }
    
    /**
     * Migrate a single task to new schema
     */
    private fun migrateTask(task: Task): Task {
        val now = Instant.now().toString()
        
        return task.copy(
            id = task.id.ifEmpty { UUID.randomUUID().toString() },
            dueDate = normalizeDate(task.dueDate),
            reminderTime = normalizeReminderTime(task.reminderTime),
            createdAt = task.createdAt.ifEmpty { now },
            updatedAt = task.updatedAt.ifEmpty { now },
            deleted = task.deleted // Preserve deletion flag
        )
    }
    
    /**
     * Normalize date to ISO-8601 format or null
     */
    private fun normalizeDate(date: String?): String? {
        return when {
            date.isNullOrEmpty() -> null
            else -> {
                try {
                    Instant.parse(date).toString()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    /**
     * Normalize reminder time (convert old format 0/"0" to null)
     */
    private fun normalizeReminderTime(reminderTime: String?): String? {
        return when {
            reminderTime.isNullOrEmpty() -> null
            reminderTime == "0" -> null
            else -> normalizeDate(reminderTime)
        }
    }
    
    /**
     * Clear all local data
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (tasksFile.exists()) {
                tasksFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Export tasks as JSON string
     */
    suspend fun exportTasksJson(): String = withContext(Dispatchers.IO) {
        try {
            val tasks = loadTasks()
            val collection = TaskCollection(tasks)
            json.encodeToString(collection)
        } catch (e: Exception) {
            "{\"tasks\":[]}"
        }
    }
    
    companion object {
        private const val TASKS_FILE_NAME = "tasks.json"
    }
}
