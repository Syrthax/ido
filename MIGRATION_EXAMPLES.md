# Migration Examples

## Example 1: Simple Task Migration

### Before (Old Schema)
```json
{
  "tasks": [
    {
      "text": "Buy groceries",
      "done": false
    }
  ]
}
```

### After (New Schema)
```json
{
  "tasks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "text": "Buy groceries",
      "done": false,
      "priority": false,
      "dueDate": null,
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

## Example 2: Task with Due Date and Old ReminderTime

### Before (Old Schema)
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
    }
  ]
}
```

### After (New Schema)
```json
{
  "tasks": [
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "text": "Someone's Birthday Party",
      "done": false,
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

**Changes:**
- ✅ Added unique `id`
- ✅ Converted `reminderTime: 0` → `null`
- ✅ Added `createdAt` and `updatedAt` timestamps
- ✅ Added `deleted: false` flag
- ✅ Preserved all original fields

---

## Example 3: Mixed Format Tasks

### Before (Old Schema)
```json
{
  "tasks": [
    {
      "text": "Old task without ID",
      "done": false,
      "reminderTime": 0
    },
    {
      "text": "Partial task",
      "done": true,
      "priority": true,
      "dueDate": "2025-12-10T04:29:00.000Z"
    },
    {
      "text": "Task with empty reminder",
      "done": false,
      "reminderTime": ""
    }
  ]
}
```

### After (New Schema)
```json
{
  "tasks": [
    {
      "id": "a1b2c3d4-e5f6-4789-a012-3456789abcde",
      "text": "Old task without ID",
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
      "id": "b2c3d4e5-f6a7-4890-b123-456789abcdef",
      "text": "Partial task",
      "done": true,
      "priority": true,
      "dueDate": "2025-12-10T04:29:00.000Z",
      "reminderTime": null,
      "notified": false,
      "createdAt": "2025-12-07T12:00:00.000Z",
      "updatedAt": "2025-12-07T12:00:00.000Z",
      "deleted": false
    },
    {
      "id": "c3d4e5f6-a7b8-4901-c234-56789abcdef0",
      "text": "Task with empty reminder",
      "done": false,
      "priority": false,
      "dueDate": null,
      "reminderTime": null,
      "notified": false,
      "createdAt": "2025-12-07T12:00:00.000Z",
      "updatedAt": "2025-12-07T12:00:00.000Z",
      "deleted": false
    }
  ]
}
```

**Changes:**
- ✅ All tasks now have unique IDs
- ✅ All `reminderTime: 0` and `""` converted to `null`
- ✅ Missing fields added with defaults
- ✅ All tasks now schema-compliant

---

## Example 4: Task with Reminder Time (New Format)

### Creating Task with Reminder
```javascript
// Due date: Dec 10, 2025 at 10:00 AM
const dueDate = new Date("2025-12-10T10:00:00").toISOString();

// Reminder: 15 minutes before due date
const dueDateObj = new Date(dueDate);
const reminderDate = new Date(dueDateObj.getTime() - (15 * 60 * 1000));
const reminderTime = reminderDate.toISOString();

