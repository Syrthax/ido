/* ===================================================
   iDo 2.0 - GOOGLE CALENDAR INTEGRATION
   =================================================== */

// Calendar state
let calendarEvents = [];
let currentWeekStart = null;
let currentView = 'week'; // 'day', 'week', 'month', 'year'
let calendarErrorShown = false; // Track if error was already shown
let hasCalendarAccess = null; // null = unknown, true = has access, false = no access
let isInitialLoad = true; // Track if this is the first load

/* ===================================================
   CALENDAR API FUNCTIONS
   =================================================== */

/**
 * Fetch Google Calendar events for a specific date range
 */
async function fetchCalendarEvents(startDate, endDate) {
    const accessToken = localStorage.getItem('access_token');
    if (!accessToken) {
        calendarEvents = [];
        return [];
    }

    // If we know we don't have access, don't even attempt the fetch
    if (hasCalendarAccess === false) {
        calendarEvents = [];
        return [];
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
            // Check for auth errors
            if (response.status === 403 || response.status === 401) {
                hasCalendarAccess = false;
                calendarEvents = [];
                
                // Show error only once per session and not on initial load
                if (!calendarErrorShown && !isInitialLoad) {
                    calendarErrorShown = true;
                    
                    // Delay the prompt to avoid blocking UI
                    setTimeout(() => {
                        const shouldReauth = confirm(
                            'Calendar access is not authorized. To use calendar features, you need to logout and sign in again.\\n\\nWould you like to logout now?'
                        );
                        
                        if (shouldReauth) {
                            localStorage.clear();
                            window.location.reload();
                        }
                    }, 500);
                }
                return [];
            }
            calendarEvents = [];
            return [];
        }

        const data = await response.json();
        calendarEvents = data.items || [];
        hasCalendarAccess = true;
        return calendarEvents;
    } catch (error) {
        // Network or other errors - fail silently
        // Only log if it's not a fetch error and not initial load
        if (!isInitialLoad && !calendarErrorShown && error.message && !error.message.includes('Failed to fetch')) {
            console.warn('Calendar error:', error.message);
        }
        calendarEvents = [];
        return [];
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
    
    await fetchCalendarEvents(weekDays[0], weekEnd);

    // Clear existing content and reset grid styling
    calendarGrid.innerHTML = '';
    calendarGrid.style.gridTemplateColumns = 'repeat(6, 1fr)';
    calendarGrid.style.gap = '1px';

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
    
    // Make header clickable to add event on this date
    header.style.cursor = 'pointer';
    header.addEventListener('click', (e) => {
        // Prevent triggering when clicking on events
        if (e.target === header || e.target.classList.contains('calendar-day-name') || e.target.classList.contains('calendar-day-number')) {
            if (window.openEventModal) {
                const dateStr = date.toISOString().split('T')[0];
                window.openEventModal(dateStr);
            }
        }
    });
    
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
    eventEl.dataset.eventId = event.id;
    eventEl.style.cursor = 'pointer';
    
    const startTime = event.start.dateTime ? 
        new Date(event.start.dateTime).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }) : 
        'All day';
    
    eventEl.innerHTML = `
        <div class="calendar-event-time">${startTime}</div>
        <div class="calendar-event-title">${event.summary || 'Untitled Event'}</div>
    `;
    
    // Make clickable to edit
    eventEl.addEventListener('click', () => {
        if (window.openEditEventModal) {
            // Find the full event object
            const fullEvent = calendarEvents.find(e => e.id === event.id);
            if (fullEvent) {
                window.openEditEventModal(fullEvent);
            }
        }
    });
    
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
   DAY VIEW
   =================================================== */

/**
 * Render single day view with hourly time slots
 */
