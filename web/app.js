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
const addTaskBtnSidebar = document.getElementById('add-task-btn-sidebar');
const addTaskSection = document.getElementById('add-task-section');
const plannedTasksContainer = document.getElementById('planned-tasks-container');
const unplannedTasksContainer = document.getElementById('unplanned-tasks-container');
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
        
        // Make tasks available globally for calendar.js
        window.tasks = tasks;
        
        // Render tasks in sidebar
        renderTasksSidebar();
        
        // Initialize calendar
        if (window.initCalendar) {
            window.initCalendar();
        }
        
        // Initialize event modal
        if (window.initEventModal) {
            window.initEventModal();
        }
        
        updateSyncStatus('synced', 'Synced');
    } catch (error) {
        console.error('Failed to load tasks:', error);
        updateSyncStatus('error', 'Failed to load tasks');
        tasks = [];
        window.tasks = tasks;
        renderTasksSidebar();
    }
    
    // Initialize date/time picker
    initializeDateTimePicker();
}

// Display user info in the header
function displayUserInfo(user) {
    const profileInfo = document.getElementById('profile-info');
    if (!profileInfo) return;
    
    // Create profile info element for sidebar
    profileInfo.innerHTML = `
        ${user.picture ? `<img src="${user.picture}" alt="${user.name}" class="profile-avatar">` : ''}
        <div class="profile-details">
            <div class="profile-name">${user.name}</div>
            <div class="profile-email">${user.email}</div>
        </div>
    `;
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

// Add task button in sidebar header
if (addTaskBtnSidebar) {
    addTaskBtnSidebar.addEventListener('click', () => {
        addTaskSection.classList.toggle('hidden');
        if (!addTaskSection.classList.contains('hidden')) {
            taskInput.focus();
        }
    });
}

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
    
    // Create new task using schema
    const newTask = window.TaskSchema.createTask(text, {
        priority: false,
        dueDate: currentDueDate,
        reminderTime: currentReminderTime
    });
    
    // Add to tasks array
    tasks.push(newTask);
    window.tasks = tasks;
    
    // Clear input and reset due date
    taskInput.value = '';
    currentDueDate = null;
    currentReminderTime = null;
    datetimeToggleBtn.classList.remove('active');
    datetimePanel.classList.add('hidden');
    
    // Re-render tasks sidebar
    renderTasksSidebar();
    
    // Re-render calendar if available
    if (window.renderCalendar) {
        window.renderCalendar();
    }
    if (window.renderTodaySection) {
        window.renderTodaySection();
    }
    
    // Save to Google Drive
    await saveTasksToCloud();
}

// Toggle task done/undone
async function toggleTask(id) {
    const task = window.TaskSchema.findTaskById(tasks, id);
    if (!task) return;
    
    const index = window.TaskSchema.findTaskIndexById(tasks, id);
    tasks[index] = window.TaskSchema.updateTask(task, { done: !task.done });
    window.tasks = tasks;
    
    renderTasksSidebar();
    if (window.renderCalendar) {
        window.renderCalendar();
    }
    if (window.renderTodaySection) {
        window.renderTodaySection();
    }
    await saveTasksToCloud();
}

// Make toggleTask available globally for onclick handlers
window.toggleTask = toggleTask;

// Toggle task priority (pin to top)
async function togglePriority(id) {
    const task = window.TaskSchema.findTaskById(tasks, id);
    if (!task) return;
    
    const index = window.TaskSchema.findTaskIndexById(tasks, id);
    tasks[index] = window.TaskSchema.updateTask(task, { priority: !task.priority });
    window.tasks = tasks;
    
    // Re-sort tasks: pinned first, then by original order
    tasks.sort((a, b) => {
        if (a.priority && !b.priority) return -1;
        if (!a.priority && b.priority) return 1;
        return 0;
    });
    
    renderTasksSidebar();
    if (window.renderCalendar) {
        window.renderCalendar();
    }
    await saveTasksToCloud();
}

// Delete a task
async function deleteTask(id) {
    const task = window.TaskSchema.findTaskById(tasks, id);
    if (!task) return;
    
    const index = window.TaskSchema.findTaskIndexById(tasks, id);
    // Use soft delete
    tasks[index] = window.TaskSchema.deleteTask(task);
    
    // Filter out deleted tasks for display
    tasks = window.TaskSchema.getActiveTasks(tasks);
    window.tasks = tasks;
    
    renderTasksSidebar();
    if (window.renderCalendar) {
        window.renderCalendar();
    }
    if (window.renderTodaySection) {
        window.renderTodaySection();
    }
    await saveTasksToCloud();
}

