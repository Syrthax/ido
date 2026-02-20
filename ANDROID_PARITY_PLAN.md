# Android iDo Parity Implementation Plan

Complete technical specification for achieving 100% feature parity between Android and Web versions.

---

## Part 1: Current Android State vs Web

### What Android HAS (Matching Web)

- Task Schema v2.0 (id, text, done, priority, dueDate, reminderTime, notified, createdAt, updatedAt, deleted)
- UUID v4 generation
- ISO-8601 timestamps (Instant.toString())
- Soft delete (deleted: Boolean)
- Priority/Pinning toggle
- Task completion (done toggle)
- Due dates (ISO-8601)
- Reminders via WorkManager
- Google OAuth with PKCE
- Google Drive sync (ido-data.json)
- Last-write-wins conflict resolution (updatedAt comparison)
- Natural language date parsing
- Notification scheduling

### What Android is MISSING vs Web

#### CRITICAL GAPS (P0)

1. Calendar View (Day/Week/Month/Year) - NONE
2. Google Calendar Integration - NONE
3. Planned vs Unplanned sections - Single list only
4. Today section - NONE
5. Drag tasks to calendar - NONE
6. Event modal (create/edit Google Calendar events) - NONE
7. Bottom navigation (Tasks/Calendar/Settings) - Single screen
8. Mini month calendar - NONE
9. Week view (Mon-Sat, 6 days) - NONE

#### MINOR GAPS (P1)

1. Settings screen last synced timestamp - Missing
2. Profile avatar display - Shows email only
3. Sync status detail - Icons only, no text
4. Filter buttons (Personal/Work) - UI exists, no logic

---

## Part 2: Missing Features Detailed

### 1. Calendar System

Web Has:
- 4 views: Day / Week / Month / Year
- Google Calendar API integration
- Time grid with hourly slots (Day view)
- Events render on calendar
- Tasks render on calendar (if dueDate set)
- Drag tasks from sidebar to calendar to set dueDate
- Mini month calendar in left sidebar
- "Today" section showing today's events + tasks
- Week = Monday-Saturday (6 days)

Android Needs:
- Complete calendar UI layer
- Google Calendar API client integration
- Event modal for CRUD operations
- Drag-and-drop support
- Date navigation (prev/next/today buttons)
- Real-time event fetching

### 2. Navigation Architecture

Current Android:
- Single screen with task list
- FAB for new task
- No bottom navigation

Target Android (Per Sketch):
- Bottom navigation bar (Tasks / Calendar / Settings)
- Three separate screens
- Tab switching

### 3. Tasks Screen Restructuring

Current:
- All tasks in single list
- Sorted by priority
- No sections

Target:
- Priority section (priority: true)
- Today section (dueDate = today)
- Later section (dueDate > today)
- Unscheduled section (dueDate = null)

### 4. Google Calendar Integration

Needs:
- Calendar API dependency
- calendar.events OAuth scope
- Event creation/editing/deletion
- Event modal with full forms
- Recurrence rule (RRULE) support

### 5. Settings Screen

Current:
- Email display
- Sign out button
- Sync now button

Target:
- Profile card (name, email, avatar)
- Last synced timestamp
- Last synced time display
- Sync now button
- Log out button

---

## Part 3: Step-by-Step Implementation Plan

### Phase 1: Navigation & Screen Structure (2 days)

Create Tab enum:
```
enum class Tab { TASKS, CALENDAR, SETTINGS }
```

Refactor MainActivity:
- Replace single-screen with NavigationBar
- Add tab state management
- Wire up screen switching

Create placeholder screens:
- ui/screens/tasks/TasksScreen.kt
- ui/screens/calendar/CalendarScreen.kt (empty)
- Extract SettingsScreen to new file

Deliverable: App with 3 tabs, Calendar tab shows "Coming soon"

---

### Phase 2: Tasks Screen Restructuring (1 day)

Update TaskRepository:
```
fun getTasksBySection(): Map<TaskSection, List<Task>>
enum class TaskSection { PRIORITY, TODAY, LATER, UNSCHEDULED }
```

Update TasksScreen UI:
- Add section headers
- Filter tasks by section
- Group rendering

Logic:
- Priority: tasks.filter { it.priority }
- Today: tasks.filter { dueDate == today }
- Later: tasks.filter { dueDate > today }
- Unscheduled: tasks.filter { dueDate == null }

Deliverable: Tasks screen with 4 sections matching web

---

### Phase 3: Calendar Data Layer (2 days)

Add dependency to app/build.gradle.kts:
```
implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")
```