const task = window.TaskSchema.createTask("Call dentist", {
    dueDate: dueDate,
    reminderTime: reminderTime
});
```

### Result
```json
{
  "id": "d4e5f6a7-b8c9-4012-d345-6789abcdef01",
  "text": "Call dentist",
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

---

## Example 5: Soft Delete Workflow

### Step 1: Active Task
```json
{
  "id": "task-123",
  "text": "Buy milk",
  "done": false,
  "priority": false,
  "dueDate": null,
  "reminderTime": null,
  "notified": false,
  "createdAt": "2025-12-07T10:00:00.000Z",
  "updatedAt": "2025-12-07T10:00:00.000Z",
  "deleted": false
}
```

### Step 2: Delete Task
```javascript
const task = window.TaskSchema.findTaskById(tasks, "task-123");
const deleted = window.TaskSchema.deleteTask(task);
```

### Step 3: Deleted Task (Kept for Sync)
```json
{
  "id": "task-123",
  "text": "Buy milk",
  "done": false,
  "priority": false,
  "dueDate": null,
  "reminderTime": null,
  "notified": false,
  "createdAt": "2025-12-07T10:00:00.000Z",
  "updatedAt": "2025-12-07T10:05:00.000Z",
  "deleted": true
}
```

### Step 4: Filter for Display
```javascript
// Only show active tasks
const activeTasks = window.TaskSchema.getActiveTasks(tasks);
// Result: [] (empty, deleted task filtered out)
```

---

## Example 6: Conflict Resolution

### Scenario: Same Task Updated on Two Devices

**Local Device (updated at 10:00):**
```json
{
  "id": "shared-task-1",
  "text": "Buy groceries",
  "done": false,
  "updatedAt": "2025-12-07T10:00:00.000Z"
}
```

**Remote Device (updated at 10:05):**
```json
{
  "id": "shared-task-1",
  "text": "Buy groceries and vegetables",
  "done": true,
  "updatedAt": "2025-12-07T10:05:00.000Z"
}
```

### Merge Result (Last Write Wins)
```javascript
const merged = window.TaskSchema.mergeTaskVersions(local, remote);
```

```json
{
  "id": "shared-task-1",
  "text": "Buy groceries and vegetables",
  "done": true,
  "updatedAt": "2025-12-07T10:05:00.000Z"
}
```

**Winner:** Remote (newer `updatedAt`)

---

## Example 7: Sync Scenario

### Local Tasks
```json
{
  "tasks": [
    {
      "id": "local-1",
      "text": "Local Task 1",
      "updatedAt": "2025-12-07T10:00:00.000Z"
    },
    {
      "id": "shared-1",
      "text": "Shared Task (old version)",
      "updatedAt": "2025-12-07T09:00:00.000Z"
    }
  ]
}
```

### Remote Tasks
```json
{
  "tasks": [
    {
      "id": "remote-1",
      "text": "Remote Task 1",
      "updatedAt": "2025-12-07T10:00:00.000Z"
    },
    {
      "id": "shared-1",
      "text": "Shared Task (new version)",
      "updatedAt": "2025-12-07T10:30:00.000Z"
    }
  ]
}
```

### Merged Result
```javascript
const merged = window.TaskSchema.mergeTasks(localTasks, remoteTasks);
```

```json
{
  "tasks": [
    {
      "id": "local-1",
      "text": "Local Task 1",
      "updatedAt": "2025-12-07T10:00:00.000Z"
    },
    {
      "id": "remote-1",
      "text": "Remote Task 1",
      "updatedAt": "2025-12-07T10:00:00.000Z"
    },
    {
      "id": "shared-1",
      "text": "Shared Task (new version)",
      "updatedAt": "2025-12-07T10:30:00.000Z"
    }
  ]
}
```

**Result:**
- ✅ Local-only task preserved
- ✅ Remote-only task added
- ✅ Shared task uses newer version (remote)

---

## Example 8: Empty Database Migration

### Before (New Install)
```json
{
  "tasks": []
}
```

### After (No Changes Needed)
```json
{
  "tasks": []
}
```

Migration is safe on empty databases.

---

## Example 9: Complete Workflow

### 1. Create Task
```javascript
const task = window.TaskSchema.createTask("Finish project", {
    priority: true,
    dueDate: "2025-12-15T17:00:00.000Z"
});
```

### 2. Add Reminder
```javascript
const reminderDate = new Date(task.dueDate);
reminderDate.setMinutes(reminderDate.getMinutes() - 30);

const updated = window.TaskSchema.updateTask(task, {
    reminderTime: reminderDate.toISOString()
});
```

### 3. Mark Complete
```javascript
const completed = window.TaskSchema.updateTask(updated, {
    done: true
});
```

### 4. Delete
```javascript
const deleted = window.TaskSchema.deleteTask(completed);
```

### Final State
```json
{
  "id": "auto-generated-uuid",
  "text": "Finish project",
  "done": true,
  "priority": true,
  "dueDate": "2025-12-15T17:00:00.000Z",
  "reminderTime": "2025-12-15T16:30:00.000Z",
  "notified": false,
  "createdAt": "2025-12-07T12:00:00.000Z",
  "updatedAt": "2025-12-07T12:10:00.000Z",
  "deleted": true
}
```