/* ===================================================
   RENDERING
   =================================================== */

// Render tasks in sidebar (planned and unplanned)
function renderTasksSidebar() {
    if (!plannedTasksContainer || !unplannedTasksContainer) return;
    
    // Clear containers
    plannedTasksContainer.innerHTML = '';
    unplannedTasksContainer.innerHTML = '';
    
    if (tasks.length === 0) {
        unplannedTasksContainer.innerHTML = `
            <div class="empty-state">
                <p>No tasks yet!</p>
                <small>Add your first task above</small>
            </div>
        `;
        return;
    }
    
    // Sort tasks: pinned first
    const sortedTasks = [...tasks].sort((a, b) => {
        if (a.priority && !b.priority) return -1;
        if (!a.priority && b.priority) return 1;
        return 0;
    });
    
    // Separate planned and unplanned tasks
    const plannedTasks = sortedTasks.filter(task => task.dueDate && !task.deleted);
    const unplannedTasks = sortedTasks.filter(task => !task.dueDate && !task.deleted);
    
    // Render planned tasks
    if (plannedTasks.length === 0) {
        plannedTasksContainer.innerHTML = '<div class="empty-state"><small>No planned tasks</small></div>';
    } else {
        plannedTasks.forEach(task => {
            const taskEl = createSidebarTaskElement(task);
            plannedTasksContainer.appendChild(taskEl);
        });
    }
    
    // Render unplanned tasks
    if (unplannedTasks.length === 0) {
        unplannedTasksContainer.innerHTML = '<div class="empty-state"><small>No unplanned tasks</small></div>';
    } else {
        unplannedTasks.forEach(task => {
            const taskEl = createSidebarTaskElement(task);
            unplannedTasksContainer.appendChild(taskEl);
        });
    }
}

// Create a sidebar task element (simplified version)
function createSidebarTaskElement(task) {
    const div = document.createElement('div');
    const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && !task.done;
    div.className = `task-item ${task.done ? 'completed' : ''} ${isOverdue ? 'overdue' : ''} ${task.priority ? 'pinned' : ''}`;
    div.draggable = true;
    div.dataset.taskId = task.id;
    
    // Drag events - use the calendar drag handlers for tasks with dates, or sidebar drag for unplanned
    if (task.dueDate && window.handleTaskDragStart) {
        div.addEventListener('dragstart', window.handleTaskDragStart);
        div.addEventListener('dragend', window.handleTaskDragEnd);
    } else {
        div.addEventListener('dragstart', window.handleTaskDragStart || handleDragStart);
        div.addEventListener('dragend', window.handleTaskDragEnd || handleDragEnd);
    }
    
    // Drag handle
    const dragHandle = document.createElement('div');
    dragHandle.className = 'drag-handle';
    dragHandle.innerHTML = `
        <svg viewBox="0 0 24 24" fill="currentColor">
            <circle cx="9" cy="5" r="1.5"></circle>
            <circle cx="9" cy="12" r="1.5"></circle>
            <circle cx="9" cy="19" r="1.5"></circle>
            <circle cx="15" cy="5" r="1.5"></circle>
            <circle cx="15" cy="12" r="1.5"></circle>
            <circle cx="15" cy="19" r="1.5"></circle>
        </svg>
    `;
    
    // Checkbox
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'task-checkbox';
    checkbox.checked = task.done;
    checkbox.addEventListener('change', () => toggleTask(task.id));
    
    // Task text container
    const textContainer = document.createElement('div');
    textContainer.className = 'task-text';
    
    // Main task text
    const text = document.createElement('span');
    text.className = 'task-main-text';
    text.textContent = task.text;
    textContainer.appendChild(text);
    
    // Due date display (only for planned tasks in sidebar)
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
    
    // Priority button
    const priorityBtn = document.createElement('button');
    priorityBtn.className = `priority-button ${task.priority ? 'active' : ''}`;
    priorityBtn.title = task.priority ? 'Unpin task' : 'Pin to top';
    priorityBtn.innerHTML = `
        <svg viewBox="0 0 24 24" fill="${task.priority ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
        </svg>
    `;
    priorityBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        togglePriority(task.id);
    });
    
    div.appendChild(dragHandle);
    div.appendChild(checkbox);
    div.appendChild(textContainer);
    div.appendChild(priorityBtn);
    
    // Delete button
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'delete-button';
    deleteBtn.textContent = 'Delete';
    deleteBtn.addEventListener('click', () => {
        if (confirm('Delete this task?')) {
            deleteTask(task.id);
        }
    });
    div.appendChild(deleteBtn);
    
    return div;
}

