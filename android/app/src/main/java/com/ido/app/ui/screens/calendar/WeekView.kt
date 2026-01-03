package com.ido.app.ui.screens.calendar

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ido.app.data.model.Task
import com.ido.app.data.remote.CalendarEvent
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * Week View component for Calendar
 * 
 * Displays 6-day week (Mon-Sat) with 24-hour vertical timeline
 * Shows both tasks (with dueDate) and Google Calendar events
 * 
 * Interactions:
 * - Tap task/event: Opens edit sheet
 * - Long-press empty slot: Opens create dialog
 * - Tap day header: Switches to Day View for that day
 */

// Constants
private val HOUR_HEIGHT = 60.dp
private val TIME_COLUMN_WIDTH = 48.dp
private val DAY_COLUMN_MIN_WIDTH = 80.dp
private const val HOURS_IN_DAY = 24
private const val DAYS_IN_WEEK = 6 // Mon-Sat

@Composable
fun WeekView(
    weekStart: LocalDate,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onDayHeaderClick: (LocalDate) -> Unit = {},
    onEmptySlotLongPress: (LocalDate, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val scrollState = rememberScrollState()
    
    // Generate days for the week (Mon-Sat = 6 days)
    val weekDays = (0 until DAYS_IN_WEEK).map { weekStart.plusDays(it.toLong()) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Day headers (sticky) - clickable to switch to Day View
        WeekDayHeaders(
            weekDays = weekDays,
            today = today,
            onDayClick = onDayHeaderClick
        )
        
        HorizontalDivider()
        
        // Scrollable time grid
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Time column
            TimeColumn()
            
            // Day columns with events
            weekDays.forEach { date ->
                DayColumn(
                    date = date,
                    isToday = date == today,
                    events = events.filter { 
                        it.startTime.atZone(zone).toLocalDate() == date 
                    },
                    tasks = tasks.filter { task ->
                        task.dueDate?.let { dueDateStr ->
                            try {
                                Instant.parse(dueDateStr).atZone(zone).toLocalDate() == date
                            } catch (e: Exception) { false }
                        } ?: false
                    },
                    onTaskClick = onTaskClick,
                    onEventClick = onEventClick,
                    onEmptySlotLongPress = { hour -> onEmptySlotLongPress(date, hour) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    
    // Auto-scroll to current hour on first composition
    LaunchedEffect(Unit) {
        val currentHour = LocalTime.now().hour
        val scrollPosition = (currentHour * HOUR_HEIGHT.value).toInt()
        scrollState.animateScrollTo(scrollPosition.coerceAtLeast(0))
    }
}

@Composable
private fun WeekDayHeaders(
    weekDays: List<LocalDate>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Empty space for time column
        Spacer(modifier = Modifier.width(TIME_COLUMN_WIDTH))
        
        weekDays.forEach { date ->
            DayHeader(
                date = date,
                isToday = date == today,
                onClick = { onDayClick(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Day name (Mon, Tue, etc.)
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Date number with highlight if today
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (isToday) Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                    else Modifier
                )
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TimeColumn() {
    Column(
        modifier = Modifier.width(TIME_COLUMN_WIDTH)
    ) {
        (0 until HOURS_IN_DAY).forEach { hour ->
            Box(
                modifier = Modifier
                    .height(HOUR_HEIGHT)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = formatHour(hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    date: LocalDate,
    isToday: Boolean,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else Color.Transparent
            )
    ) {
        // Hour grid lines with long-press detection
        Column {
            repeat(HOURS_IN_DAY) { hour ->
                Box(
                    modifier = Modifier
                        .height(HOUR_HEIGHT)
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    onEmptySlotLongPress(hour)
                                }
                            )
                        }
                )
            }
        }
        
        // Current time indicator (red line)
        if (isToday) {
            CurrentTimeIndicator()
        }
        
        // Events positioned by time
        events.forEach { event ->
            if (!event.isAllDay) {
                val startTime = event.startTime.atZone(zone).toLocalTime()
                val endTime = event.endTime.atZone(zone).toLocalTime()
                
                EventBlock(
                    event = event,
                    startTime = startTime,
                    endTime = endTime,
                    onClick = { onEventClick(event) }
                )
            }
        }
        
        // All-day events at top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            events.filter { it.isAllDay }.take(2).forEach { event ->
                AllDayEventChip(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
        
        // Tasks positioned by due time
        tasks.forEach { task ->
            val dueTime = task.dueDate?.let { dueDateStr ->
                try {
                    Instant.parse(dueDateStr).atZone(zone).toLocalTime()
                } catch (e: Exception) { 
                    null // ignore invalid dates
                }
            }
            
            if (dueTime != null) {
                TaskBlock(
                    task = task,
                    time = dueTime,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeIndicator() {
    val currentTime = LocalTime.now()
    val minutesSinceMidnight = currentTime.hour * 60 + currentTime.minute
    val topOffset = (minutesSinceMidnight * HOUR_HEIGHT.value / 60).dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = topOffset)
            .height(2.dp)
            .background(MaterialTheme.colorScheme.error)
    )
}

@Composable
private fun EventBlock(
    event: CalendarEvent,
    startTime: LocalTime,
    endTime: LocalTime,
    onClick: () -> Unit
) {
    val startMinutes = startTime.hour * 60 + startTime.minute
    val endMinutes = endTime.hour * 60 + endTime.minute
    val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(30)
    
    val topOffset = (startMinutes * HOUR_HEIGHT.value / 60).dp
    val height = (durationMinutes * HOUR_HEIGHT.value / 60).dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .offset(y = topOffset)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Column {
            Text(
                text = event.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (height > 40.dp) {
                Text(
                    text = "${formatTime(startTime)} - ${formatTime(endTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AllDayEventChip(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun TaskBlock(
    task: Task,
    time: LocalTime,
    onClick: () -> Unit
) {
    val minutes = time.hour * 60 + time.minute
    val topOffset = (minutes * HOUR_HEIGHT.value / 60).dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .offset(y = topOffset)
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (task.priority) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.tertiaryContainer
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.priority) {
                Text(
                    text = "★ ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = task.text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (task.priority) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "$hour:${time.minute.toString().padStart(2, '0')} $amPm"
}
