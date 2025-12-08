/* ===================================================
   iDo 2.0 - GOOGLE CALENDAR INTEGRATION
   =================================================== */

// Calendar state
let calendarEvents = [];
let currentWeekStart = null;

/* ===================================================
   CALENDAR API FUNCTIONS
   =================================================== */

/**
 * Fetch Google Calendar events for a specific date range
 */
async function fetchCalendarEvents(startDate, endDate) {
    const accessToken = localStorage.getItem('access_token');
    if (!accessToken) {
        throw new Error('No access token available');
    }

    try {
        const timeMin = startDate.toISOString();
        const timeMax = endDate.toISOString();
        
        const response = await fetch(
            `https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=${encodeURIComponent(timeMin)}&timeMax=${encodeURIComponent(timeMax)}&singleEvents=true&orderBy=startTime`,
            {
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error('Failed to fetch calendar events');
        }

        const data = await response.json();
        calendarEvents = data.items || [];
        return calendarEvents;
    } catch (error) {
        console.error('Error fetching calendar events:', error);
        throw error;
    }
}

/* ===================================================
   WEEK NAVIGATION
   =================================================== */

/**
 * Get the start of the current week (Monday)
 */
function getWeekStart(date = new Date()) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust for Monday start
    return new Date(d.setDate(diff));
}

/**
 * Get Monday to Saturday of the current week
 */
function getWeekDays(weekStart) {
    const days = [];
    for (let i = 0; i < 6; i++) { // Monday to Saturday (6 days)
        const date = new Date(weekStart);
        date.setDate(weekStart.getDate() + i);
        days.push(date);
    }
    return days;
}

/**
 * Navigate to previous week
 */
function previousWeek() {
    if (!currentWeekStart) {
        currentWeekStart = getWeekStart();
    }
    currentWeekStart.setDate(currentWeekStart.getDate() - 7);
    renderCalendar();
}

/**
 * Navigate to next week
 */
function nextWeek() {
    if (!currentWeekStart) {
        currentWeekStart = getWeekStart();
    }
    currentWeekStart.setDate(currentWeekStart.getDate() + 7);
    renderCalendar();
}

/**
 * Go to current week
 */
function goToToday() {
    currentWeekStart = getWeekStart();
    renderCalendar();
}

/* ===================================================
   CALENDAR RENDERING
   =================================================== */

/**
 * Render the weekly calendar view
 */
async function renderCalendar() {
    if (!currentWeekStart) {
        currentWeekStart = getWeekStart();
    }

    const weekDays = getWeekDays(currentWeekStart);
    const calendarGrid = document.getElementById('calendar-grid');
    
    if (!calendarGrid) {
        console.error('Calendar grid element not found');
        return;
    }

    // Fetch events for the week
    const weekEnd = new Date(weekDays[5]); // Saturday
    weekEnd.setHours(23, 59, 59, 999);
    
    try {
        await fetchCalendarEvents(weekDays[0], weekEnd);
    } catch (error) {
        console.error('Failed to load calendar events:', error);
    }

    // Clear existing content
    calendarGrid.innerHTML = '';

    // Render day columns
    weekDays.forEach((date, index) => {
        const dayColumn = createDayColumn(date, index);
        calendarGrid.appendChild(dayColumn);
    });

    // Update week display header
    updateWeekHeader(weekDays);
}

/**
 * Create a day column for the calendar
 */