// Legacy renderTasks function (kept for backward compatibility)
function renderTasks() {
    renderTasksSidebar();
}

// Make renderTasksSidebar available globally
window.renderTasksSidebar = renderTasksSidebar;

// Create a task element (legacy function for compatibility)
function createTaskElement(task) {
    return createSidebarTaskElement(task);
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
        
        // Update calendar and today section after save
        if (window.renderCalendar) {
            window.renderCalendar();
        }
        if (window.renderTodaySection) {
            window.renderTodaySection();
        }
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

// Make saveTasksToCloud available globally
window.saveTasksToCloud = saveTasksToCloud;

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
                currentDueDate = new Date(date + (time ? 'T' + time : 'T23:59')).toISOString();
            }
        }
        
        // Set reminder time as ISO timestamp
        if (reminderEnabled.checked && currentDueDate) {
            const reminderMinutes = parseInt(reminderTime.value) || 0;
            const dueDate = new Date(currentDueDate);
            const reminderDate = new Date(dueDate.getTime() - (reminderMinutes * 60 * 1000));
            currentReminderTime = reminderDate.toISOString();
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
    
    tasks.forEach((task) => {
        if (task.done || task.notified || !task.dueDate) return;
        
        const dueDate = new Date(task.dueDate);
        
        // Check if reminderTime is set (ISO string)
        if (task.reminderTime) {
            const reminderDate = new Date(task.reminderTime);
            
            // Check if it's time to send reminder
            if (now >= reminderDate && now < dueDate) {
                const minutesUntilDue = Math.floor((dueDate - now) / (60 * 1000));
                sendNotification(task, minutesUntilDue);
                
                // Update task as notified
                const index = window.TaskSchema.findTaskIndexById(tasks, task.id);
                tasks[index] = window.TaskSchema.updateTask(task, { notified: true });
            }
        }
        
        // Check if task is overdue
        if (now >= dueDate) {
            if (!task.notified) {
                sendNotification(task, 0, true);
                
                // Update task as notified
                const index = window.TaskSchema.findTaskIndexById(tasks, task.id);
                tasks[index] = window.TaskSchema.updateTask(task, { notified: true });
            }
        }
    });
}

// Send browser notification
function sendNotification(task, reminderMinutes, isOverdue = false) {
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
        tag: `task-${task.id}`,
        requireInteraction: true
    });
    
    notification.onclick = () => {
        window.focus();
        notification.close();
    };
}

/* ===================================================
   DRAG AND DROP FUNCTIONALITY
   =================================================== */

let draggedElement = null;

function handleDragStart(e) {
    draggedElement = this;
    const draggedTaskId = this.dataset.taskId;
    this.classList.add('dragging');
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/html', this.innerHTML);
}

function handleDragOver(e) {
    if (e.preventDefault) {
        e.preventDefault();
    }
    e.dataTransfer.dropEffect = 'move';
    return false;
}

function handleDragEnter(e) {
    if (this !== draggedElement) {
        this.classList.add('drag-over');
    }
}

function handleDragLeave(e) {
    this.classList.remove('drag-over');
}

function handleDrop(e) {
    if (e.stopPropagation) {
        e.stopPropagation();
    }
    
    if (draggedElement !== this) {
        const dropTaskId = this.dataset.taskId;
        
        // Find indices
        const draggedIndex = window.TaskSchema.findTaskIndexById(tasks, draggedTaskId);
        const dropIndex = window.TaskSchema.findTaskIndexById(tasks, dropTaskId);
        
        // Reorder tasks array
        const draggedTask = tasks[draggedIndex];
        tasks.splice(draggedIndex, 1);
        tasks.splice(dropIndex, 0, draggedTask);
        
        // Re-render and save
        renderTasks();
        saveTasksToCloud();
    }
    
    return false;
}

function handleDragEnd(e) {
    this.classList.remove('dragging');
    
    // Remove drag-over class from all items
    document.querySelectorAll('.task-item').forEach(item => {
        item.classList.remove('drag-over');
    });
}
