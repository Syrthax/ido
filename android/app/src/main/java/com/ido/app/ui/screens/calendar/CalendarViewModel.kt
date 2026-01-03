package com.ido.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ido.app.data.model.Task
import com.ido.app.data.remote.CalendarDataSource
import com.ido.app.data.remote.CalendarEvent
import com.ido.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * ViewModel for Calendar screen
 * 
 * Data sources:
 * - Tasks: TaskRepository (synced from Drive)
 * - Events: Google Calendar API (fetched fresh, NOT cached)
 * 
 * CRITICAL: Calendar events are NOT persisted locally.
 * On every sync/refresh, old events are REPLACED, not merged.
 */
class CalendarViewModel(
    private val taskRepository: TaskRepository,
    private val calendarDataSource: CalendarDataSource
) : ViewModel() {
    
    // View mode: Week or Day
    private val _viewMode = MutableStateFlow(CalendarViewMode.WEEK)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()
    
    // Selected date for Day View
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    // Current week start date (Monday)
    private val _currentWeekStart = MutableStateFlow(getWeekStart(LocalDate.now()))
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart.asStateFlow()
    
    // Calendar events from Google Calendar (memory only, NO persistence)
    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()
    
    // Tasks with due dates for calendar display
    private val _tasksWithDueDate = MutableStateFlow<List<Task>>(emptyList())
    val tasksWithDueDate: StateFlow<List<Task>> = _tasksWithDueDate.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Selected calendar item for bottom sheet
    private val _selectedItem = MutableStateFlow<CalendarItem?>(null)
    val selectedItem: StateFlow<CalendarItem?> = _selectedItem.asStateFlow()
    
    // Create event mode
    private val _isCreatingEvent = MutableStateFlow(false)
    val isCreatingEvent: StateFlow<Boolean> = _isCreatingEvent.asStateFlow()
    
    // Create task mode
    private val _isCreatingTask = MutableStateFlow(false)
    val isCreatingTask: StateFlow<Boolean> = _isCreatingTask.asStateFlow()
    
    // Prefilled date/hour for create dialogs
    private val _prefilledDate = MutableStateFlow<LocalDate?>(null)
    val prefilledDate: StateFlow<LocalDate?> = _prefilledDate.asStateFlow()
    
    private val _prefilledHour = MutableStateFlow<Int?>(null)
    val prefilledHour: StateFlow<Int?> = _prefilledHour.asStateFlow()
    
    init {
        observeTasks()
    }
    
    /**
     * Observe tasks from repository and filter those with due dates
     */
    private fun observeTasks() {
        viewModelScope.launch {
            taskRepository.tasks.collect { tasks ->
                _tasksWithDueDate.value = tasks.filter { 
                    it.dueDate != null && !it.deleted 
                }
            }
        }
    }
    
    // ==================== VIEW MODE ====================
    
    /**
     * Switch to Day View for specific date
     */
    fun showDayView(date: LocalDate) {
        _selectedDate.value = date
        _viewMode.value = CalendarViewMode.DAY
        loadDayEvents(date)
    }
    
    /**
     * Switch back to Week View
     */
    fun showWeekView() {
        _viewMode.value = CalendarViewMode.WEEK
        loadWeekEvents()
    }
    
    /**
     * Toggle between Week and Day view
     */
    fun toggleViewMode() {
        when (_viewMode.value) {
            CalendarViewMode.WEEK -> showDayView(_selectedDate.value)
            CalendarViewMode.DAY -> showWeekView()
        }
    }
    
    // ==================== LOADING ====================
    
    /**
     * Load calendar events for current week
     * 
     * CRITICAL: This CLEARS all existing events before fetching new ones.
     * No merging, no deduplication - complete replacement.
     */
    fun loadWeekEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // CLEAR existing events BEFORE fetching
            _events.value = emptyList()
            
            val weekStart = _currentWeekStart.value
            // Week is Mon-Sat (6 days), so end is Saturday + 1 day = Sunday
            val weekEnd = weekStart.plusDays(6)
            
            android.util.Log.d("CalendarViewModel", "Loading events for week: $weekStart to $weekEnd")
            
            val fetchedEvents = calendarDataSource.fetchEvents(weekStart, weekEnd)
            
            if (fetchedEvents != null) {
                // REPLACE events entirely (no merge)
                _events.value = fetchedEvents
                android.util.Log.d("CalendarViewModel", "Loaded ${fetchedEvents.size} events")
            } else {
                _error.value = "Failed to load calendar events"
                android.util.Log.w("CalendarViewModel", "Failed to load events - calendar service may not be initialized")
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Load events for specific day
     */
    fun loadDayEvents(date: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // CLEAR existing events
            _events.value = emptyList()
            
            val fetchedEvents = calendarDataSource.fetchEvents(date, date.plusDays(1))
            
            if (fetchedEvents != null) {
                _events.value = fetchedEvents
            } else {
                _error.value = "Failed to load events"
            }
            
            _isLoading.value = false
        }
    }
    
    // ==================== NAVIGATION ====================
    
    /**
     * Navigate to previous week
     */
    fun previousWeek() {
        _currentWeekStart.value = _currentWeekStart.value.minusWeeks(1)
        loadWeekEvents()
    }
    
    /**
     * Navigate to next week
     */
    fun nextWeek() {
        _currentWeekStart.value = _currentWeekStart.value.plusWeeks(1)
        loadWeekEvents()
    }
    
    /**
     * Navigate to previous day (in Day View)
     */
    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        loadDayEvents(_selectedDate.value)
    }
    
    /**
     * Navigate to next day (in Day View)
     */
    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
        loadDayEvents(_selectedDate.value)
    }
    
    /**
     * Navigate to today
     */
    fun goToToday() {
        val today = LocalDate.now()
        when (_viewMode.value) {
            CalendarViewMode.WEEK -> {
                _currentWeekStart.value = getWeekStart(today)
                loadWeekEvents()
            }
            CalendarViewMode.DAY -> {
                _selectedDate.value = today
                loadDayEvents(today)
            }
        }
    }
    
    // ==================== ITEM SELECTION ====================
    
    /**
     * Select task to edit
     */
    fun selectTask(task: Task) {
        _selectedItem.value = CalendarItem.TaskItem(task)
    }
    
    /**
     * Select event to edit
     */
    fun selectEvent(event: CalendarEvent) {
        _selectedItem.value = CalendarItem.EventItem(event)
    }
    
    /**
     * Clear selection
     */
    fun clearSelection() {
        _selectedItem.value = null
        _isCreatingEvent.value = false
        _isCreatingTask.value = false
        _prefilledDate.value = null
        _prefilledHour.value = null
    }
    
    // ==================== CREATE ====================
    
    /**
     * Show create event sheet
     */
    fun showCreateEvent(date: LocalDate? = null, hour: Int? = null) {
        _prefilledDate.value = date ?: _selectedDate.value
        _prefilledHour.value = hour
        _isCreatingEvent.value = true
        _selectedItem.value = null
    }
    
    /**
     * Show create task sheet
     */
    fun showCreateTask(date: LocalDate? = null, hour: Int? = null) {
        _prefilledDate.value = date ?: _selectedDate.value
        _prefilledHour.value = hour
        _isCreatingTask.value = true
        _selectedItem.value = null
    }
    
    /**
     * Create new Google Calendar event
     */
    fun createEvent(
        title: String,
        description: String?,
        startTime: Instant,
        endTime: Instant,
        isAllDay: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val created = calendarDataSource.createEvent(
                title = title,
                description = description,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay
            )
            
            if (created != null) {
                // Refresh events to show new event
                refreshEvents()
            } else {
                _error.value = "Failed to create event"
            }
            
            _isLoading.value = false
            clearSelection()
        }
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Update Google Calendar event
     */
    fun updateEvent(
        event: CalendarEvent,
        title: String,
        description: String?,
        startTime: Instant,
        endTime: Instant,
        isAllDay: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val updated = calendarDataSource.updateEvent(
                eventId = event.id,
                title = title,
                description = description,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay
            )
            
            if (updated != null) {
                // Refresh events
                refreshEvents()
            } else {
                _error.value = "Failed to update event"
            }
            
            _isLoading.value = false
            clearSelection()
        }
    }
    
    // ==================== DELETE ====================
    
    /**
     * Delete Google Calendar event
     */
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val deleted = calendarDataSource.deleteEvent(eventId)
            
            if (deleted) {
                // Refresh events
                refreshEvents()
            } else {
                _error.value = "Failed to delete event"
            }
            
            _isLoading.value = false
            clearSelection()
        }
    }
    
    // ==================== REFRESH ====================
    
    /**
     * Refresh events (called after Drive sync completes)
     * 
     * CRITICAL: This is the cache fix.
     * 1. CLEAR all events
     * 2. Fetch fresh from Google Calendar
     * 3. Replace state entirely
     */
    fun refreshEvents() {
        when (_viewMode.value) {
            CalendarViewMode.WEEK -> loadWeekEvents()
            CalendarViewMode.DAY -> loadDayEvents(_selectedDate.value)
        }
    }
    
    /**
     * Clear all events (for sign out)
     */
    fun clearEvents() {
        _events.value = emptyList()
        _tasksWithDueDate.value = emptyList()
    }
    
    // ==================== HELPERS ====================
    
    /**
     * Get tasks for a specific date
     */
    fun getTasksForDate(date: LocalDate): List<Task> {
        val zone = ZoneId.systemDefault()
        return _tasksWithDueDate.value.filter { task ->
            task.dueDate?.let { dueDateStr ->
                try {
                    val taskDate = Instant.parse(dueDateStr)
                        .atZone(zone)
                        .toLocalDate()
                    taskDate == date
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }
    }
    
    /**
     * Get events for a specific date
     */
    fun getEventsForDate(date: LocalDate): List<CalendarEvent> {
        val zone = ZoneId.systemDefault()
        return _events.value.filter { event ->
            val eventDate = event.startTime.atZone(zone).toLocalDate()
            eventDate == date
        }
    }
    
    /**
     * Get week range formatted string (e.g., "Jan 1 - Jan 6")
     */
    fun getWeekRangeString(): String {
        val weekStart = _currentWeekStart.value
        val weekEnd = weekStart.plusDays(5) // 6 days, so +5 from Monday
        
        val formatter = DateTimeFormatter.ofPattern("MMM d")
        return "${formatter.format(weekStart)} – ${formatter.format(weekEnd)}"
    }
    
    /**
     * Get formatted date string for Day View
     */
    fun getSelectedDateString(): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
        return formatter.format(_selectedDate.value)
    }
    
    companion object {
        /**
         * Get Monday of the week containing the given date
         * Week starts on Monday per requirements
         */
        fun getWeekStart(date: LocalDate): LocalDate {
            return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
    }
}
