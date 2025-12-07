/* ===================================================
   iDo - TASK SCHEMA & MIGRATION
   JSON Schema Version 2.0 - Enhanced for Sync & Offline
   =================================================== */

/* ===================================================
   SCHEMA DEFINITION
   =================================================== */

/**
 * New Task Schema (v2.0)
 * 
 * @typedef {Object} Task
 * @property {string} id - UUID v4 identifier (required)
 * @property {string} text - Task description (required)
 * @property {boolean} done - Completion status (default: false)
 * @property {boolean} priority - Priority/pinned status (default: false)
 * @property {string|null} dueDate - ISO-8601 timestamp or null
 * @property {string|null} reminderTime - ISO-8601 timestamp or null
 * @property {boolean} notified - Whether reminder notification was sent (default: false)
 * @property {string} createdAt - ISO-8601 creation timestamp (required)
 * @property {string} updatedAt - ISO-8601 last update timestamp (required)
 * @property {boolean} deleted - Soft delete flag (default: false)
 */

/**
 * Task Collection Schema
 * 
 * @typedef {Object} TaskCollection
 * @property {Task[]} tasks - Array of tasks
 */

/* ===================================================
   UUID GENERATION
   =================================================== */

/**
 * Generate a UUID v4
 * Uses crypto.randomUUID() if available, falls back to polyfill
 */
function generateUUID() {
    // Use native crypto.randomUUID() if available
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID();
    }
    
    // Fallback polyfill for older browsers
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/* ===================================================
   SCHEMA VALIDATION
   =================================================== */

/**
 * Check if a task follows the old schema format
 */
function isOldFormat(task) {
    // Old format tasks are missing one or more new required fields
    return !task.id || 
           !task.createdAt || 
           !task.updatedAt || 
           task.deleted === undefined;
}

/**
 * Validate a task object against the new schema
 */
function validateTask(task) {
    const errors = [];
    
    // Required fields
    if (!task.id || typeof task.id !== 'string') {
        errors.push('Missing or invalid id');
    }
    if (!task.text || typeof task.text !== 'string') {
        errors.push('Missing or invalid text');
    }
    if (typeof task.done !== 'boolean') {
        errors.push('Missing or invalid done status');
    }
    if (!task.createdAt || typeof task.createdAt !== 'string') {
        errors.push('Missing or invalid createdAt timestamp');
    }
    if (!task.updatedAt || typeof task.updatedAt !== 'string') {
        errors.push('Missing or invalid updatedAt timestamp');
    }
    
    // Optional fields validation
    if (task.dueDate !== null && task.dueDate !== undefined) {
        if (typeof task.dueDate !== 'string') {
            errors.push('Invalid dueDate format (must be ISO-8601 string or null)');
        }
    }
    if (task.reminderTime !== null && task.reminderTime !== undefined) {
        if (typeof task.reminderTime !== 'string') {
            errors.push('Invalid reminderTime format (must be ISO-8601 string or null)');
        }
    }
    
    return {
        valid: errors.length === 0,
        errors
    };
}

/* ===================================================
   TASK MIGRATION
   =================================================== */

/**
 * Migrate a single task from old format to new format
 * 
 * Old format example:
 * {
 *   "text": "Task",
 *   "done": false,
 *   "priority": true,
 *   "dueDate": "2025-12-10T04:29:00.000Z",
 *   "reminderTime": 0,
 *   "notified": false
 * }
 * 
 * New format example:
 * {
 *   "id": "uuid-v4",
 *   "text": "Task",
 *   "done": false,
 *   "priority": false,
 *   "dueDate": "2025-12-10T04:29:00.000Z",
 *   "reminderTime": null,
 *   "notified": false,
 *   "createdAt": "2025-12-07T05:00:00.000Z",
 *   "updatedAt": "2025-12-07T05:00:00.000Z",
 *   "deleted": false
 * }
 */