Update OAuth scopes in DriveDataSource.kt:
```
.requestScopes(
    Scope(DriveScopes.DRIVE_FILE),
    Scope("https://www.googleapis.com/auth/calendar.events")
)
```

Create new files:
- data/remote/CalendarDataSource.kt
- domain/model/CalendarEvent.kt

Implement CalendarDataSource methods:
- initializeCalendarService(account: GoogleSignInAccount)
- fetchEvents(startDate: Instant, endDate: Instant): List<CalendarEvent>
- createEvent(event: CalendarEvent): CalendarEvent
- updateEvent(eventId: String, event: CalendarEvent): CalendarEvent
- deleteEvent(eventId: String): Boolean

Deliverable: Calendar API fully functional, events can be fetched

---

### Phase 4: Calendar UI - Week View (3 days)

Create new files:
- ui/screens/calendar/CalendarViewModel.kt
- ui/screens/calendar/WeekView.kt
- ui/screens/calendar/components/DayHeader.kt
- ui/screens/calendar/components/DayColumn.kt

CalendarViewModel methods:
- loadWeek()
- nextWeek()
- previousWeek()
- goToToday()
- getWeekStart(): Instant

WeekView logic:
- Display Mon-Sat (6 days only)
- Hourly time slots
- Render events and tasks
- Drag-and-drop support

Drag-and-drop:
- When task dropped on day/hour, update task.dueDate
- Trigger sync after update

Deliverable: Fully functional Week View with events and tasks

---

### Phase 5: Calendar UI - Other Views (2 days)

Day View:
- Single column
- 24-hour timeline
- Similar to week but one day only

Month View:
- Traditional grid layout
- 7 columns (Sun-Sat)
- Show first 3 events/tasks per day
- "+N more" indicator for overflow

Year View:
- 4 columns, 3 rows (12 months)
- Mini calendars for each month
- Dots indicating days with events
- Click day to navigate to month view

Create files:
- ui/screens/calendar/DayView.kt
- ui/screens/calendar/MonthView.kt
- ui/screens/calendar/YearView.kt

Deliverable: All 4 calendar views functional

---

### Phase 6: Event Modal (2 days)

Create file:
- ui/screens/calendar/EventSheet.kt

Event creation form fields:
- Title (required)
- Description (optional)
- Start date
- Start time
- End date
- End time
- All-day toggle
- Recurring toggle
- Recurrence type dropdown (Daily, Weekly, Monthly, Yearly)
- Recurrence count input

Buttons:
- Delete (if editing)
- Cancel
- Save

Wire to CalendarViewModel:
- createEvent(event: CalendarEvent)
- updateEvent(eventId: String, event: CalendarEvent)
- deleteEvent(eventId: String)

Deliverable: Full event CRUD via modal

---

### Phase 7: Mini Calendar & Today Section (1 day)

Create files:
- ui/screens/calendar/MiniCalendar.kt
- ui/screens/calendar/TodaySection.kt

MiniCalendar:
- Month grid layout
- Current month focused
- Clickable days to jump to date
- Today highlighted

TodaySection:
- Shows today's date
- Lists today's calendar events
- Lists today's tasks (dueDate = today)
- Empty state if nothing scheduled

Update CalendarScreen layout:
- Left sidebar: MiniCalendar + TodaySection
- Center: Calendar view (Day/Week/Month/Year)
- Bottom: Unscheduled tasks section

Deliverable: Complete calendar UI matching web

---

### Phase 8: Settings Enhancement (0.5 days)

Update SyncManager:
- Track lastSyncTime: Instant?
- Update on every sync completion
- Persist to SharedPreferences

Update SettingsScreen:
- Display last sync timestamp
- Format as human-readable time
- Show "Never" if no sync yet

Deliverable: Settings screen matches web functionality

---

### Phase 9: Testing & Polish (2 days)

Test checklist:
- [ ] All 4 calendar views render correctly
- [ ] Tasks appear on calendar when dueDate set
- [ ] Drag task to calendar updates dueDate
- [ ] Google Calendar events sync correctly
- [ ] Event CRUD operations work
- [ ] Recurring events display properly
- [ ] Week view = Mon-Sat (6 days) matches web
- [ ] Today section updates in real-time
- [ ] Last sync timestamp persists across app restarts
- [ ] OAuth includes calendar.events scope
- [ ] Sign out clears all data
- [ ] No duplicate Drive files created
- [ ] All timestamps are ISO-8601
- [ ] Last-write-wins conflict resolution works
- [ ] Notification scheduling matches web behavior
- [ ] Profile info displays correctly
- [ ] Sync status visible in settings

