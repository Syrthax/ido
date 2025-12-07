# iDo Web App - JSON Schema Migration Guide

## Overview

The iDo web app has been upgraded from a basic JSON schema to a robust, sync-compatible schema designed for cross-platform synchronization, offline support, and conflict resolution.

---

## Schema Comparison

### Old Schema (v1.0)

```json
{
  "tasks": [
    {
      "text": "Someone's Birthday Party",
      "done": false,
      "priority": true,
      "dueDate": "2025-12-10T04:29:00.000Z",
      "reminderTime": 0,
      "notified": false
    },
    {
      "text": "hi",
      "done": false
    }
  ]
}
```

**Issues:**
- ❌ No unique identifiers for tasks
- ❌ No timestamps for tracking creation/modification
- ❌ No soft delete support (deletion propagation)
- ❌ Ambiguous `reminderTime: 0` (means "no reminder")
- ❌ Inconsistent field presence across tasks
- ❌ Cannot handle conflict resolution during sync

---

### New Schema (v2.0)

```json
{
  "tasks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "text": "Someone's Birthday Party",
      "done": false,
      "priority": true,
      "dueDate": "2025-12-10T04:29:00.000Z",
      "reminderTime": "2025-12-10T04:24:00.000Z",
      "notified": false,
      "createdAt": "2025-12-07T05:00:00.000Z",
      "updatedAt": "2025-12-07T05:10:00.000Z",
      "deleted": false
    },
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "text": "Buy groceries",
      "done": false,
      "priority": false,
      "dueDate": null,
      "reminderTime": null,
      "notified": false,
      "createdAt": "2025-12-07T05:15:00.000Z",
      "updatedAt": "2025-12-07T05:15:00.000Z",
      "deleted": false
    }
  ]
}
```

**Improvements:**
- ✅ Unique UUID v4 for each task (`id`)
- ✅ Creation and modification timestamps (`createdAt`, `updatedAt`)
- ✅ Soft delete flag (`deleted`)
- ✅ Consistent ISO-8601 timestamps
- ✅ `reminderTime` is now an ISO timestamp or `null` (not `0`)
- ✅ All fields always present
- ✅ Ready for sync and conflict resolution

---

## Field Specifications

### Required Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | string | UUID v4 identifier (unique, immutable) | `"550e8400-e29b-41d4-a716-446655440000"` |
| `text` | string | Task description | `"Buy groceries"` |
| `done` | boolean | Completion status | `false` |
| `priority` | boolean | Priority/pinned status | `true` |
| `dueDate` | string\|null | ISO-8601 due date or null | `"2025-12-10T04:29:00.000Z"` |
| `reminderTime` | string\|null | ISO-8601 reminder time or null | `"2025-12-10T04:24:00.000Z"` |
| `notified` | boolean | Whether reminder was sent | `false` |
| `createdAt` | string | ISO-8601 creation timestamp | `"2025-12-07T05:00:00.000Z"` |
| `updatedAt` | string | ISO-8601 last update timestamp | `"2025-12-07T05:10:00.000Z"` |
| `deleted` | boolean | Soft delete flag | `false` |

### Field Constraints

- **`id`**: Must be a valid UUID v4 (generated using `crypto.randomUUID()`)
- **`text`**: Cannot be empty string
- **`dueDate`**: Must be valid ISO-8601 string or `null` (no `0` or `""`)
- **`reminderTime`**: Must be valid ISO-8601 string or `null` (no `0` or `""`)
- **All timestamps**: Must be in ISO-8601 format (`YYYY-MM-DDTHH:mm:ss.sssZ`)

---

## Migration Process

### Automatic Migration

The web app automatically migrates old-format tasks when loading from Google Drive.

**Migration Logic:**

1. **Detect old format**: Check if tasks are missing `id`, `createdAt`, `updatedAt`, or `deleted`
2. **Generate IDs**: Create UUID v4 for tasks without `id`
3. **Normalize timestamps**: Convert all dates to ISO-8601 format
4. **Convert reminderTime**: Transform `0`, `"0"`, `""`, or `undefined` → `null`
5. **Add metadata**: Set `createdAt` and `updatedAt` to current time if missing
6. **Add deletion flag**: Set `deleted: false` if missing
7. **Save migrated data**: Write updated JSON back to Google Drive

