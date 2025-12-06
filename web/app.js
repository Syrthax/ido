/* ===================================================
   iDo - MAIN APPLICATION LOGIC
   =================================================== */

// Application state
let tasks = [];
let isSaving = false;
let currentDueDate = null;
let currentReminderTime = null;
let notificationPermission = false;

// DOM Elements
const loginSection = document.getElementById('login-section');
const appSection = document.getElementById('app-section');
const loginBtn = document.getElementById('login-btn');
const logoutBtn = document.getElementById('logout-btn');
const taskInput = document.getElementById('task-input');
const addTaskBtn = document.getElementById('add-task-btn');
const tasksContainer = document.getElementById('tasks-container');
const syncStatusText = document.getElementById('sync-status-text');
const manualSyncBtn = document.getElementById('manual-sync-btn');

// DateTime Elements
const datetimeToggleBtn = document.getElementById('add-datetime-btn');
const datetimePanel = document.getElementById('datetime-panel');
const datetimeTabs = document.querySelectorAll('.datetime-tab');
const naturalDateInput = document.getElementById('natural-date-input');
const suggestionChips = document.querySelectorAll('.suggestion-chip');
const datePicker = document.getElementById('date-picker');
const timePicker = document.getElementById('time-picker');
const reminderEnabled = document.getElementById('reminder-enabled');
const reminderTime = document.getElementById('reminder-time');
const setDatetimeBtn = document.getElementById('set-datetime-btn');
const clearDatetimeBtn = document.getElementById('clear-datetime-btn');

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
    
    // Request notification permission
    await requestNotificationPermission();
    
    // Start notification checker
    startNotificationChecker();
    
    try {
        // Fetch user info
        const storedUserInfo = localStorage.getItem('user_info');
        let user;
        
        if (storedUserInfo) {
            user = JSON.parse(storedUserInfo);
        } else {
            user = await window.DriveAPI.getUserInfo();
        }
        
        // Display user info
        displayUserInfo(user);
        
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
    
    // Initialize date/time picker
    initializeDateTimePicker();
}