Deliverable: 100% feature parity achieved

---

## Part 4: UI Component Mapping

Web Component → Android Equivalent

Login Screen (index.html 16-36) → MainActivity.kt (exists)

Left Sidebar (index.html 61-75) → CREATE: ui/screens/calendar/LeftSidebar.kt
- Mini Calendar (calendar.js 408-443) → CREATE: ui/screens/calendar/MiniCalendar.kt
- Today Section (calendar.js 445-501) → CREATE: ui/screens/calendar/TodaySection.kt
- Filters (index.html 67-72) → SKIP (non-functional in web)

Center Calendar (index.html 90-107) → CREATE: ui/screens/calendar/CalendarScreen.kt
- View Toggle (index.html 92-97) → CREATE: ui/components/ViewToggle.kt
- Week View (calendar.js 185-228) → CREATE: ui/screens/calendar/WeekView.kt
- Day View (calendar.js 684-789) → CREATE: ui/screens/calendar/DayView.kt
- Month View (calendar.js 845-985) → CREATE: ui/screens/calendar/MonthView.kt
- Year View (calendar.js 1001-1125) → CREATE: ui/screens/calendar/YearView.kt

Right Sidebar Tasks (index.html 112-211) → REFACTOR: ui/screens/tasks/TasksScreen.kt
- Profile Info (app.js 141-153) → REFACTOR: Move to SettingsScreen
- Add Task Input (index.html 127-192) → REFACTOR: ui/screens/edit/EditTaskSheet.kt
- Planned Tasks (app.js 333-357) → CREATE: Section in TasksScreen
- Unplanned Tasks (app.js 359-371) → CREATE: Section in TasksScreen

Event Modal (index.html 236-309) → CREATE: ui/screens/calendar/EventSheet.kt

Bottom Bar Sync (index.html 215-227) → MOVE: To Settings tab

Bottom Navigation → CREATE: MainActivity.kt

---

## Part 5: Final Parity Checklist

### Core Features

Task Schema v2.0 - Web: ✅ Android: ✅ MATCH
UUID v4 - Web: ✅ Android: ✅ MATCH
ISO-8601 Timestamps - Web: ✅ Android: ✅ MATCH
Soft Delete - Web: ✅ Android: ✅ MATCH
Priority (Pinning) - Web: ✅ Android: ✅ MATCH
Task Completion - Web: ✅ Android: ✅ MATCH
Due Dates - Web: ✅ Android: ✅ MATCH
Reminders - Web: ✅ Android: ✅ MATCH
Natural Language Parsing - Web: ✅ Android: ✅ MATCH

### Authentication & Sync

Google OAuth - Web: ✅ PKCE Android: ✅ PKCE MATCH
Drive File Sync - Web: ✅ ido-data.json Android: ✅ ido-data.json MATCH
Calendar Scope - Web: ✅ calendar.events Android: ❌ MISSING → PHASE 3
Last-Write-Wins - Web: ✅ updatedAt Android: ✅ updatedAt MATCH
Duplicate File Cleanup - Web: ✅ Android: ✅ MATCH
Token Refresh - Web: ✅ Android: ✅ MATCH
Logout Clears Data - Web: ✅ Android: ✅ MATCH

### Calendar System

Day View - Web: ✅ 24-hour Android: ❌ → PHASE 5
Week View (Mon-Sat) - Web: ✅ 6 days Android: ❌ → PHASE 4
Month View - Web: ✅ Grid Android: ❌ → PHASE 5
Year View - Web: ✅ 12 minis Android: ❌ → PHASE 5
Google Calendar Events - Web: ✅ Fetch/CRUD Android: ❌ → PHASE 3
Event Modal - Web: ✅ Full CRUD Android: ❌ → PHASE 6
Recurring Events - Web: ✅ RRULE Android: ❌ → PHASE 6
Drag Task to Calendar - Web: ✅ Sets dueDate Android: ❌ → PHASE 4
Mini Month Calendar - Web: ✅ Sidebar Android: ❌ → PHASE 7
Today Section - Web: ✅ Shows items Android: ❌ → PHASE 7

### UI Structure

Bottom Navigation - Web: N/A Android: ❌ → PHASE 1
Tasks Tab - Web: ✅ Right sidebar Android: ⚠️ Single → PHASE 2
Calendar Tab - Web: ✅ Center Android: ❌ → PHASES 4-7
Settings Tab - Web: ✅ Modal Android: ⚠️ Partial → PHASE 8
Planned vs Unplanned - Web: ✅ Sections Android: ❌ Mixed → PHASE 2
Priority Section - Web: ✅ Top Android: ❌ → PHASE 2
Today Section - Web: ✅ Sidebar Android: ❌ → PHASE 7
Later Section - Web: ✅ Planned Android: ❌ → PHASE 2