### Migration Code

The migration is handled by `taskSchema.js`:

```javascript
// Called automatically on app load
const migratedData = window.TaskSchema.migrateTasks(data);
```

### Migration is Idempotent

The migration can be run multiple times safely:
- Tasks already in new format are validated and preserved
- Only old-format tasks are migrated
- No data is lost or duplicated

---

## Usage in Application

### Creating Tasks

```javascript
// Use the TaskSchema factory function
const newTask = window.TaskSchema.createTask("Buy milk", {
    priority: false,
    dueDate: "2025-12-10T10:00:00.000Z",
    reminderTime: "2025-12-10T09:45:00.000Z"
});

tasks.push(newTask);
```

**Generated Task:**
```json
{
  "id": "uuid-generated",
  "text": "Buy milk",
  "done": false,
  "priority": false,
  "dueDate": "2025-12-10T10:00:00.000Z",
  "reminderTime": "2025-12-10T09:45:00.000Z",
  "notified": false,
  "createdAt": "2025-12-07T12:00:00.000Z",
  "updatedAt": "2025-12-07T12:00:00.000Z",
  "deleted": false
}
```

### Updating Tasks

```javascript
// Update task and auto-refresh updatedAt
const task = window.TaskSchema.findTaskById(tasks, taskId);
const updatedTask = window.TaskSchema.updateTask(task, {
    done: true
});

// Replace in array
const index = window.TaskSchema.findTaskIndexById(tasks, taskId);
tasks[index] = updatedTask;
```

### Deleting Tasks (Soft Delete)

```javascript
// Mark as deleted (preserves for sync)
const task = window.TaskSchema.findTaskById(tasks, taskId);
const deletedTask = window.TaskSchema.deleteTask(task);

const index = window.TaskSchema.findTaskIndexById(tasks, taskId);
tasks[index] = deletedTask;

// Filter out deleted tasks for display
tasks = window.TaskSchema.getActiveTasks(tasks);
```

### Filtering Tasks

```javascript
// Get active tasks (not deleted)
const activeTasks = window.TaskSchema.getActiveTasks(tasks);

// Get deleted tasks (for cleanup/sync)
const deletedTasks = window.TaskSchema.getDeletedTasks(tasks);
```

---

## Conflict Resolution

### Merge Strategy: Last Write Wins

When syncing between devices, conflicts are resolved using the `updatedAt` timestamp:

```javascript
// Merge local and remote tasks
const mergedTasks = window.TaskSchema.mergeTasks(localTasks, remoteTasks);
```

**Example Conflict:**

**Local Task:**
```json
{
  "id": "abc-123",
  "text": "Buy groceries",
  "done": false,
  "updatedAt": "2025-12-07T10:00:00.000Z"
}
```

**Remote Task:**
```json
{
  "id": "abc-123",
  "text": "Buy groceries and vegetables",
  "done": true,
  "updatedAt": "2025-12-07T10:05:00.000Z"
}
```

**Merged Result (remote wins):**
```json
{
  "id": "abc-123",
  "text": "Buy groceries and vegetables",
  "done": true,
  "updatedAt": "2025-12-07T10:05:00.000Z"
}
```

---

## Schema Validation

Validate tasks before saving:

```javascript
const validation = window.TaskSchema.validateTask(task);

if (!validation.valid) {
    console.error('Invalid task:', validation.errors);
}
```

**Example Validation Errors:**
```javascript
{
  valid: false,
  errors: [
    "Missing or invalid id",
    "Invalid dueDate format (must be ISO-8601 string or null)"
  ]
}
```

---

## Breaking Changes

### For Web App

1. **Task References**: Use `task.id` instead of array indices
2. **Reminder Time**: Now stored as ISO timestamp, not minutes as integer
3. **Deleted Tasks**: Tasks are soft-deleted, not removed from array immediately

