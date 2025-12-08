# iDo 2.0 - Developer Quick Reference

## 🎯 Quick Start

```bash
# 1. Update config.js with Calendar scope
scope: '...existing-scopes... https://www.googleapis.com/auth/calendar.readonly'

# 2. Enable Calendar API in Google Cloud Console
# 3. Deploy files to server
# 4. Test login and permissions
```

---

## 📁 New Files

| File | Purpose | Size |
|------|---------|------|
| `calendar.js` | Google Calendar integration | ~620 lines |
| `IDO_2.0_GUIDE.md` | User documentation | ~400 lines |
| `IDO_2.0_SETUP.md` | Deployment guide | ~300 lines |
| `IDO_2.0_ARCHITECTURE.md` | Technical diagrams | ~350 lines |
| `IDO_2.0_IMPLEMENTATION_SUMMARY.md` | Change summary | ~500 lines |

---

## 🔧 Modified Files

| File | Changes | Key Updates |
|------|---------|-------------|
| `index.html` | Three-pane layout | New sidebar divs, calendar grid |
| `style.css` | Calendar styles | ~800 lines of new CSS |
| `app.js` | Sidebar rendering | New render functions, global exports |
| `config.example.js` | Calendar scope | Added calendar.readonly |

---

## 🌐 Global Functions

### Exported from app.js
```javascript
window.toggleTask(id)         // Toggle task completion
window.renderTasksSidebar()   // Re-render task lists
window.saveTasksToCloud()     // Save to Google Drive
window.tasks = [...]          // Task array (shared state)
```

### Exported from calendar.js
```javascript
window.initCalendar()         // Initialize calendar on load
window.renderCalendar()       // Re-render weekly view
window.renderMiniCalendar()   // Re-render mini month
window.renderTodaySection()   // Re-render today panel
window.previousWeek()         // Navigate to previous week
window.nextWeek()             // Navigate to next week
window.goToToday()            // Jump to current week
window.goToDateFromMini(y,m,d) // Jump to specific date
window.handleTaskDragStart(e) // Drag start handler
window.handleTaskDragEnd(e)   // Drag end handler
```

---

## 📦 Key Components

### Left Sidebar
```html
<aside class="left-sidebar">
  <div id="mini-calendar"></div>
  <div id="today-section"></div>
  <div class="filters-section"></div>
</aside>
```

### Center Area
```html
<main class="center-area">
  <div id="week-header"></div>
  <div id="calendar-grid"></div>
</main>
```

### Right Sidebar
```html
<aside class="right-sidebar">
  <div id="profile-info"></div>
  <div class="tasks-sidebar-header"></div>
  <div id="add-task-section"></div>
  <div id="planned-tasks-container"></div>
  <div id="unplanned-tasks-container"></div>
</aside>
```

---

## 🎨 CSS Classes

### Layout
```css
.three-pane-layout        /* Grid: 280px | 1fr | 320px */
.left-sidebar             /* Left panel */
.center-area              /* Calendar area */
.right-sidebar            /* Tasks panel */
.bottom-bar               /* Sync status */
```

### Calendar
```css
.calendar-grid            /* 6-column grid (Mon-Sat) */
.calendar-day-column      /* Individual day */
.calendar-day-column.today /* Today indicator */
.calendar-day-column.drag-over /* Drag feedback */
.calendar-event           /* Google Calendar event */
.calendar-task            /* iDo task on calendar */
```

### Mini Calendar
```css
.mini-calendar            /* Container */
.mini-calendar-grid       /* 7x6 grid */
.mini-calendar-day        /* Individual date */
.mini-calendar-day.today  /* Today highlight */
```

### Tasks
```css
.tasks-section            /* Planned/Unplanned container */
.tasks-list               /* Task items container */
.task-item                /* Individual task */
.task-item.completed      /* Completed task */
.task-item.overdue        /* Overdue task */
.task-item.dragging       /* Being dragged */
```

---

## 🔄 Data Flow

### Task Creation
```javascript
// 1. User inputs task
const newTask = TaskSchema.createTask(text, { dueDate, reminderTime });

// 2. Add to array
tasks.push(newTask);
window.tasks = tasks;

// 3. Render
renderTasksSidebar();

// 4. Save
await saveTasksToCloud();

// 5. Update calendar
renderCalendar();
```

### Drag and Drop
```javascript
// 1. Drag start
draggedTaskId = taskElement.dataset.taskId;

// 2. Drop on calendar
const newDate = dayColumn.dataset.date;

// 3. Update task
await updateTaskDate(taskId, newDate);

// 4. Re-render
renderCalendar();
renderTasksSidebar();
```

### Calendar Load
```javascript
// 1. Get week range
const weekStart = getWeekStart();
const weekDays = getWeekDays(weekStart);

// 2. Fetch events
const events = await fetchCalendarEvents(weekStart, weekEnd);

// 3. Render grid
weekDays.forEach(day => {
  createDayColumn(day); // Events + tasks
});
```

---

## 🔐 OAuth Scopes

Required scopes in `config.js`:
```javascript
scope: 'https://www.googleapis.com/auth/drive.file ' +
       'https://www.googleapis.com/auth/userinfo.email ' +
       'https://www.googleapis.com/auth/userinfo.profile ' +
       'https://www.googleapis.com/auth/calendar.readonly'
```

---

## 🐛 Common Issues & Fixes

### Calendar events not showing
```javascript
// Check API enabled
console.log('Calendar API enabled?');

// Verify scope
console.log(CONFIG.scope.includes('calendar.readonly'));

// Check console for errors
// Look for 403 or 401 errors
```