function migrateTask(task) {
    const now = new Date().toISOString();
    
    // Create migrated task with all required fields
    const migratedTask = {
        // Generate ID if missing
        id: task.id || generateUUID(),
        
        // Preserve existing fields
        text: task.text || '',
        done: task.done === true, // Ensure boolean
        priority: task.priority === true, // Ensure boolean
        
        // Normalize dueDate
        dueDate: task.dueDate && task.dueDate !== '' ? 
                 normalizeISODate(task.dueDate) : null,
        
        // Convert reminderTime: 0 → null, normalize ISO strings
        reminderTime: normalizeReminderTime(task.reminderTime),
        
        // Preserve notification status
        notified: task.notified === true, // Ensure boolean
        
        // Add timestamps
        createdAt: task.createdAt ? normalizeISODate(task.createdAt) : now,
        updatedAt: task.updatedAt ? normalizeISODate(task.updatedAt) : now,
        
        // Add deletion flag
        deleted: task.deleted === true // Ensure boolean
    };
    
    return migratedTask;
}

/**
 * Normalize reminder time value
 * - Converts 0 → null
 * - Converts empty string → null
 * - Normalizes ISO date strings
 */
function normalizeReminderTime(reminderTime) {
    if (reminderTime === 0 || 
        reminderTime === '0' || 
        reminderTime === '' || 
        reminderTime === undefined || 
        reminderTime === null) {
        return null;
    }
    return normalizeISODate(reminderTime);
}

/**
 * Normalize date to ISO-8601 format
 * Ensures consistent format across all timestamps
 */
function normalizeISODate(dateValue) {
    if (!dateValue) return null;
    
    try {
        const date = new Date(dateValue);
        if (isNaN(date.getTime())) {
            console.warn('Invalid date value:', dateValue);
            return null;
        }
        return date.toISOString();
    } catch (error) {
        console.error('Error normalizing date:', error);
        return null;
    }
}

/**
 * Migrate entire task collection
 * 
 * @param {Object} data - Task collection object with { tasks: [] }
 * @returns {Object} Migrated task collection
 */
function migrateTasks(data) {
    // Ensure data has tasks array
    if (!data || !Array.isArray(data.tasks)) {
        console.warn('Invalid task data, initializing empty array');
        return { tasks: [] };
    }
    
    let migratedCount = 0;
    let validCount = 0;
    
    const migratedTasks = data.tasks.map(task => {
        if (isOldFormat(task)) {
            migratedCount++;
            console.log('Migrating task:', task.text || '(no text)');
            return migrateTask(task);
        } else {
            validCount++;
            // Already in new format, but ensure all fields are present
            return migrateTask(task); // This ensures consistency
        }
    });
    
    console.log(`Migration complete: ${migratedCount} tasks migrated, ${validCount} tasks already valid`);
    
    return {
        tasks: migratedTasks
    };
}

/* ===================================================
   TASK FACTORY & CRUD HELPERS
   =================================================== */

/**
 * Create a new task with full schema compliance
 * 
 * @param {string} text - Task description
 * @param {Object} options - Optional task properties
 * @returns {Task} New task object
 */
function createTask(text, options = {}) {
    const now = new Date().toISOString();
    
    return {
        id: generateUUID(),
        text: text.trim(),
        done: options.done === true,
        priority: options.priority === true,
        dueDate: options.dueDate ? normalizeISODate(options.dueDate) : null,
        reminderTime: options.reminderTime ? normalizeISODate(options.reminderTime) : null,
        notified: false,
        createdAt: now,
        updatedAt: now,
        deleted: false
    };
}

/**
 * Update a task and refresh updatedAt timestamp
 * 
 * @param {Task} task - Task to update
 * @param {Object} updates - Fields to update
 * @returns {Task} Updated task object
 */
function updateTask(task, updates) {
    const updatedTask = {
        ...task,
        ...updates,
        updatedAt: new Date().toISOString()
    };
    
    // Ensure dates are normalized
    if (updates.dueDate !== undefined) {
        updatedTask.dueDate = updates.dueDate ? normalizeISODate(updates.dueDate) : null;
    }
    if (updates.reminderTime !== undefined) {
        updatedTask.reminderTime = updates.reminderTime ? normalizeISODate(updates.reminderTime) : null;
    }
    
    return updatedTask;
}