async function renderDayView() {
    const calendarGrid = document.getElementById('calendar-grid');
    if (!calendarGrid) return;

    // Use current week start to determine which day to show, or today
    const currentDate = currentWeekStart || new Date();
    
    // Fetch events for the day
    const dayStart = new Date(currentDate);
    dayStart.setHours(0, 0, 0, 0);
    const dayEnd = new Date(currentDate);
    dayEnd.setHours(23, 59, 59, 999);
    
    await fetchCalendarEvents(dayStart, dayEnd);

    // Update header
    updateDayViewHeader(currentDate);

    // Clear and set up grid for day view
    calendarGrid.innerHTML = '';
    calendarGrid.style.gridTemplateColumns = '60px 1fr';
    calendarGrid.style.gap = '0';
    
    // Create time column and events column
    const timeColumn = document.createElement('div');
    timeColumn.className = 'day-view-time-column';
    
    const eventsColumn = document.createElement('div');
    eventsColumn.className = 'day-view-events-column';
    
    // Create hourly slots (00:00 to 23:00)
    for (let hour = 0; hour < 24; hour++) {
        // Time label
        const timeSlot = document.createElement('div');
        timeSlot.className = 'day-view-time-slot';
        const displayHour = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour;
        const ampm = hour < 12 ? 'AM' : 'PM';
        timeSlot.textContent = `${displayHour}:00 ${ampm}`;
        timeColumn.appendChild(timeSlot);
        
        // Event slot
        const eventSlot = document.createElement('div');
        eventSlot.className = 'day-view-event-slot';
        eventSlot.dataset.hour = hour;
        eventSlot.dataset.date = currentDate.toISOString().split('T')[0];
        
        // Add events and tasks for this hour
        const hourEvents = getEventsForHour(currentDate, hour);
        hourEvents.forEach(event => {
            const eventEl = createCalendarEventElement(event);
            eventSlot.appendChild(eventEl);
        });
        
        const hourTasks = getTasksForHour(currentDate, hour);
        hourTasks.forEach(task => {
            const taskEl = createCalendarTaskElement(task);
            eventSlot.appendChild(taskEl);
        });
        
        // Make droppable
        eventSlot.addEventListener('dragover', handleCalendarDragOver);
        eventSlot.addEventListener('drop', handleCalendarDrop);
        eventSlot.addEventListener('dragleave', handleCalendarDragLeave);
        eventSlot.addEventListener('dragenter', handleCalendarDragEnter);
        
        // Click to add event at this time
        eventSlot.addEventListener('click', (e) => {
            if (e.target === eventSlot && window.openEventModal) {
                const dateStr = currentDate.toISOString().split('T')[0];
                window.openEventModal(dateStr);
            }
        });
        
        eventsColumn.appendChild(eventSlot);
    }
    
    calendarGrid.appendChild(timeColumn);
    calendarGrid.appendChild(eventsColumn);
}

/**
 * Update header for day view
 */
function updateDayViewHeader(date) {
    const weekHeader = document.getElementById('week-header');
    if (!weekHeader) return;

    const dateStr = date.toLocaleDateString('en-US', { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
    });
    
    weekHeader.innerHTML = `
        <button class="week-nav-btn" onclick="window.previousDay()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
        </button>
        <div class="week-title">
            <h2>${dateStr}</h2>
        </div>
        <button class="week-nav-btn" onclick="window.nextDay()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
        </button>
        <button class="today-btn" onclick="window.goToToday()">Today</button>
    `;
}

/**
 * Navigate to previous day
 */
function previousDay() {
    if (!currentWeekStart) currentWeekStart = new Date();
    currentWeekStart.setDate(currentWeekStart.getDate() - 1);
    renderDayView();
}

/**
 * Navigate to next day
 */
function nextDay() {
    if (!currentWeekStart) currentWeekStart = new Date();
    currentWeekStart.setDate(currentWeekStart.getDate() + 1);
    renderDayView();
}

/**
 * Get events for specific hour
 */
function getEventsForHour(date, hour) {
    return calendarEvents.filter(event => {
        if (!event.start.dateTime) return false;
        const eventDate = new Date(event.start.dateTime);
        return isSameDay(eventDate, date) && eventDate.getHours() === hour;
    });
}

/**
 * Get tasks for specific hour
 */
function getTasksForHour(date, hour) {
    if (!window.tasks) return [];
    
    return window.tasks.filter(task => {
        if (!task.dueDate || task.deleted) return false;
        const taskDate = new Date(task.dueDate);
        return isSameDay(taskDate, date) && taskDate.getHours() === hour;
    });
}

/* ===================================================
   MONTH VIEW
   =================================================== */

/**
 * Render month view calendar
 */