// Display user info in the header
function displayUserInfo(user) {
    const headerLogo = document.querySelector('.header-logo');
    if (!headerLogo) return;
    
    // Create user info element
    const userInfoEl = document.createElement('div');
    userInfoEl.className = 'user-info';
    userInfoEl.innerHTML = `
        ${user.picture ? `<img src="${user.picture}" alt="${user.name}" class="user-avatar">` : ''}
        <div class="user-details">
            <div class="user-name">${user.name}</div>
            <div class="user-email">${user.email}</div>
        </div>
    `;
    
    // Insert after logout button
    const header = document.querySelector('.header');
    const logoutBtn = document.getElementById('logout-btn');
    header.insertBefore(userInfoEl, logoutBtn);
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

// Manual sync button
manualSyncBtn.addEventListener('click', async () => {
    if (!isSaving) {
        manualSyncBtn.classList.add('syncing');
        await saveTasksToCloud();
        setTimeout(() => {
            manualSyncBtn.classList.remove('syncing');
        }, 500);
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
        done: false,
        dueDate: currentDueDate,
        reminderTime: currentReminderTime,
        notified: false
    };
    
    // Add to tasks array
    tasks.push(newTask);
    
    // Clear input and reset due date
    taskInput.value = '';
    currentDueDate = null;
    currentReminderTime = null;
    datetimeToggleBtn.classList.remove('active');
    datetimePanel.classList.add('hidden');
    
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
    const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && !task.done;
    div.className = `task-item ${task.done ? 'completed' : ''} ${isOverdue ? 'overdue' : ''}`;
    
    // Checkbox
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'task-checkbox';
    checkbox.checked = task.done;
    checkbox.addEventListener('change', () => toggleTask(index));
    
    // Task text container
    const textContainer = document.createElement('div');
    textContainer.className = 'task-text';
    
    // Main task text
    const text = document.createElement('span');
    text.className = 'task-main-text';
    text.textContent = task.text;
    textContainer.appendChild(text);
    
    // Due date display
    if (task.dueDate) {
        const dueDateEl = document.createElement('span');
        dueDateEl.className = 'task-due-date';
        
        const dueDate = new Date(task.dueDate);
        const formattedDate = formatDueDate(dueDate);
        
        dueDateEl.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="16" y1="2" x2="16" y2="6"></line>
                <line x1="8" y1="2" x2="8" y2="6"></line>
                <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
            ${formattedDate}
        `;
        textContainer.appendChild(dueDateEl);
    }
    
    // Overdue badge
    if (isOverdue) {
        const overdueEl = document.createElement('div');
        overdueEl.className = 'overdue-badge';
        overdueEl.innerHTML = `
            <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2L2 7v10c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-10-5zm0 16h-2v-2h2v2zm0-4h-2V6h2v8z"/>
            </svg>
            Task Overdue
        `;
        div.appendChild(checkbox);
        div.appendChild(textContainer);
        div.appendChild(overdueEl);
    } else {
        div.appendChild(checkbox);
        div.appendChild(textContainer);
    }
    
    // Delete button
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'delete-button';
    deleteBtn.textContent = 'Delete';
    deleteBtn.addEventListener('click', () => {
        if (confirm('Delete this task?')) {
            deleteTask(index);
        }
    });
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

/* ===================================================
   DATE/TIME PICKER FUNCTIONALITY
   =================================================== */

// Initialize date/time picker
function initializeDateTimePicker() {
    // Toggle date/time panel
    datetimeToggleBtn.addEventListener('click', () => {
        datetimePanel.classList.toggle('hidden');
        datetimeToggleBtn.classList.toggle('active');
    });
    
    // Tab switching
    datetimeTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetTab = tab.dataset.tab;
            
            // Update active tab
            datetimeTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            // Update active content
            document.querySelectorAll('.datetime-tab-content').forEach(content => {
                content.classList.remove('active');
            });
            document.querySelector(`[data-content="${targetTab}"]`).classList.add('active');
        });
    });
    
    // Natural language suggestions
    suggestionChips.forEach(chip => {
        chip.addEventListener('click', () => {
            naturalDateInput.value = chip.dataset.natural;
        });
    });
    
    // Reminder checkbox
    reminderEnabled.addEventListener('change', () => {
        reminderTime.disabled = !reminderEnabled.checked;
    });
    
    // Set date/time button
    setDatetimeBtn.addEventListener('click', () => {
        const activeTab = document.querySelector('.datetime-tab.active').dataset.tab;
        
        if (activeTab === 'natural') {
            const naturalText = naturalDateInput.value.trim();
            if (naturalText) {
                currentDueDate = parseNaturalLanguageDate(naturalText);
            }
        } else {
            const date = datePicker.value;
            const time = timePicker.value;
            
            if (date) {
                currentDueDate = new Date(date + (time ? 'T' + time : 'T23:59'));
            }
        }
        
        // Set reminder time
        if (reminderEnabled.checked && currentDueDate) {
            currentReminderTime = parseInt(reminderTime.value);
        } else {
            currentReminderTime = null;
        }
        
        // Update UI
        if (currentDueDate) {
            datetimeToggleBtn.classList.add('active');
            datetimePanel.classList.add('hidden');
        }
    });
    
    // Clear button
    clearDatetimeBtn.addEventListener('click', () => {
        currentDueDate = null;
        currentReminderTime = null;
        naturalDateInput.value = '';
        datePicker.value = '';
        timePicker.value = '';
        reminderEnabled.checked = false;
        reminderTime.disabled = true;
        datetimeToggleBtn.classList.remove('active');
        datetimePanel.classList.add('hidden');
    });
}

// Natural language date parser
function parseNaturalLanguageDate(text) {
    const now = new Date();
    text = text.toLowerCase().trim();
    
    // Extract time if present
    let hour = 23, minute = 59;
    const timeMatch = text.match(/(\d{1,2})\s*(am|pm|:(\d{2}))?\s*(am|pm)?/i);
    
    if (timeMatch) {
        hour = parseInt(timeMatch[1]);
        const hasColon = timeMatch[2] && timeMatch[2].startsWith(':');
        const meridiem = timeMatch[4] || timeMatch[2];
        
        if (hasColon) {
            minute = parseInt(timeMatch[3]);
        } else {
            minute = 0;
        }
        
        if (meridiem && meridiem.toLowerCase() === 'pm' && hour < 12) {
            hour += 12;
        } else if (meridiem && meridiem.toLowerCase() === 'am' && hour === 12) {
            hour = 0;
        }
    }
    
    let targetDate = new Date();
    
    // Today
    if (text.includes('today')) {
        targetDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour, minute);
    }
    // Tomorrow
    else if (text.includes('tomorrow')) {
        targetDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, hour, minute);
    }
    // Next week/monday/tuesday etc
    else if (text.includes('next')) {
        const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
        const dayMatch = days.find(day => text.includes(day));
        
        if (dayMatch) {
            const targetDay = days.indexOf(dayMatch);
            const currentDay = now.getDay();
            let daysToAdd = targetDay - currentDay;
            if (daysToAdd <= 0) daysToAdd += 7;
            
            targetDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + daysToAdd, hour, minute);
        } else if (text.includes('week')) {
            targetDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 7, hour, minute);
        }
    }
    // Specific day of week (this week)
    else {
        const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
        const dayMatch = days.find(day => text.includes(day));
        
        if (dayMatch) {
            const targetDay = days.indexOf(dayMatch);
            const currentDay = now.getDay();
            let daysToAdd = targetDay - currentDay;
            if (daysToAdd < 0) daysToAdd += 7;
            
            targetDate = new Date(now.getFullYear(), now.getMonth(), now.getDate() + daysToAdd, hour, minute);
        }
    }
    
    return targetDate.toISOString();
}

// Format due date for display
function formatDueDate(date) {
    const now = new Date();
    const diff = date - now;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    const timeStr = date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
    
    if (days < 0) {
        return `Overdue - ${timeStr}`;
    } else if (days === 0) {
        return `Today at ${timeStr}`;
    } else if (days === 1) {
        return `Tomorrow at ${timeStr}`;
    } else if (days < 7) {
        const dayName = date.toLocaleDateString('en-US', { weekday: 'long' });
        return `${dayName} at ${timeStr}`;
    } else {
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ` at ${timeStr}`;
    }
}

/* ===================================================
   NOTIFICATION SYSTEM
   =================================================== */

// Request notification permission
async function requestNotificationPermission() {
    if ('Notification' in window) {
        const permission = await Notification.requestPermission();
        notificationPermission = permission === 'granted';
    }
}

// Start notification checker
function startNotificationChecker() {
    // Check every minute
    setInterval(() => {
        checkDueTasks();
    }, 60000); // 60 seconds
    
    // Also check immediately
    checkDueTasks();
}

// Check for due tasks and send notifications
function checkDueTasks() {
    if (!notificationPermission) return;
    
    const now = new Date();
    
    tasks.forEach((task, index) => {
        if (task.done || task.notified || !task.dueDate) return;
        
        const dueDate = new Date(task.dueDate);
        const reminderMinutes = task.reminderTime || 0;
        const notifyTime = new Date(dueDate.getTime() - (reminderMinutes * 60 * 1000));
        
        // Check if it's time to notify
        if (now >= notifyTime && now < dueDate) {
            sendNotification(task, index, reminderMinutes);
            tasks[index].notified = true;
        } else if (now >= dueDate) {
            // Task is overdue
            if (!task.notified) {
                sendNotification(task, index, 0, true);
                tasks[index].notified = true;
            }
        }
    });
}

// Send browser notification
function sendNotification(task, index, reminderMinutes, isOverdue = false) {
    let title, body;
    
    if (isOverdue) {
        title = '⚠️ Task Overdue!';
        body = `"${task.text}" is now overdue!`;
    } else if (reminderMinutes > 0) {
        title = '⏰ Task Reminder';
        body = `"${task.text}" is due in ${reminderMinutes} minutes!`;
    } else {
        title = '🔔 Task Due Now!';
        body = `Time to complete: "${task.text}"`;
    }
    
    const notification = new Notification(title, {
        body: body,
        icon: '../assets/logo.png',
        badge: '../assets/logo.png',
        tag: `task-${index}`,
        requireInteraction: true
    });
    
    notification.onclick = () => {
        window.focus();
        notification.close();
    };
}
