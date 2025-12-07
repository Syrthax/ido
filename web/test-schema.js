/* ===================================================
   iDo - SCHEMA MIGRATION TESTS
   Test suite for validating schema migration
   =================================================== */

console.log('🧪 Starting Schema Migration Tests...\n');

/* ===================================================
   TEST UTILITIES
   =================================================== */

let testsPassed = 0;
let testsFailed = 0;

function assert(condition, testName) {
    if (condition) {
        console.log(`✅ PASS: ${testName}`);
        testsPassed++;
    } else {
        console.error(`❌ FAIL: ${testName}`);
        testsFailed++;
    }
}

function assertEqual(actual, expected, testName) {
    if (JSON.stringify(actual) === JSON.stringify(expected)) {
        console.log(`✅ PASS: ${testName}`);
        testsPassed++;
    } else {
        console.error(`❌ FAIL: ${testName}`);
        console.error('  Expected:', expected);
        console.error('  Actual:', actual);
        testsFailed++;
    }
}

/* ===================================================
   TEST 1: UUID GENERATION
   =================================================== */

console.log('\n--- Test 1: UUID Generation ---');

const uuid1 = window.TaskSchema.generateUUID();
const uuid2 = window.TaskSchema.generateUUID();

assert(uuid1 !== uuid2, 'UUIDs should be unique');
assert(uuid1.length === 36, 'UUID should be 36 characters');
assert(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(uuid1), 
       'UUID should match v4 format');

/* ===================================================
   TEST 2: OLD FORMAT DETECTION
   =================================================== */

console.log('\n--- Test 2: Old Format Detection ---');

const oldTask1 = {
    text: "Old task",
    done: false,
    reminderTime: 0
};

const oldTask2 = {
    text: "Another old task",
    done: true,
    priority: true,
    dueDate: "2025-12-10T04:29:00.000Z"
};

const newTask = {
    id: "abc-123",
    text: "New task",
    done: false,
    priority: false,
    dueDate: null,
    reminderTime: null,
    notified: false,
    createdAt: "2025-12-07T05:00:00.000Z",
    updatedAt: "2025-12-07T05:00:00.000Z",
    deleted: false
};

assert(window.TaskSchema.isOldFormat(oldTask1), 'Should detect old task 1');
assert(window.TaskSchema.isOldFormat(oldTask2), 'Should detect old task 2');
assert(!window.TaskSchema.isOldFormat(newTask), 'Should not detect new task as old');

/* ===================================================
   TEST 3: TASK MIGRATION
   =================================================== */

console.log('\n--- Test 3: Task Migration ---');

const oldData = {
    tasks: [
        {
            text: "Task with reminderTime 0",
            done: false,
            reminderTime: 0
        },
        {
            text: "Task without timestamps",
            done: true,
            priority: true,
            dueDate: "2025-12-10T04:29:00.000Z",
            notified: false
        }
    ]
};

const migratedData = window.TaskSchema.migrateTasks(oldData);

assert(migratedData.tasks.length === 2, 'Should preserve task count');
assert(migratedData.tasks[0].id !== undefined, 'Task 1 should have ID');
assert(migratedData.tasks[1].id !== undefined, 'Task 2 should have ID');
assert(migratedData.tasks[0].createdAt !== undefined, 'Task 1 should have createdAt');
assert(migratedData.tasks[0].updatedAt !== undefined, 'Task 1 should have updatedAt');
assert(migratedData.tasks[0].deleted === false, 'Task 1 should have deleted flag');
assert(migratedData.tasks[0].reminderTime === null, 'Task 1 reminderTime should be null (was 0)');
assert(migratedData.tasks[1].dueDate === "2025-12-10T04:29:00.000Z", 'Task 2 should preserve dueDate');

/* ===================================================
   TEST 4: CREATE TASK
   =================================================== */

console.log('\n--- Test 4: Create Task ---');

const newCreatedTask = window.TaskSchema.createTask("Buy milk", {
    priority: true,
    dueDate: "2025-12-10T10:00:00.000Z",
    reminderTime: "2025-12-10T09:45:00.000Z"
});

assert(newCreatedTask.id !== undefined, 'Created task should have ID');
assert(newCreatedTask.text === "Buy milk", 'Created task should have correct text');
assert(newCreatedTask.priority === true, 'Created task should have priority');
assert(newCreatedTask.done === false, 'Created task should be not done');
assert(newCreatedTask.dueDate === "2025-12-10T10:00:00.000Z", 'Created task should have dueDate');
assert(newCreatedTask.reminderTime === "2025-12-10T09:45:00.000Z", 'Created task should have reminderTime');
assert(newCreatedTask.notified === false, 'Created task should not be notified');
assert(newCreatedTask.deleted === false, 'Created task should not be deleted');
assert(newCreatedTask.createdAt !== undefined, 'Created task should have createdAt');
assert(newCreatedTask.updatedAt !== undefined, 'Created task should have updatedAt');