function createDayColumn(date, dayIndex) {
    const column = document.createElement('div');
    column.className = 'calendar-day-column';
    column.dataset.date = date.toISOString().split('T')[0];
    
    // Check if today
    const isToday = isSameDay(date, new Date());
    if (isToday) {
        column.classList.add('today');
    }

    // Day header
    const header = document.createElement('div');
    header.className = 'calendar-day-header';
    
    const dayName = date.toLocaleDateString('en-US', { weekday: 'short' });
    const dayNumber = date.getDate();
    
    header.innerHTML = `
        <span class="calendar-day-name">${dayName}</span>
        <span class="calendar-day-number">${dayNumber}</span>
    `;
    column.appendChild(header);

    // Events container
    const eventsContainer = document.createElement('div');
    eventsContainer.className = 'calendar-events-container';
    
    // Add calendar events for this day
    const dayEvents = getEventsForDate(date);
    dayEvents.forEach(event => {
        const eventEl = createCalendarEventElement(event);
        eventsContainer.appendChild(eventEl);
    });

    // Add planned tasks for this day
    const plannedTasks = getPlannedTasksForDate(date);
    plannedTasks.forEach(task => {
        const taskEl = createCalendarTaskElement(task);
        eventsContainer.appendChild(taskEl);
    });

    column.appendChild(eventsContainer);

    // Make droppable
    column.addEventListener('dragover', handleCalendarDragOver);
    column.addEventListener('drop', handleCalendarDrop);
    column.addEventListener('dragleave', handleCalendarDragLeave);
    column.addEventListener('dragenter', handleCalendarDragEnter);

    return column;
}

/**
 * Create a calendar event element
 */
function createCalendarEventElement(event) {
    const eventEl = document.createElement('div');
    eventEl.className = 'calendar-event';
    
    const startTime = event.start.dateTime ? 
        new Date(event.start.dateTime).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }) : 
        'All day';
    
    eventEl.innerHTML = `
        <div class="calendar-event-time">${startTime}</div>
        <div class="calendar-event-title">${event.summary || 'Untitled Event'}</div>
    `;
    
    return eventEl;
}

/**
 * Create a calendar task element (for planned tasks)
 */
function createCalendarTaskElement(task) {
    const taskEl = document.createElement('div');
    taskEl.className = `calendar-task ${task.done ? 'completed' : ''}`;
    taskEl.draggable = true;
    taskEl.dataset.taskId = task.id;
    
    taskEl.innerHTML = `
        <div class="calendar-task-checkbox">
            <input type="checkbox" ${task.done ? 'checked' : ''} onclick="window.toggleTask('${task.id}')">
        </div>
        <div class="calendar-task-text">${task.text}</div>
    `;
    
    // Drag events
    taskEl.addEventListener('dragstart', handleTaskDragStart);
    taskEl.addEventListener('dragend', handleTaskDragEnd);
    
    return taskEl;
}

/**
 * Update the week header display
 */
function updateWeekHeader(weekDays) {
    const weekHeader = document.getElementById('week-header');
    if (!weekHeader) return;

    const monthYear = weekDays[0].toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    const startDay = weekDays[0].getDate();
    const endDay = weekDays[5].getDate();
    
    weekHeader.innerHTML = `
        <button class="week-nav-btn" onclick="window.previousWeek()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
        </button>
        <div class="week-title">
            <h2>${monthYear}</h2>
            <span class="week-range">${startDay} - ${endDay}</span>
        </div>
        <button class="week-nav-btn" onclick="window.nextWeek()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
        </button>
        <button class="today-btn" onclick="window.goToToday()">Today</button>
    `;
}

/* ===================================================
   MINI MONTH CALENDAR
   =================================================== */

/**
 * Render mini month calendar in left sidebar
 */
