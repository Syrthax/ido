# iDo Android App - Schema Migration Guide

## Overview

The web app has been upgraded to use a new JSON schema (v2.0) designed for cross-platform sync. The Android app must implement the same schema to ensure compatibility.

---

## New Schema Structure

```kotlin
data class Task(
    val id: String,                  // UUID v4
    val text: String,                // Task description
    val done: Boolean,               // Completion status
    val priority: Boolean,           // Pin to top
    val dueDate: String?,            // ISO-8601 or null
    val reminderTime: String?,       // ISO-8601 or null (NOT minutes!)
    val notified: Boolean,           // Notification sent
    val createdAt: String,           // ISO-8601 creation time
    val updatedAt: String,           // ISO-8601 update time
    val deleted: Boolean             // Soft delete flag
)

data class TaskCollection(
    val tasks: List<Task>
)
```

---

## Key Changes from Old Format

| Old Format | New Format | Notes |
|------------|------------|-------|
| No `id` | `id: String` (UUID v4) | **REQUIRED** - Use `UUID.randomUUID().toString()` |
| `reminderTime: Int` (minutes) | `reminderTime: String?` (ISO-8601) | Store as timestamp, not duration |
| No timestamps | `createdAt`, `updatedAt` | **REQUIRED** - Use `Instant.now().toString()` |
| Hard delete | `deleted: Boolean` | Soft delete for sync propagation |
| Optional fields | All fields always present | Ensures consistency |

---

## Implementation Steps

### 1. Update Data Model

```kotlin
import java.time.Instant
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val done: Boolean = false,
    val priority: Boolean = false,
    val dueDate: String? = null,
    val reminderTime: String? = null,
    val notified: Boolean = false,
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString(),
    val deleted: Boolean = false
)
```

### 2. Create Task Factory

```kotlin
object TaskFactory {
    fun createTask(
        text: String,
        priority: Boolean = false,
        dueDate: String? = null,
        reminderTime: String? = null
    ): Task {
        val now = Instant.now().toString()
        
        return Task(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            done = false,
            priority = priority,
            dueDate = dueDate,
            reminderTime = reminderTime,
            notified = false,
            createdAt = now,
            updatedAt = now,
            deleted = false
        )
    }
    
    fun updateTask(task: Task, updates: TaskUpdate): Task {
        return task.copy(
            text = updates.text ?: task.text,
            done = updates.done ?: task.done,
            priority = updates.priority ?: task.priority,
            dueDate = updates.dueDate ?: task.dueDate,
            reminderTime = updates.reminderTime ?: task.reminderTime,
            notified = updates.notified ?: task.notified,
            updatedAt = Instant.now().toString()
        )
    }
    
    fun deleteTask(task: Task): Task {
        return task.copy(
            deleted = true,
            updatedAt = Instant.now().toString()
        )
    }
}

data class TaskUpdate(
    val text: String? = null,
    val done: Boolean? = null,
    val priority: Boolean? = null,
    val dueDate: String? = null,
    val reminderTime: String? = null,
    val notified: Boolean? = null
)
```

### 3. Implement Migration Logic

```kotlin
object TaskMigration {
    fun migrateTasks(data: TaskCollection): TaskCollection {
        val migratedTasks = data.tasks.map { task ->
            if (needsMigration(task)) {
                migrateTask(task)
            } else {
                task
            }
        }
        
        return TaskCollection(tasks = migratedTasks)
    }
    
    private fun needsMigration(task: Task): Boolean {
        return task.id.isEmpty() || 
               task.createdAt.isEmpty() || 
               task.updatedAt.isEmpty()
    }
    
    private fun migrateTask(task: Task): Task {
        val now = Instant.now().toString()
        
        return Task(
            id = task.id.ifEmpty { UUID.randomUUID().toString() },
            text = task.text,
            done = task.done,
            priority = task.priority,
            dueDate = normalizeDate(task.dueDate),
            reminderTime = normalizeReminderTime(task.reminderTime),
            notified = task.notified,
            createdAt = task.createdAt.ifEmpty { now },
            updatedAt = task.updatedAt.ifEmpty { now },
            deleted = task.deleted
        )
    }
    
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
    
    private fun normalizeReminderTime(reminderTime: String?): String? {
        // Old format stored minutes as Int, convert 0 to null
        return when {
            reminderTime.isNullOrEmpty() -> null
            reminderTime == "0" -> null
            else -> normalizeDate(reminderTime)
        }
    }
}
```

### 4. Implement Conflict Resolution

```kotlin
object SyncManager {
    fun mergeTasks(
        localTasks: List<Task>,
        remoteTasks: List<Task>
    ): List<Task> {
        val taskMap = mutableMapOf<String, Task>()
        
        // Add all local tasks
        localTasks.forEach { task ->
            taskMap[task.id] = task
        }
        
        // Merge remote tasks
        remoteTasks.forEach { remoteTask ->
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
    
    private fun mergeTaskVersions(local: Task, remote: Task): Task {
        val localTime = Instant.parse(local.updatedAt)
        val remoteTime = Instant.parse(remote.updatedAt)
        
        return if (remoteTime.isAfter(localTime)) {
            Log.d("SyncManager", "Using remote version for task ${remote.id}")
            remote
        } else {
            Log.d("SyncManager", "Using local version for task ${local.id}")
            local
        }
    }
}
```

### 5. Update Google Drive Sync