/* ===================================================
   TEST 5: UPDATE TASK
   =================================================== */

console.log('\n--- Test 5: Update Task ---');

const taskToUpdate = window.TaskSchema.createTask("Original text");
const originalUpdatedAt = taskToUpdate.updatedAt;

// Wait a tiny bit to ensure timestamp changes
setTimeout(() => {
    const updatedTask = window.TaskSchema.updateTask(taskToUpdate, {
        text: "Updated text",
        done: true
    });
    
    assert(updatedTask.text === "Updated text", 'Updated task should have new text');
    assert(updatedTask.done === true, 'Updated task should be done');
    assert(updatedTask.id === taskToUpdate.id, 'Updated task should preserve ID');
    assert(updatedTask.updatedAt !== originalUpdatedAt, 'Updated task should have new updatedAt');
    
    console.log('✅ Update task test completed');
}, 10);

/* ===================================================
   TEST 6: DELETE TASK (SOFT)
   =================================================== */

console.log('\n--- Test 6: Delete Task ---');

const taskToDelete = window.TaskSchema.createTask("To be deleted");
const deletedTask = window.TaskSchema.deleteTask(taskToDelete);

assert(deletedTask.deleted === true, 'Deleted task should have deleted flag');
assert(deletedTask.id === taskToDelete.id, 'Deleted task should preserve ID');
assert(deletedTask.text === taskToDelete.text, 'Deleted task should preserve text');

/* ===================================================
   TEST 7: FILTER TASKS
   =================================================== */

console.log('\n--- Test 7: Filter Tasks ---');

const mixedTasks = [
    window.TaskSchema.createTask("Active 1"),
    window.TaskSchema.createTask("Active 2"),
    window.TaskSchema.deleteTask(window.TaskSchema.createTask("Deleted 1")),
    window.TaskSchema.createTask("Active 3"),
    window.TaskSchema.deleteTask(window.TaskSchema.createTask("Deleted 2"))
];

const activeTasks = window.TaskSchema.getActiveTasks(mixedTasks);
const deletedTasks = window.TaskSchema.getDeletedTasks(mixedTasks);

assert(activeTasks.length === 3, 'Should filter 3 active tasks');
assert(deletedTasks.length === 2, 'Should filter 2 deleted tasks');
assert(activeTasks.every(t => !t.deleted), 'All active tasks should not be deleted');
assert(deletedTasks.every(t => t.deleted), 'All deleted tasks should be deleted');

/* ===================================================
   TEST 8: FIND TASK BY ID
   =================================================== */

console.log('\n--- Test 8: Find Task By ID ---');

const task1 = window.TaskSchema.createTask("Task 1");
const task2 = window.TaskSchema.createTask("Task 2");
const task3 = window.TaskSchema.createTask("Task 3");
const testTasks = [task1, task2, task3];

const foundTask = window.TaskSchema.findTaskById(testTasks, task2.id);
const foundIndex = window.TaskSchema.findTaskIndexById(testTasks, task2.id);

assert(foundTask.id === task2.id, 'Should find correct task by ID');
assert(foundTask.text === "Task 2", 'Found task should have correct text');
assert(foundIndex === 1, 'Should find correct index');

const notFoundTask = window.TaskSchema.findTaskById(testTasks, "non-existent-id");
const notFoundIndex = window.TaskSchema.findTaskIndexById(testTasks, "non-existent-id");

assert(notFoundTask === null, 'Should return null for non-existent ID');
assert(notFoundIndex === -1, 'Should return -1 for non-existent ID');

/* ===================================================
   TEST 9: TASK VALIDATION
   =================================================== */

console.log('\n--- Test 9: Task Validation ---');

const validTask = window.TaskSchema.createTask("Valid task");
const validationValid = window.TaskSchema.validateTask(validTask);

assert(validationValid.valid === true, 'Valid task should pass validation');
assert(validationValid.errors.length === 0, 'Valid task should have no errors');

const invalidTask = {
    text: "Invalid task",
    done: "not a boolean", // Invalid type
    // Missing required fields
};

const validationInvalid = window.TaskSchema.validateTask(invalidTask);