function renderMiniCalendar() {
    const miniCalendar = document.getElementById('mini-calendar');
    if (!miniCalendar) return;

    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    
    // Get first day of month and number of days
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    // Month header
    const monthName = now.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    
    let html = `
        <div class="mini-calendar-header">
            <h3>${monthName}</h3>
        </div>
        <div class="mini-calendar-grid">
            <div class="mini-calendar-day-name">M</div>
            <div class="mini-calendar-day-name">T</div>
            <div class="mini-calendar-day-name">W</div>
            <div class="mini-calendar-day-name">T</div>
            <div class="mini-calendar-day-name">F</div>
            <div class="mini-calendar-day-name">S</div>
            <div class="mini-calendar-day-name">S</div>
    `;

    // Adjust for Monday start (0 = Monday in our system)
    const adjustedStart = startingDayOfWeek === 0 ? 6 : startingDayOfWeek - 1;
    
    // Empty cells before month starts
    for (let i = 0; i < adjustedStart; i++) {
        html += '<div class="mini-calendar-day empty"></div>';
    }

    // Days of month
    const today = now.getDate();
    for (let day = 1; day <= daysInMonth; day++) {
        const isToday = day === today;
        const dayClass = isToday ? 'mini-calendar-day today' : 'mini-calendar-day';
        html += `<div class="${dayClass}" onclick="window.goToDateFromMini(${year}, ${month}, ${day})">${day}</div>`;
    }

    html += '</div>';
    miniCalendar.innerHTML = html;
}

/**
 * Navigate to specific date from mini calendar
 */
function goToDateFromMini(year, month, day) {
    const selectedDate = new Date(year, month, day);
    currentWeekStart = getWeekStart(selectedDate);
    renderCalendar();
}

/* ===================================================
   TODAY SECTION
   =================================================== */

/**
 * Render today's events and tasks in left sidebar
 */
function renderTodaySection() {
    const todaySection = document.getElementById('today-section');
    if (!todaySection) return;

    const today = new Date();
    const todayEvents = getEventsForDate(today);
    const todayTasks = getPlannedTasksForDate(today);

    let html = `
        <div class="today-header">
            <h3>Today</h3>
            <span class="today-date">${today.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' })}</span>
        </div>
        <div class="today-content">
    `;

    if (todayEvents.length === 0 && todayTasks.length === 0) {
        html += '<div class="today-empty">No events or tasks today</div>';
    } else {
        // Events
        if (todayEvents.length > 0) {
            html += '<div class="today-section-title">Events</div>';
            todayEvents.forEach(event => {
                const startTime = event.start.dateTime ? 
                    new Date(event.start.dateTime).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }) : 
                    'All day';
                html += `
                    <div class="today-item event">
                        <span class="today-item-time">${startTime}</span>
                        <span class="today-item-title">${event.summary || 'Untitled Event'}</span>
                    </div>
                `;
            });
        }

        // Tasks
        if (todayTasks.length > 0) {
            html += '<div class="today-section-title">Tasks</div>';
            todayTasks.forEach(task => {
                html += `
                    <div class="today-item task ${task.done ? 'completed' : ''}">
                        <input type="checkbox" ${task.done ? 'checked' : ''} onclick="window.toggleTask('${task.id}')">
                        <span class="today-item-title">${task.text}</span>
                    </div>
                `;
            });
        }
    }

    html += '</div>';
    todaySection.innerHTML = html;
}

/* ===================================================
   HELPER FUNCTIONS
   =================================================== */

/**
 * Check if two dates are the same day
 */
function isSameDay(date1, date2) {
    return date1.getFullYear() === date2.getFullYear() &&
           date1.getMonth() === date2.getMonth() &&
           date1.getDate() === date2.getDate();
}

/**
 * Get calendar events for a specific date
 */
function getEventsForDate(date) {
    return calendarEvents.filter(event => {
        const eventDate = event.start.dateTime ? 
            new Date(event.start.dateTime) : 
            new Date(event.start.date);
        return isSameDay(eventDate, date);
    });
}

/**
 * Get planned tasks for a specific date
 */
function getPlannedTasksForDate(date) {
    if (!window.tasks) return [];
    
    return window.tasks.filter(task => {
        if (!task.dueDate || task.deleted) return false;
        const taskDate = new Date(task.dueDate);
        return isSameDay(taskDate, date);
    });
}

/* ===================================================
   DRAG AND DROP HANDLERS
   =================================================== */

let draggedTaskElement = null;
let draggedTaskId = null;

/**
 * Handle task drag start from sidebar or calendar
 */