async function renderMonthView() {
    const calendarGrid = document.getElementById('calendar-grid');
    if (!calendarGrid) return;

    const now = currentWeekStart || new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    
    // Fetch events for the month
    const monthStart = new Date(year, month, 1);
    const monthEnd = new Date(year, month + 1, 0, 23, 59, 59, 999);
    
    await fetchCalendarEvents(monthStart, monthEnd);

    // Update header
    updateMonthViewHeader(now);

    // Clear and reset grid for month view
    calendarGrid.innerHTML = '';
    calendarGrid.style.gridTemplateColumns = 'repeat(7, 1fr)';
    calendarGrid.style.gap = '1px';
    
    // Day name headers
    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    dayNames.forEach(name => {
        const header = document.createElement('div');
        header.className = 'month-view-day-header';
        header.textContent = name;
        calendarGrid.appendChild(header);
    });
    
    // Get first day of month and number of days
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();
    
    // Empty cells before month starts
    for (let i = 0; i < startingDayOfWeek; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'month-view-day-cell empty';
        calendarGrid.appendChild(emptyCell);
    }
    
    // Days of month
    const today = new Date();
    for (let day = 1; day <= daysInMonth; day++) {
        const date = new Date(year, month, day);
        const cell = document.createElement('div');
        cell.className = 'month-view-day-cell';
        cell.dataset.date = date.toISOString().split('T')[0];
        
        if (isSameDay(date, today)) {
            cell.classList.add('today');
        }
        
        // Day number
        const dayNumber = document.createElement('div');
        dayNumber.className = 'month-view-day-number';
        dayNumber.textContent = day;
        cell.appendChild(dayNumber);
        
        // Events container
        const eventsContainer = document.createElement('div');
        eventsContainer.className = 'month-view-events';
        
        const dayEvents = getEventsForDate(date);
        const dayTasks = getPlannedTasksForDate(date);
        
        // Show first 3 items, then "+N more"
        const allItems = [...dayEvents, ...dayTasks];
        const displayItems = allItems.slice(0, 3);
        const remainingCount = allItems.length - 3;
        
        displayItems.forEach(item => {
            const itemEl = document.createElement('div');
            itemEl.className = item.summary ? 'month-view-event' : 'month-view-task';
            itemEl.textContent = item.summary || item.text;
            itemEl.title = item.summary || item.text;
            
            if (item.id && item.summary && window.openEditEventModal) {
                itemEl.style.cursor = 'pointer';
                itemEl.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const fullEvent = calendarEvents.find(e => e.id === item.id);
                    if (fullEvent) window.openEditEventModal(fullEvent);
                });
            }
            
            eventsContainer.appendChild(itemEl);
        });
        
        if (remainingCount > 0) {
            const more = document.createElement('div');
            more.className = 'month-view-more';
            more.textContent = `+${remainingCount} more`;
            eventsContainer.appendChild(more);
        }
        
        cell.appendChild(eventsContainer);
        
        // Click to add event
        cell.addEventListener('click', (e) => {
            if (e.target === cell || e.target === dayNumber) {
                if (window.openEventModal) {
                    window.openEventModal(date.toISOString().split('T')[0]);
                }
            }
        });
        
        // Make droppable
        cell.addEventListener('dragover', handleCalendarDragOver);
        cell.addEventListener('drop', handleCalendarDrop);
        cell.addEventListener('dragleave', handleCalendarDragLeave);
        cell.addEventListener('dragenter', handleCalendarDragEnter);
        
        calendarGrid.appendChild(cell);
    }
}

/**
 * Update header for month view
 */
function updateMonthViewHeader(date) {
    const weekHeader = document.getElementById('week-header');
    if (!weekHeader) return;

    const monthYear = date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    
    weekHeader.innerHTML = `
        <button class="week-nav-btn" onclick="window.previousMonth()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
        </button>
        <div class="week-title">
            <h2>${monthYear}</h2>
        </div>
        <button class="week-nav-btn" onclick="window.nextMonth()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
        </button>
        <button class="today-btn" onclick="window.goToToday()">Today</button>
    `;
}

/**
 * Navigate to previous month
 */
function previousMonth() {
    if (!currentWeekStart) currentWeekStart = new Date();
    currentWeekStart.setMonth(currentWeekStart.getMonth() - 1);
    renderMonthView();
}

/**
 * Navigate to next month
 */
function nextMonth() {
    if (!currentWeekStart) currentWeekStart = new Date();
    currentWeekStart.setMonth(currentWeekStart.getMonth() + 1);
    renderMonthView();
}

/* ===================================================
   YEAR VIEW
   =================================================== */

/**
 * Render year view with 12 mini month calendars
 */
async function renderYearView() {
    const calendarGrid = document.getElementById('calendar-grid');
    if (!calendarGrid) return;

    const now = currentWeekStart || new Date();
    const year = now.getFullYear();
    
    // Fetch events for the entire year
    const yearStart = new Date(year, 0, 1);
    const yearEnd = new Date(year, 11, 31, 23, 59, 59, 999);
    
    await fetchCalendarEvents(yearStart, yearEnd);

    // Update header
    updateYearViewHeader(year);

    // Clear and reset grid for year view
    calendarGrid.innerHTML = '';
    calendarGrid.style.gridTemplateColumns = 'repeat(4, 1fr)';
    calendarGrid.style.gap = '24px';
    
    // Create 12 mini month calendars
    for (let month = 0; month < 12; month++) {
        const monthContainer = createYearViewMonth(year, month);
        calendarGrid.appendChild(monthContainer);
    }
}

