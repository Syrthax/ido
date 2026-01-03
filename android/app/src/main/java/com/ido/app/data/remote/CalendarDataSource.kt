package com.ido.app.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.Events
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Google Calendar event data class
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: Instant,
    val endTime: Instant,
    val isAllDay: Boolean,
    val location: String?,
    val calendarId: String,
    val recurrence: List<String>? = null
)

/**
 * Google Calendar remote data source
 * Handles fetching and mutating calendar events from Google Calendar API
 * 
 * CRUD operations:
 * - Create: Insert new event
 * - Read: Fetch events for date range
 * - Update: Patch existing event
 * - Delete: Remove event
 * 
 * IMPORTANT: Events are NOT stored locally. All operations go directly to Google Calendar API.
 */
class CalendarDataSource(private val context: Context) {
    
    private var calendarService: Calendar? = null
    
    companion object {
        private const val APP_NAME = "iDo"
        private const val PRIMARY_CALENDAR = "primary"
    }
    
    /**
     * Initialize Calendar service with signed-in account
     */
    fun initializeCalendarService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(CALENDAR_EVENTS_SCOPE)
        )
        credential.selectedAccount = account.account
        
        calendarService = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
    }
    
    /**
     * Check if calendar service is initialized
     */
    fun isInitialized(): Boolean {
        return calendarService != null && GoogleSignIn.getLastSignedInAccount(context) != null
    }
    
    /**
     * Fetch events for a date range
     * 
     * @param startDate Start of range (inclusive)
     * @param endDate End of range (exclusive)
     * @return List of calendar events, or null if not signed in
     */
    suspend fun fetchEvents(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CalendarEvent>? = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: run {
                android.util.Log.w("CalendarDataSource", "Calendar service not initialized")
                return@withContext null
            }
            
            val zone = ZoneId.systemDefault()
            val startDateTime = startDate.atStartOfDay(zone).toInstant()
            val endDateTime = endDate.atStartOfDay(zone).toInstant()
            
            android.util.Log.d("CalendarDataSource", "Fetching events from $startDate to $endDate")
            
            // Fetch events from primary calendar
            val events = service.events()
                .list(PRIMARY_CALENDAR)
                .setTimeMin(DateTime(startDateTime.toEpochMilli()))
                .setTimeMax(DateTime(endDateTime.toEpochMilli()))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setMaxResults(250) // Match web app behavior
                .execute()
            
            return@withContext events.items?.mapNotNull { event ->
                parseEvent(event)
            } ?: emptyList()
            
        } catch (e: Exception) {
            android.util.Log.e("CalendarDataSource", "Error fetching events: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Fetch events for today
     */
    suspend fun fetchTodayEvents(): List<CalendarEvent>? {
        val today = LocalDate.now()
        return fetchEvents(today, today.plusDays(1))
    }
    
    /**
     * Fetch events for current week (Monday to Saturday)
     */
    suspend fun fetchWeekEvents(): List<CalendarEvent>? {
        val today = LocalDate.now()
        // Get start of week (Monday)
        val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        // End of week is Saturday + 1 day (exclusive end)
        val endOfWeek = startOfWeek.plusDays(6) // Mon + 6 = Sunday (exclusive)
        return fetchEvents(startOfWeek, endOfWeek)
    }
    
    /**
     * Fetch events for specific week starting on given Monday
     */
    suspend fun fetchWeekEvents(weekStart: LocalDate): List<CalendarEvent>? {
        val weekEnd = weekStart.plusDays(6) // 6 days: Mon-Sat, end is exclusive
        return fetchEvents(weekStart, weekEnd)
    }
    
    /**
     * Fetch events for current month
     */
    suspend fun fetchMonthEvents(): List<CalendarEvent>? {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)
        val endOfMonth = startOfMonth.plusMonths(1)
        return fetchEvents(startOfMonth, endOfMonth)
    }
    
    /**
     * Fetch events for specific month
     */
    suspend fun fetchMonthEvents(year: Int, month: Int): List<CalendarEvent>? {
        val startOfMonth = LocalDate.of(year, month, 1)
        val endOfMonth = startOfMonth.plusMonths(1)
        return fetchEvents(startOfMonth, endOfMonth)
    }
    
    /**
     * Group events by date for calendar view
     */
    fun groupEventsByDate(events: List<CalendarEvent>): Map<LocalDate, List<CalendarEvent>> {
        val zone = ZoneId.systemDefault()
        return events.groupBy { event ->
            event.startTime.atZone(zone).toLocalDate()
        }
    }
    
    /**
     * Parse Google Calendar Event to our CalendarEvent model
     */
    private fun parseEvent(event: Event): CalendarEvent? {
        val id = event.id ?: return null
        val title = event.summary ?: "(No title)"
        
        // Handle all-day vs timed events
        val (startTime, isAllDay) = parseEventDateTime(event.start) ?: return null
        val (endTime, _) = parseEventDateTime(event.end) ?: return null
        
        return CalendarEvent(
            id = id,
            title = title,
            description = event.description,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            location = event.location,
            calendarId = PRIMARY_CALENDAR,
            recurrence = event.recurrence
        )
    }
    
    /**
     * Parse EventDateTime to Instant
     * Returns pair of (Instant, isAllDay)
     */
    private fun parseEventDateTime(eventDateTime: EventDateTime?): Pair<Instant, Boolean>? {
        if (eventDateTime == null) return null
        
        return when {
            // All-day event (date only, no time)
            eventDateTime.date != null -> {
                val instant = Instant.ofEpochMilli(eventDateTime.date.value)
                Pair(instant, true)
            }
            // Timed event
            eventDateTime.dateTime != null -> {
                val instant = Instant.ofEpochMilli(eventDateTime.dateTime.value)
                Pair(instant, false)
            }
            else -> null
        }
    }
    
    /**
     * Clear service reference (for sign out)
     */
    fun clear() {
        calendarService = null
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Create a new Google Calendar event
     * 
     * @return Created CalendarEvent or null on failure
     */
    suspend fun createEvent(
        title: String,
        description: String?,
        startTime: Instant,
        endTime: Instant,
        isAllDay: Boolean
    ): CalendarEvent? = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: run {
                android.util.Log.w("CalendarDataSource", "Calendar service not initialized")
                return@withContext null
            }
            
            val zone = ZoneId.systemDefault()
            
            val event = Event().apply {
                summary = title
                this.description = description
                
                if (isAllDay) {
                    // All-day events use date only (no time component)
                    start = EventDateTime().setDate(
                        DateTime(true, startTime.toEpochMilli(), 0)
                    )
                    end = EventDateTime().setDate(
                        DateTime(true, endTime.toEpochMilli(), 0)
                    )
                } else {
                    // Timed events use dateTime
                    start = EventDateTime().setDateTime(
                        DateTime(startTime.toEpochMilli())
                    ).setTimeZone(zone.id)
                    end = EventDateTime().setDateTime(
                        DateTime(endTime.toEpochMilli())
                    ).setTimeZone(zone.id)
                }
            }
            
            val createdEvent = service.events()
                .insert(PRIMARY_CALENDAR, event)
                .execute()
            
            android.util.Log.d("CalendarDataSource", "Created event: ${createdEvent.id}")
            
            return@withContext parseEvent(createdEvent)
            
        } catch (e: Exception) {
            android.util.Log.e("CalendarDataSource", "Error creating event: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Update an existing Google Calendar event
     * 
     * @return Updated CalendarEvent or null on failure
     */
    suspend fun updateEvent(
        eventId: String,
        title: String,
        description: String?,
        startTime: Instant,
        endTime: Instant,
        isAllDay: Boolean
    ): CalendarEvent? = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: run {
                android.util.Log.w("CalendarDataSource", "Calendar service not initialized")
                return@withContext null
            }
            
            val zone = ZoneId.systemDefault()
            
            // Get existing event first
            val existingEvent = service.events()
                .get(PRIMARY_CALENDAR, eventId)
                .execute()
            
            // Update fields
            existingEvent.summary = title
            existingEvent.description = description
            
            if (isAllDay) {
                existingEvent.start = EventDateTime().setDate(
                    DateTime(true, startTime.toEpochMilli(), 0)
                )
                existingEvent.end = EventDateTime().setDate(
                    DateTime(true, endTime.toEpochMilli(), 0)
                )
            } else {
                existingEvent.start = EventDateTime().setDateTime(
                    DateTime(startTime.toEpochMilli())
                ).setTimeZone(zone.id)
                existingEvent.end = EventDateTime().setDateTime(
                    DateTime(endTime.toEpochMilli())
                ).setTimeZone(zone.id)
            }
            
            val updatedEvent = service.events()
                .update(PRIMARY_CALENDAR, eventId, existingEvent)
                .execute()
            
            android.util.Log.d("CalendarDataSource", "Updated event: ${updatedEvent.id}")
            
            return@withContext parseEvent(updatedEvent)
            
        } catch (e: Exception) {
            android.util.Log.e("CalendarDataSource", "Error updating event: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Delete a Google Calendar event
     * 
     * @return true if deleted successfully
     */
    suspend fun deleteEvent(eventId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: run {
                android.util.Log.w("CalendarDataSource", "Calendar service not initialized")
                return@withContext false
            }
            
            service.events()
                .delete(PRIMARY_CALENDAR, eventId)
                .execute()
            
            android.util.Log.d("CalendarDataSource", "Deleted event: $eventId")
            
            return@withContext true
            
        } catch (e: Exception) {
            android.util.Log.e("CalendarDataSource", "Error deleting event: ${e.message}", e)
            return@withContext false
        }
    }
}
