/* ===================================================
   iDo - MAIN APPLICATION LOGIC
   =================================================== */

// Application state
let tasks = [];
let isSaving = false;

// DOM Elements
const loginSection = document.getElementById('login-section');
const appSection = document.getElementById('app-section');
const loginBtn = document.getElementById('login-btn');
const logoutBtn = document.getElementById('logout-btn');
const taskInput = document.getElementById('task-input');
const addTaskBtn = document.getElementById('add-task-btn');
const tasksContainer = document.getElementById('tasks-container');
const syncStatusText = document.getElementById('sync-status-text');

/* ===================================================
   INITIALIZATION
   =================================================== */

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', async () => {
    // Check if CONFIG is loaded
    if (typeof CONFIG === 'undefined') {
        console.error('CONFIG not loaded! Make sure config.js is loaded before app.js');
        alert('Configuration error. Please check the console.');
        return;
    }
    
    console.log('CONFIG loaded:', { clientId: CONFIG.clientId, redirectUri: CONFIG.redirectUri });
    
    // Check if this is an OAuth callback
    const isCallback = await window.DriveAPI.handleCallback();
    
    if (isCallback) {
        // Successfully authenticated, load the app
        await initializeApp();
    } else {
        // Check if user is already logged in
        const isLoggedIn = window.DriveAPI.checkExistingAuth();
        
        if (isLoggedIn) {
            await initializeApp();
        } else {
            showLoginScreen();
        }
    }
});

// Show login screen
function showLoginScreen() {
    loginSection.classList.remove('hidden');
    appSection.classList.add('hidden');
}

// Show app screen
function showAppScreen() {
    loginSection.classList.add('hidden');
    appSection.classList.remove('hidden');
}

// Initialize the app
async function initializeApp() {
    showAppScreen();
    updateSyncStatus('loading', 'Loading tasks...');
    
    try {
        // Load tasks from Google Drive
        tasks = await window.DriveAPI.loadTasks();
        renderTasks();
        updateSyncStatus('synced', 'Synced');
    } catch (error) {
        console.error('Failed to load tasks:', error);
        updateSyncStatus('error', 'Failed to load tasks');
        tasks = [];
        renderTasks();
    }
}

/* ===================================================
   EVENT LISTENERS
   =================================================== */

// Login button
loginBtn.addEventListener('click', () => {
    window.DriveAPI.initiateLogin();
});

// Logout button
logoutBtn.addEventListener('click', () => {
    if (confirm('Are you sure you want to logout?')) {
        window.DriveAPI.logout();
    }
});

// Add task button
addTaskBtn.addEventListener('click', () => {
    addTask();
});

// Add task on Enter key
taskInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        addTask();
    }
});

/* ===================================================
   TASK OPERATIONS
   =================================================== */

// Add a new task
async function addTask() {
    const text = taskInput.value.trim();
    
    if (!text) {
        return;
    }
    
    // Create new task
    const newTask = {
        text: text,
        done: false
    };
    
    // Add to tasks array
    tasks.push(newTask);
    
    // Clear input
    taskInput.value = '';
    
    // Re-render tasks
    renderTasks();
    
    // Save to Google Drive
    await saveTasksToCloud();
}

// Toggle task done/undone
async function toggleTask(index) {
    tasks[index].done = !tasks[index].done;
    renderTasks();
    await saveTasksToCloud();
}

// Delete a task
async function deleteTask(index) {
    tasks.splice(index, 1);
    renderTasks();
    await saveTasksToCloud();
}

/* ===================================================
   RENDERING
   =================================================== */

// Render all tasks to the DOM
function renderTasks() {
    tasksContainer.innerHTML = '';
    
    if (tasks.length === 0) {
        tasksContainer.innerHTML = `
            <div class="empty-state">
                <p>No tasks yet!</p>
                <small>Add your first task above to get started</small>
            </div>
        `;
        return;
    }
    
    tasks.forEach((task, index) => {
        const taskItem = createTaskElement(task, index);
        tasksContainer.appendChild(taskItem);
    });
}

// Create a task element
function createTaskElement(task, index) {
    const div = document.createElement('div');
    div.className = `task-item ${task.done ? 'completed' : ''}`;
    
    // Checkbox
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'task-checkbox';
    checkbox.checked = task.done;
    checkbox.addEventListener('change', () => toggleTask(index));
    
    // Task text
    const text = document.createElement('span');
    text.className = 'task-text';
    text.textContent = task.text;
    
    // Delete button
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'delete-button';
    deleteBtn.textContent = 'Delete';
    deleteBtn.addEventListener('click', () => {
        if (confirm('Delete this task?')) {
            deleteTask(index);
        }
    });
    
    // Append elements
    div.appendChild(checkbox);
    div.appendChild(text);
    div.appendChild(deleteBtn);
    
    return div;
}

/* ===================================================
   CLOUD SYNC
   =================================================== */

// Save tasks to Google Drive
async function saveTasksToCloud() {
    if (isSaving) {
        return; // Prevent multiple simultaneous saves
    }
    
    isSaving = true;
    updateSyncStatus('saving', 'Saving...');
    
    try {
        await window.DriveAPI.saveTasks(tasks);
        updateSyncStatus('synced', 'Synced');
    } catch (error) {
        console.error('Failed to save tasks:', error);
        console.error('Error details:', {
            message: error.message,
            stack: error.stack
        });
        updateSyncStatus('error', 'Sync failed: ' + error.message);
    } finally {
        isSaving = false;
    }
}

/* ===================================================
   UI UPDATES
   =================================================== */

// Update sync status indicator
function updateSyncStatus(status, text) {
    const syncStatus = document.querySelector('.sync-status');
    
    // Remove all status classes
    syncStatus.classList.remove('saving', 'synced', 'error');
    
    // Add current status class
    if (status !== 'loading') {
        syncStatus.classList.add(status);
    }
    
    // Update text
    syncStatusText.textContent = text;
}