### Drag-and-drop not working
```javascript
// Verify handlers attached
console.log('Drag handlers:', {
  start: typeof window.handleTaskDragStart,
  end: typeof window.handleTaskDragEnd
});

// Check if calendar.js loaded
console.log('Calendar loaded:', typeof window.initCalendar);
```

### Tasks not syncing
```javascript
// Check save status
console.log('Is saving:', isSaving);

// Verify Drive API
console.log('Drive API:', window.DriveAPI);

// Manual sync
await window.saveTasksToCloud();
```

---

## 📊 State Management

### Global State Variables
```javascript
// app.js
let tasks = [];              // All tasks
let isSaving = false;        // Save lock
let currentDueDate = null;   // Current date picker value

// calendar.js
let calendarEvents = [];     // Google Calendar events
let currentWeekStart = null; // Currently displayed week
```

### Shared State
```javascript
window.tasks = tasks;  // Shared between app.js and calendar.js
```

---

## 🎯 Rendering Pipeline

### On Task Change
```javascript
// 1. Modify task
tasks[index] = TaskSchema.updateTask(task, changes);

// 2. Update global
window.tasks = tasks;

// 3. Re-render sidebar
renderTasksSidebar();

// 4. Re-render calendar
if (window.renderCalendar) renderCalendar();

// 5. Re-render today
if (window.renderTodaySection) renderTodaySection();

// 6. Save
await saveTasksToCloud();
```

### On Week Change
```javascript
// 1. Update week start
currentWeekStart.setDate(currentWeekStart.getDate() + 7);

// 2. Fetch new events
await fetchCalendarEvents(start, end);

// 3. Re-render calendar
renderCalendar();

// 4. Update header
updateWeekHeader(weekDays);
```

---

## 🧪 Testing Checklist

### Manual Tests
```
□ Login with Google (new Calendar permission)
□ Calendar events display
□ Tasks split into planned/unplanned
□ Drag unplanned → calendar (assigns date)
□ Drag planned → different day (reschedules)
□ Complete task (updates all views)
□ Delete task (removes from all views)
□ Add task with date (appears planned + calendar)
□ Add task without date (appears unplanned)
□ Week navigation (prev/next/today)
□ Mini calendar click (jumps to week)
□ Today section shows events/tasks
□ Sync status updates
□ Manual sync works
```

### Browser Tests
```
□ Chrome
□ Firefox
□ Safari
□ Edge
□ Mobile Safari
□ Mobile Chrome
```

---

## 🚀 Deployment Commands

### Via Git
```bash
git add .
git commit -m "Upgrade to iDo 2.0"
git push origin main
```

### Manual Upload
```bash
# Upload these files:
web/index.html
web/style.css
web/app.js
web/calendar.js  # NEW
web/config.js    # Updated scope
```

---

## 📞 Debug Commands

### Browser Console
```javascript
// Check initialization
console.log('Tasks:', window.tasks);
console.log('Calendar events:', window.calendarEvents);
console.log('Current week:', window.currentWeekStart);

// Test functions
window.renderCalendar();
window.renderTasksSidebar();
window.goToToday();

// Check state
console.log('Is saving:', isSaving);
console.log('Access token:', localStorage.getItem('access_token'));

// Force sync
await window.saveTasksToCloud();

// Check permissions
console.log('Scope:', CONFIG.scope);
```

---

## 📝 Code Snippets

### Add a Calendar Event Manually (for testing)
```javascript
// This is READ-ONLY in iDo 2.0
// Use Google Calendar app to add events
// They will appear automatically in iDo
```

### Force Re-render Everything
```javascript
window.renderTasksSidebar();
window.renderCalendar();
window.renderMiniCalendar();
window.renderTodaySection();
```

### Clear All Data (reset)
```javascript
// WARNING: Deletes everything!
localStorage.clear();
window.tasks = [];
location.reload();
```

---

## 🎨 Customization

### Change Week Start Day
```javascript
// In calendar.js, modify getWeekStart()
// Current: Monday (day 1)
// To start Sunday: change diff calculation
```

### Add Sunday to Calendar
```javascript
// In calendar.js, change:
for (let i = 0; i < 7; i++) { // Was 6
  // Add Sunday column
}
```

### Change Colors
```css
/* In style.css */
:root {
  --accent: #2563eb;  /* Change to your color */
}
```

---

## 📚 API References

### Google Calendar API
```
Endpoint: https://www.googleapis.com/calendar/v3/calendars/primary/events
Method: GET
Params: timeMin, timeMax, singleEvents, orderBy
Returns: { items: [...events] }
```

### Google Drive API
```
Endpoint: https://www.googleapis.com/drive/v3/files/{fileId}
Method: GET (read), PATCH (update)
Returns: File content or metadata
```

---

## ✅ Production Checklist

```
□ Google Cloud Console updated (Calendar API enabled)
□ OAuth consent screen has Calendar scope
□ config.js has correct credentials
□ All files deployed
□ HTTPS enabled (required for OAuth)
□ Test login works
□ Test calendar events appear
□ Test drag-and-drop
□ Check mobile responsiveness
□ Monitor error logs
□ Set up quota alerts
□ Document any custom changes
```

---

## 🎊 That's It!

You now have everything you need to understand, deploy, and maintain iDo 2.0.

**Quick Links:**
- User Guide: `IDO_2.0_GUIDE.md`
- Setup: `IDO_2.0_SETUP.md`
- Architecture: `IDO_2.0_ARCHITECTURE.md`
- Implementation: `IDO_2.0_IMPLEMENTATION_SUMMARY.md`

**Support:** [@i._._.sarthak](https://instagram.com/i._._.sarthak)

---

**Happy Coding! 🚀**
