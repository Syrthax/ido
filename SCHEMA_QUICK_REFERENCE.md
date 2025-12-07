# iDo Task Schema v2.0 - Quick Reference

## Task Object Structure

```javascript
{
  "id": "uuid-v4",                      // Unique identifier
  "text": "Task description",           // Task text
  "done": false,                        // Completion status
  "priority": false,                    // Pin to top
  "dueDate": "ISO-8601 | null",        // Due date/time
  "reminderTime": "ISO-8601 | null",   // Reminder time
  "notified": false,                    // Notification sent?
  "createdAt": "ISO-8601",             // Creation timestamp
  "updatedAt": "ISO-8601",             // Last update timestamp
  "deleted": false                      // Soft delete flag
}
```

---

## Common Operations

### Create Task
```javascript
const task = window.TaskSchema.createTask("Buy milk", {
    priority: false,
    dueDate: "2025-12-10T10:00:00.000Z",
    reminderTime: "2025-12-10T09:45:00.000Z"
});
```

### Update Task
```javascript
const task = window.TaskSchema.findTaskById(tasks, taskId);
const updated = window.TaskSchema.updateTask(task, { done: true });
```

### Delete Task (Soft)
```javascript
const task = window.TaskSchema.findTaskById(tasks, taskId);
const deleted = window.TaskSchema.deleteTask(task);
```

### Find Task
```javascript
// By ID
const task = window.TaskSchema.findTaskById(tasks, "uuid");

// Get index
const index = window.TaskSchema.findTaskIndexById(tasks, "uuid");
```

### Filter Tasks
```javascript
// Active (not deleted)
const active = window.TaskSchema.getActiveTasks(tasks);

// Deleted
const deleted = window.TaskSchema.getDeletedTasks(tasks);
```

---

## Migration

### Automatic (on load)
```javascript
const data = await loadFromDrive();
const migrated = window.TaskSchema.migrateTasks(data);
// Returns: { tasks: [...] }
```

### Check if migration needed
```javascript
const needsMigration = task.id === undefined || 
                       task.createdAt === undefined;
```

---

## Validation

```javascript
const validation = window.TaskSchema.validateTask(task);

if (validation.valid) {
    // Task is valid
} else {
    console.error(validation.errors);
}
```

---

## Sync & Conflict Resolution

### Merge Tasks
```javascript
const merged = window.TaskSchema.mergeTasks(localTasks, remoteTasks);
// Uses last-write-wins based on updatedAt
```

### Merge Individual Task Versions
```javascript
const winner = window.TaskSchema.mergeTaskVersions(localTask, remoteTask);
// Returns task with latest updatedAt
```

---

## Date Handling

### Set Due Date
```javascript
const dueDate = new Date("2025-12-10T10:00:00").toISOString();
// "2025-12-10T10:00:00.000Z"
```

### Set Reminder (15 minutes before)
```javascript
const dueDate = new Date("2025-12-10T10:00:00");
const reminderDate = new Date(dueDate.getTime() - (15 * 60 * 1000));
const reminderTime = reminderDate.toISOString();
```

### Clear Date/Reminder
```javascript
task.dueDate = null;
task.reminderTime = null;
```

---

## Migration Transformations

| Old Format | New Format |
|------------|------------|
| `reminderTime: 0` | `reminderTime: null` |
| `reminderTime: ""` | `reminderTime: null` |
| `reminderTime: undefined` | `reminderTime: null` |
| Missing `id` | `id: "generated-uuid"` |
| Missing `createdAt` | `createdAt: "current-time"` |
| Missing `updatedAt` | `updatedAt: "current-time"` |
| Missing `deleted` | `deleted: false` |

---

## Best Practices

1. **Always use task IDs** - Never rely on array indices
2. **Update timestamps** - Use `updateTask()` to auto-refresh `updatedAt`
3. **Soft delete** - Use `deleteTask()` instead of array splice
4. **Validate before save** - Check schema compliance
5. **Normalize dates** - Always use ISO-8601 format
6. **Filter deleted** - Use `getActiveTasks()` for display

---

## File Structure

```
/web/
├── taskSchema.js    # Schema, migration, CRUD helpers
├── drive.js         # Google Drive sync (auto-migrates on load)
├── app.js           # App logic (uses TaskSchema API)
└── index.html       # Loads taskSchema.js before app.js
```

---

## API Export

All functions are available under `window.TaskSchema`:

```javascript
window.TaskSchema = {
    // Core
    generateUUID,
    migrateTasks,
    validateTask,
    isOldFormat,
    
    // CRUD
    createTask,
    updateTask,
    deleteTask,
    restoreTask,
    
    // Queries
    getActiveTasks,
    getDeletedTasks,
    findTaskById,
    findTaskIndexById,
    
    // Sync
    mergeTaskVersions,
    mergeTasks,
    
    // Utilities
    normalizeISODate,
    normalizeReminderTime
};
```

---

## Example: Complete Task Lifecycle

```javascript
// 1. Create
const task = window.TaskSchema.createTask("Buy groceries", {
    priority: true,
    dueDate: "2025-12-10T18:00:00.000Z"
});
tasks.push(task);

// 2. Update
const index = window.TaskSchema.findTaskIndexById(tasks, task.id);
tasks[index] = window.TaskSchema.updateTask(task, {
    done: true
});

// 3. Delete (soft)
tasks[index] = window.TaskSchema.deleteTask(tasks[index]);

// 4. Filter active
tasks = window.TaskSchema.getActiveTasks(tasks);

// 5. Save
await saveToDrive({ tasks });
```

---

## Error Handling

```javascript
try {
    const task = window.TaskSchema.createTask(text, options);
    const validation = window.TaskSchema.validateTask(task);
    
    if (!validation.valid) {
        throw new Error(validation.errors.join(', '));
    }
    
    tasks.push(task);
    await saveToDrive({ tasks });
} catch (error) {
    console.error('Task operation failed:', error);
}
```

---

## Console Debugging

```javascript
// Check if task is old format
window.TaskSchema.isOldFormat(task);

// Validate task
window.TaskSchema.validateTask(task);

// Get all active tasks
window.TaskSchema.getActiveTasks(tasks);

// Find task
window.TaskSchema.findTaskById(tasks, "uuid");
```