/**
 * Mark a task as deleted (soft delete)
 * 
 * @param {Task} task - Task to delete
 * @returns {Task} Deleted task object
 */
function deleteTask(task) {
    return updateTask(task, { deleted: true });
}

/**
 * Restore a soft-deleted task
 * 
 * @param {Task} task - Task to restore
 * @returns {Task} Restored task object
 */
function restoreTask(task) {
    return updateTask(task, { deleted: false });
}

/**
 * Filter out deleted tasks (for display)
 * 
 * @param {Task[]} tasks - Array of tasks
 * @returns {Task[]} Non-deleted tasks
 */
function getActiveTasks(tasks) {
    return tasks.filter(task => !task.deleted);
}

/**
 * Filter deleted tasks (for sync/cleanup)
 * 
 * @param {Task[]} tasks - Array of tasks
 * @returns {Task[]} Deleted tasks
 */
function getDeletedTasks(tasks) {
    return tasks.filter(task => task.deleted);
}

/**
 * Find task by ID
 * 
 * @param {Task[]} tasks - Array of tasks
 * @param {string} id - Task ID
 * @returns {Task|null} Found task or null
 */
function findTaskById(tasks, id) {
    return tasks.find(task => task.id === id) || null;
}

/**
 * Find task index by ID
 * 
 * @param {Task[]} tasks - Array of tasks
 * @param {string} id - Task ID
 * @returns {number} Task index or -1
 */
function findTaskIndexById(tasks, id) {
    return tasks.findIndex(task => task.id === id);
}

/* ===================================================
   CONFLICT RESOLUTION HELPERS
   =================================================== */

/**
 * Merge two task versions (for conflict resolution)
 * Uses "last write wins" strategy based on updatedAt timestamp
 * 
 * @param {Task} local - Local task version
 * @param {Task} remote - Remote task version
 * @returns {Task} Merged task
 */
function mergeTaskVersions(local, remote) {
    const localTime = new Date(local.updatedAt).getTime();
    const remoteTime = new Date(remote.updatedAt).getTime();
    
    // Last write wins
    if (remoteTime > localTime) {
        console.log(`Conflict resolved: Using remote version for task ${remote.id}`);
        return remote;
    } else {
        console.log(`Conflict resolved: Using local version for task ${local.id}`);
        return local;
    }
}

/**
 * Merge two task collections (for sync)
 * 
 * @param {Task[]} localTasks - Local tasks
 * @param {Task[]} remoteTasks - Remote tasks
 * @returns {Task[]} Merged task array
 */
function mergeTasks(localTasks, remoteTasks) {
    const merged = new Map();
    
    // Add all local tasks
    localTasks.forEach(task => {
        merged.set(task.id, task);
    });
    
    // Merge remote tasks
    remoteTasks.forEach(remoteTask => {
        const localTask = merged.get(remoteTask.id);
        
        if (localTask) {
            // Conflict - merge versions
            merged.set(remoteTask.id, mergeTaskVersions(localTask, remoteTask));
        } else {
            // New remote task
            merged.set(remoteTask.id, remoteTask);
        }
    });
    
    return Array.from(merged.values());
}

/* ===================================================
   EXPORTS
   =================================================== */

// Export all functions for use in other modules
window.TaskSchema = {
    // Core functions
    generateUUID,
    migrateTasks,
    validateTask,
    isOldFormat,
    
    // Task CRUD
    createTask,
    updateTask,
    deleteTask,
    restoreTask,
    
    // Task queries
    getActiveTasks,
    getDeletedTasks,
    findTaskById,
    findTaskIndexById,
    
    // Sync helpers
    mergeTaskVersions,
    mergeTasks,
    
    // Utilities
    normalizeISODate,
    normalizeReminderTime
};