### For Android App

The Android app must implement:
1. UUID generation for new tasks
2. ISO-8601 timestamp handling
3. Soft delete with `deleted` flag
4. Conflict resolution using `updatedAt`

---

## API Reference

### `TaskSchema` Functions

#### Core Functions
- `generateUUID()` - Generate UUID v4
- `migrateTasks(data)` - Migrate task collection to v2.0
- `validateTask(task)` - Validate task against schema
- `isOldFormat(task)` - Check if task needs migration

#### Task CRUD
- `createTask(text, options)` - Create new task
- `updateTask(task, updates)` - Update task with new fields
- `deleteTask(task)` - Soft delete task
- `restoreTask(task)` - Restore deleted task

#### Task Queries
- `getActiveTasks(tasks)` - Filter non-deleted tasks
- `getDeletedTasks(tasks)` - Filter deleted tasks
- `findTaskById(tasks, id)` - Find task by ID
- `findTaskIndexById(tasks, id)` - Find task index by ID

#### Sync Helpers
- `mergeTaskVersions(local, remote)` - Merge two task versions
- `mergeTasks(localTasks, remoteTasks)` - Merge task arrays

#### Utilities
- `normalizeISODate(dateValue)` - Normalize date to ISO-8601
- `normalizeReminderTime(reminderTime)` - Convert old reminder format

---

## Testing Migration

### Test Data (Old Format)

```json
{
  "tasks": [
    {
      "text": "Old task with reminderTime 0",
      "done": false,
      "reminderTime": 0
    },
    {
      "text": "Old task without timestamps",
      "done": true,
      "priority": true,
      "dueDate": "2025-12-10T04:29:00.000Z"
    }
  ]
}
```

### Expected Result (Migrated)

```json
{
  "tasks": [
    {
      "id": "generated-uuid-1",
      "text": "Old task with reminderTime 0",
      "done": false,
      "priority": false,
      "dueDate": null,
      "reminderTime": null,
      "notified": false,
      "createdAt": "2025-12-07T12:00:00.000Z",
      "updatedAt": "2025-12-07T12:00:00.000Z",
      "deleted": false
    },
    {
      "id": "generated-uuid-2",
      "text": "Old task without timestamps",
      "done": true,
      "priority": true,
      "dueDate": "2025-12-10T04:29:00.000Z",
      "reminderTime": null,
      "notified": false,
      "createdAt": "2025-12-07T12:00:00.000Z",
      "updatedAt": "2025-12-07T12:00:00.000Z",
      "deleted": false
    }
  ]
}
```

---

## Backward Compatibility

✅ **The web app remains backward compatible:**
- Old JSON files are automatically migrated on load
- No data is lost during migration
- Migration happens transparently to the user
- Migrated data is saved back to Google Drive

---

## Next Steps for Android App

To implement the same schema in Android:

1. **Update Data Model**
   ```kotlin
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

2. **Implement Migration Logic**
   - Convert existing tasks to new schema
   - Handle old `reminderTime` as integer → ISO string

3. **Add Conflict Resolution**
   - Compare `updatedAt` timestamps
   - Implement last-write-wins strategy

4. **Sync with Google Drive**
   - Load tasks from Drive
   - Merge local and remote changes
   - Save merged tasks back

---

## Files Modified

1. **`/web/taskSchema.js`** (NEW) - Schema definition and migration logic
2. **`/web/drive.js`** - Added auto-migration on load
3. **`/web/app.js`** - Updated all CRUD operations to use new schema
4. **`/web/index.html`** - Added taskSchema.js script import

---

## Summary

The new JSON schema provides:
- ✅ Stable task IDs for reliable syncing
- ✅ Timestamps for conflict resolution
- ✅ Soft delete for deletion propagation
- ✅ Consistent ISO-8601 date format
- ✅ Full backward compatibility
- ✅ Ready for offline-first architecture
- ✅ Cross-platform sync support

All existing tasks are automatically migrated with **zero data loss**.