assert(validationInvalid.valid === false, 'Invalid task should fail validation');
assert(validationInvalid.errors.length > 0, 'Invalid task should have errors');

/* ===================================================
   TEST 10: MERGE TASK VERSIONS
   =================================================== */

console.log('\n--- Test 10: Merge Task Versions ---');

const localVersion = window.TaskSchema.createTask("Task");
const remoteVersion = window.TaskSchema.updateTask(localVersion, { 
    text: "Updated remotely" 
});

// Simulate time difference
setTimeout(() => {
    const laterLocalVersion = window.TaskSchema.updateTask(localVersion, { 
        text: "Updated locally later" 
    });
    
    const merged1 = window.TaskSchema.mergeTaskVersions(localVersion, remoteVersion);
    assert(merged1.text === "Updated remotely", 'Should use remote version (newer)');
    
    const merged2 = window.TaskSchema.mergeTaskVersions(laterLocalVersion, remoteVersion);
    assert(merged2.text === "Updated locally later", 'Should use local version (newer)');
    
    console.log('✅ Merge task versions test completed');
}, 10);

/* ===================================================
   TEST 11: MERGE TASK ARRAYS
   =================================================== */

console.log('\n--- Test 11: Merge Task Arrays ---');

const localTasks = [
    window.TaskSchema.createTask("Local Task 1"),
    window.TaskSchema.createTask("Local Task 2")
];

const remoteTask1 = { ...localTasks[0] };
remoteTask1.text = "Updated Remote Task 1";
remoteTask1.updatedAt = new Date(Date.now() + 1000).toISOString(); // Newer

const remoteTasks = [
    remoteTask1,
    window.TaskSchema.createTask("New Remote Task")
];

const merged = window.TaskSchema.mergeTasks(localTasks, remoteTasks);

assert(merged.length === 3, 'Merged array should have 3 tasks');
assert(merged.some(t => t.text === "Updated Remote Task 1"), 'Should include updated remote task');
assert(merged.some(t => t.text === "Local Task 2"), 'Should include local-only task');
assert(merged.some(t => t.text === "New Remote Task"), 'Should include remote-only task');

/* ===================================================
   TEST 12: NORMALIZE REMINDER TIME
   =================================================== */

console.log('\n--- Test 12: Normalize Reminder Time ---');

assert(window.TaskSchema.normalizeReminderTime(0) === null, 'Should convert 0 to null');
assert(window.TaskSchema.normalizeReminderTime('0') === null, 'Should convert "0" to null');
assert(window.TaskSchema.normalizeReminderTime('') === null, 'Should convert empty string to null');
assert(window.TaskSchema.normalizeReminderTime(undefined) === null, 'Should convert undefined to null');
assert(window.TaskSchema.normalizeReminderTime(null) === null, 'Should preserve null');

const isoDate = window.TaskSchema.normalizeReminderTime("2025-12-10T09:45:00.000Z");
assert(isoDate === "2025-12-10T09:45:00.000Z", 'Should preserve valid ISO date');

/* ===================================================
   TEST 13: IDEMPOTENT MIGRATION
   =================================================== */

console.log('\n--- Test 13: Idempotent Migration ---');

const dataToMigrate = {
    tasks: [
        { text: "Old task", done: false },
        window.TaskSchema.createTask("Already migrated")
    ]
};

const firstMigration = window.TaskSchema.migrateTasks(dataToMigrate);
const secondMigration = window.TaskSchema.migrateTasks(firstMigration);

assert(firstMigration.tasks.length === secondMigration.tasks.length, 'Task count should be same');
assert(firstMigration.tasks[0].id === secondMigration.tasks[0].id, 'IDs should be preserved');
assert(firstMigration.tasks[1].id === secondMigration.tasks[1].id, 'Migrated task ID should be preserved');

/* ===================================================
   TEST SUMMARY
   =================================================== */

setTimeout(() => {
    console.log('\n' + '='.repeat(50));
    console.log('📊 TEST SUMMARY');
    console.log('='.repeat(50));
    console.log(`✅ Passed: ${testsPassed}`);
    console.log(`❌ Failed: ${testsFailed}`);
    console.log(`📈 Total: ${testsPassed + testsFailed}`);
    console.log(`🎯 Success Rate: ${((testsPassed / (testsPassed + testsFailed)) * 100).toFixed(1)}%`);
    console.log('='.repeat(50));
    
    if (testsFailed === 0) {
        console.log('🎉 ALL TESTS PASSED!');
    } else {
        console.log('⚠️  SOME TESTS FAILED - Review errors above');
    }
}, 100);