/**
 * Create a mini month calendar for year view
 */
function createYearViewMonth(year, month) {
    const container = document.createElement('div');
    container.className = 'year-view-month';
    
    // Month name
    const monthName = new Date(year, month).toLocaleDateString('en-US', { month: 'long' });
    const header = document.createElement('div');
    header.className = 'year-view-month-header';
    header.textContent = monthName;
    container.appendChild(header);
    
    // Day headers
    const dayHeaders = document.createElement('div');
    dayHeaders.className = 'year-view-day-headers';
    const dayNames = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
    dayNames.forEach(name => {
        const dayHeader = document.createElement('div');
        dayHeader.textContent = name;
        dayHeaders.appendChild(dayHeader);
    });
    container.appendChild(dayHeaders);
    
    // Days grid
    const daysGrid = document.createElement('div');
    daysGrid.className = 'year-view-days-grid';
    
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();
    
    // Empty cells
    for (let i = 0; i < startingDayOfWeek; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'year-view-day empty';
        daysGrid.appendChild(emptyCell);
    }
    
    // Days
    const today = new Date();
    for (let day = 1; day <= daysInMonth; day++) {
        const date = new Date(year, month, day);
        const dayCell = document.createElement('div');
        dayCell.className = 'year-view-day';
        dayCell.textContent = day;
        dayCell.dataset.date = date.toISOString().split('T')[0];
        
        if (isSameDay(date, today)) {
            dayCell.classList.add('today');
        }
        
        // Check if day has events
        const dayEvents = getEventsForDate(date);
        const dayTasks = getPlannedTasksForDate(date);
        if (dayEvents.length > 0 || dayTasks.length > 0) {
            dayCell.classList.add('has-events');
            const dot = document.createElement('div');
            dot.className = 'year-view-event-dot';
            dayCell.appendChild(dot);
        }
        
        // Click to navigate to month view of this month
        dayCell.addEventListener('click', () => {
            currentWeekStart = new Date(year, month, day);
            currentView = 'month';
            document.querySelectorAll('.view-toggle-btn').forEach(btn => {
                btn.classList.toggle('active', btn.dataset.view === 'month');
            });
            renderMonthView();
        });
        
        daysGrid.appendChild(dayCell);
    }
    
    container.appendChild(daysGrid);
    return container;
}

/**
 * Update header for year view
 */
function updateYearViewHeader(year) {
    const weekHeader = document.getElementById('week-header');
    if (!weekHeader) return;
    
    weekHeader.innerHTML = `
        <button class="week-nav-btn" onclick="window.previousYear()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
        </button>
        <div class="week-title">
            <h2>${year}</h2>
        </div>
        <button class="week-nav-btn" onclick="window.nextYear()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
        </button>
        <button class="today-btn" onclick="window.goToToday()">Today</button>
    `;
}

/**
 * Navigate to previous year
 */
function previousYear() {
    if (!currentWeekStart) currentWeekStart = new Date();
    currentWeekStart.setFullYear(currentWeekStart.getFullYear() - 1);
    renderYearView();
}

/**
 * Navigate to next year
 */
function nextYear() {
    if (!currentWeekStart) currentWeekStart = new Date();
    currentWeekStart.setFullYear(currentWeekStart.getFullYear() + 1);
    renderYearView();
}

/* ===================================================
   INITIALIZATION
   =================================================== */

/**
 * Initialize calendar view
 */
function initCalendar() {
    currentWeekStart = getWeekStart();
    
    // Mark that initial load is complete after first render
    setTimeout(() => {
        isInitialLoad = false;
    }, 1000);
    
    renderCalendar();
    renderMiniCalendar();
    renderTodaySection();
    
    // Add view toggle listeners
    const viewToggleBtns = document.querySelectorAll('.view-toggle-btn');
    viewToggleBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            // Update active state
            viewToggleBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            // Switch view
            const view = btn.dataset.view;
            currentView = view;
            
            // Render appropriate view
            switch (view) {
                case 'day':
                    renderDayView();
                    break;
                case 'week':
                    renderCalendar();
                    break;
                case 'month':
                    renderMonthView();
                    break;
                case 'year':
                    renderYearView();
                    break;
                default:
                    renderCalendar();
            }
        });
    });
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
window.previousDay = previousDay;
window.nextDay = nextDay;
window.previousMonth = previousMonth;
window.nextMonth = nextMonth;
window.previousYear = previousYear;
window.nextYear = nextYear;
window.goToToday = goToToday;
window.goToDateFromMini = goToDateFromMini;
window.handleTaskDragStart = handleTaskDragStart;
window.handleTaskDragEnd = handleTaskDragEnd;
