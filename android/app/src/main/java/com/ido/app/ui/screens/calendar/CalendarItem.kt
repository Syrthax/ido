package com.ido.app.ui.screens.calendar

import com.ido.app.data.model.Task
import com.ido.app.data.remote.CalendarEvent

/**
 * Sealed class representing items that can appear on the calendar
 * Used for unified handling of task and event clicks
 */
sealed class CalendarItem {
    data class TaskItem(val task: Task) : CalendarItem()
    data class EventItem(val event: CalendarEvent) : CalendarItem()
}

/**
 * View mode for calendar display
 */
enum class CalendarViewMode {
    WEEK,
    DAY
}