function handleTaskDragStart(e) {
    draggedTaskElement = e.target;
    draggedTaskId = e.target.dataset.taskId;
    e.target.classList.add('dragging');
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/html', e.target.innerHTML);
}

/**
 * Handle task drag end
 */
function handleTaskDragEnd(e) {
    e.target.classList.remove('dragging');
    
    // Remove drag-over class from all columns
    document.querySelectorAll('.calendar-day-column').forEach(col => {
        col.classList.remove('drag-over');
    });
}

/**
 * Handle drag over calendar column
 */
function handleCalendarDragOver(e) {
    if (e.preventDefault) {
        e.preventDefault();
    }
    e.dataTransfer.dropEffect = 'move';
    return false;
}

/**
 * Handle drag enter calendar column
 */
function handleCalendarDragEnter(e) {
    if (e.target.classList.contains('calendar-day-column') || 
        e.target.closest('.calendar-day-column')) {
        const column = e.target.classList.contains('calendar-day-column') ? 
            e.target : e.target.closest('.calendar-day-column');
        column.classList.add('drag-over');
    }
}

/**
 * Handle drag leave calendar column
 */
function handleCalendarDragLeave(e) {
    if (e.target.classList.contains('calendar-day-column')) {
        e.target.classList.remove('drag-over');
    }
}

/**
 * Handle drop on calendar column
 */
async function handleCalendarDrop(e) {
    if (e.stopPropagation) {
        e.stopPropagation();
    }
    e.preventDefault();

    const column = e.target.classList.contains('calendar-day-column') ? 
        e.target : e.target.closest('.calendar-day-column');
    
    if (!column) return;

    column.classList.remove('drag-over');

    const newDateStr = column.dataset.date;
    if (!newDateStr || !draggedTaskId) return;

    // Update task date
    await updateTaskDate(draggedTaskId, newDateStr);

    // Reset drag state
    draggedTaskElement = null;
    draggedTaskId = null;

    // Re-render calendar and tasks
    renderCalendar();
    if (window.renderTasksSidebar) {
        window.renderTasksSidebar();
    }
}

/**
 * Update task's due date
 */
async function updateTaskDate(taskId, newDateStr) {
    if (!window.tasks || !window.TaskSchema) return;

    const task = window.TaskSchema.findTaskById(window.tasks, taskId);
    if (!task) return;

    const index = window.TaskSchema.findTaskIndexById(window.tasks, taskId);
    
    // Create new date with time from original due date or default to 9 AM
    const newDate = new Date(newDateStr);
    if (task.dueDate) {
        const oldDate = new Date(task.dueDate);
        newDate.setHours(oldDate.getHours(), oldDate.getMinutes(), oldDate.getSeconds());
    } else {
        newDate.setHours(9, 0, 0, 0); // Default to 9 AM
    }

    // Update task
    window.tasks[index] = window.TaskSchema.updateTask(task, { 
        dueDate: newDate.toISOString() 
    });

    // Save to cloud
    if (window.saveTasksToCloud) {
        await window.saveTasksToCloud();
    }
}

/* ===================================================
   INITIALIZATION
   =================================================== */

/**
 * Initialize calendar view
 */
function initCalendar() {
    currentWeekStart = getWeekStart();
    renderCalendar();
    renderMiniCalendar();
    renderTodaySection();
}

/* ===================================================
   EXPORT FUNCTIONS
   =================================================== */

// Make functions available globally
window.initCalendar = initCalendar;
window.renderCalendar = renderCalendar;
window.renderMiniCalendar = renderMiniCalendar;
window.renderTodaySection = renderTodaySection;
window.previousWeek = previousWeek;
window.nextWeek = nextWeek;
window.goToToday = goToToday;
window.goToDateFromMini = goToDateFromMini;
window.handleTaskDragStart = handleTaskDragStart;
window.handleTaskDragEnd = handleTaskDragEnd;