### Settings Screen

Profile Info - Web: ✅ Name+Email+Avatar Android: ⚠️ Email only → PHASE 8
Last Synced Timestamp - Web: ✅ Bottom bar Android: ❌ → PHASE 8
Sync Now Button - Web: ✅ Android: ✅ DONE
Logout Button - Web: ✅ Android: ✅ DONE

### Explicitly NOT Required

- Offline Mode (not in web, not in scope)
- IndexedDB / Room DB (web uses localStorage only for tokens)
- Categories / Tags (web UI non-functional)
- Search (not in web)
- Push Notifications (only reminders, already done)
- Undo/Redo (not in web)
- Collaboration (not in scope)

---

## Part 6: Time Estimate Summary

Phase 1: Navigation - 2 days - No dependencies
Phase 2: Tasks Screen - 1 day - Depends on Phase 1
Phase 3: Calendar API - 2 days - Depends on Phase 1
Phase 4: Week View - 3 days - Depends on Phase 3
Phase 5: Other Views - 2 days - Depends on Phase 4
Phase 6: Event Modal - 2 days - Depends on Phase 3
Phase 7: Mini Calendar - 1 day - Depends on Phase 4
Phase 8: Settings - 0.5 days - No dependencies
Phase 9: Testing - 2 days - Depends on all

TOTAL: ~15.5 days (3 weeks)

---

## Part 7: Critical Stability Notes

### 1. Week Length Must Be 6 Days (Mon-Sat)

DO NOT:
```
private fun getWeekDays(weekStart: Instant): List<Instant> {
    return (0..6).map { weekStart.plus(it.toLong(), ChronoUnit.DAYS) } // 7 days - WRONG
}
```

DO:
```
private fun getWeekDays(weekStart: Instant): List<Instant> {
    return (0..5).map { weekStart.plus(it.toLong(), ChronoUnit.DAYS) } // 6 days - CORRECT
}
```

### 2. OAuth Scope Order Matters

CORRECT:
```
.requestScopes(
    Scope(DriveScopes.DRIVE_FILE),
    Scope("https://www.googleapis.com/auth/calendar.events")
)
```

WRONG - Will break existing users:
```
.requestScopes(
    Scope("https://www.googleapis.com/auth/calendar.events"),
    Scope(DriveScopes.DRIVE_FILE)
)
```

### 3. No Global Mutable State

AVOID:
```
object GlobalTasks {
    var tasks: List<Task> = emptyList() // Leaks across screens
}
```

USE:
```
class TaskRepository {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
}
```

### 4. Timezone Safety

ALWAYS:
```
val localTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
```

NEVER:
```
val utcTime = instant.atZone(ZoneOffset.UTC).toLocalDateTime() // Wrong for display
```

### 5. Drive File Deduplication

Web strategy (already correct in Android):
```
// Keep most recently modified file, delete others
val files = result.files.sortedByDescending { it.modifiedTime }
val keepFile = files.first()
files.drop(1).forEach { deleteFile(it.id) }
```

---

## Part 8: Final Deliverable

After completing all 9 phases, Android app will have:

100% Feature Parity with Web:
- Same task schema, same sync logic, same OAuth flow
- Calendar with 4 views (Day/Week/Month/Year)
- Google Calendar event integration
- Drag-and-drop task scheduling
- Planned/Unplanned task sections
- Last sync timestamp
- Profile display in settings

Material 3 Expressive UI (per sketch):
- Bottom navigation (Tasks / Calendar / Settings)
- Sectioned task list (Priority / Today / Later / Unscheduled)
- Full calendar grid layouts
- Event creation modal

No Scope Creep:
- No offline mode (matches web)
- No categories (web has non-functional UI)
- No search (not in web)
- No backend (serverless architecture preserved)

---

## Implementation Notes

Start with Phase 1 (Navigation) and proceed sequentially. Each phase is independently testable.

Focus on matching web behavior exactly, not reinterpreting it.

All dates must be ISO-8601 strings (Instant.toString()).

All timestamps must use UTC internally and display in local timezone.

Soft delete must be implemented (deleted: Boolean), not physical deletion.

Last-write-wins uses updatedAt timestamp comparison.

Week view MUST be Mon-Sat (6 days), not Sun-Sat or other variants.

OAuth must include both drive.file and calendar.events scopes.

---

END OF PLAN