```kotlin
class DriveManager(private val driveService: Drive) {
    suspend fun loadTasks(): List<Task> {
        val fileContent = driveService.files()
            .get(FILE_ID)
            .executeMediaAsInputStream()
            .bufferedReader()
            .use { it.readText() }
        
        val data = Gson().fromJson(fileContent, TaskCollection::class.java)
        
        // Auto-migrate on load
        val migrated = TaskMigration.migrateTasks(data)
        
        // Save if migration occurred
        if (needsMigration(data)) {
            saveTasks(migrated.tasks)
        }
        
        return migrated.tasks
    }
    
    suspend fun saveTasks(tasks: List<Task>) {
        val data = TaskCollection(tasks = tasks)
        val json = Gson().toJson(data)
        
        val content = ByteArrayContent(
            "application/json",
            json.toByteArray()
        )
        
        driveService.files()
            .update(FILE_ID, null, content)
            .execute()
    }
}
```

### 6. Handle Reminders

**OLD WAY (Minutes before due date):**
```kotlin
// ❌ Don't do this anymore
val reminderTime = 15 // minutes
```

**NEW WAY (ISO timestamp):**
```kotlin
// ✅ Store as ISO timestamp
val dueDate = Instant.parse("2025-12-10T10:00:00.000Z")
val reminderMinutes = 15
val reminderTime = dueDate.minusSeconds(reminderMinutes * 60L).toString()

val task = TaskFactory.createTask(
    text = "Call dentist",
    dueDate = dueDate.toString(),
    reminderTime = reminderTime
)
```

**Scheduling Notification:**
```kotlin
fun scheduleNotification(task: Task) {
    if (task.reminderTime == null) return
    
    val reminderInstant = Instant.parse(task.reminderTime)
    val triggerTime = reminderInstant.toEpochMilli()
    
    // Schedule notification at exact time
    val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
        .setInitialDelay(
            triggerTime - System.currentTimeMillis(),
            TimeUnit.MILLISECONDS
        )
        .setInputData(workDataOf("taskId" to task.id))
        .build()
    
    WorkManager.getInstance(context).enqueue(workRequest)
}
```

---

## Gson Configuration

```kotlin
val gson = GsonBuilder()
    .registerTypeAdapter(Task::class.java, TaskDeserializer())
    .create()

class TaskDeserializer : JsonDeserializer<Task> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Task {
        val jsonObject = json.asJsonObject
        val now = Instant.now().toString()
        
        return Task(
            id = jsonObject.get("id")?.asString ?: UUID.randomUUID().toString(),
            text = jsonObject.get("text")?.asString ?: "",
            done = jsonObject.get("done")?.asBoolean ?: false,
            priority = jsonObject.get("priority")?.asBoolean ?: false,
            dueDate = jsonObject.get("dueDate")?.takeUnless { it.isJsonNull }?.asString,
            reminderTime = normalizeReminderTime(jsonObject.get("reminderTime")),
            notified = jsonObject.get("notified")?.asBoolean ?: false,
            createdAt = jsonObject.get("createdAt")?.asString ?: now,
            updatedAt = jsonObject.get("updatedAt")?.asString ?: now,
            deleted = jsonObject.get("deleted")?.asBoolean ?: false
        )
    }
    
    private fun normalizeReminderTime(element: JsonElement?): String? {
        return when {
            element == null || element.isJsonNull -> null
            element.isJsonPrimitive && element.asString == "0" -> null
            element.isJsonPrimitive && element.asString.isEmpty() -> null
            else -> element.asString
        }
    }
}
```

---

## Testing Checklist

- [ ] Create new task with UUID
- [ ] Update task with new `updatedAt`
- [ ] Soft delete task (set `deleted = true`)
- [ ] Filter out deleted tasks for display
- [ ] Migrate old JSON format
- [ ] Load tasks from Google Drive
- [ ] Save tasks to Google Drive
- [ ] Handle conflict resolution (last write wins)
- [ ] Schedule notifications using ISO timestamp
- [ ] Validate task schema before saving

---

## Example Usage

```kotlin
// Create task
val task = TaskFactory.createTask(
    text = "Buy groceries",
    priority = true,
    dueDate = Instant.now().plusSeconds(3600).toString()
)

// Update task
val updated = TaskFactory.updateTask(task, TaskUpdate(done = true))

// Delete task
val deleted = TaskFactory.deleteTask(updated)

// Filter active tasks
val activeTasks = tasks.filter { !it.deleted }

// Find by ID
val found = tasks.find { it.id == taskId }

// Sync
val merged = SyncManager.mergeTasks(localTasks, remoteTasks)
```

---

## Common Pitfalls

1. **Don't use array indices** - Always use task `id`
2. **Don't store reminderTime as Int** - Use ISO-8601 string
3. **Don't hard delete** - Use soft delete (`deleted = true`)
4. **Always update `updatedAt`** - Required for conflict resolution
5. **Don't forget migration** - Auto-migrate on first load

---

## Resources

- Web implementation: `/web/taskSchema.js`
- Migration guide: `/SCHEMA_MIGRATION_GUIDE.md`
- Examples: `/MIGRATION_EXAMPLES.md`
- Quick reference: `/SCHEMA_QUICK_REFERENCE.md`

---

## Support

For questions or issues, refer to the web implementation as the reference. The JavaScript code in `taskSchema.js` is the authoritative source for migration logic and schema validation.
