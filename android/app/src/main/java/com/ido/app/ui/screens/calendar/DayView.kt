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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ido.app.data.model.Task
import com.ido.app.data.remote.CalendarEvent
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * Day View component for Calendar
 * 
 * Single day focused view with 24-hour vertical timeline
 * Shows tasks and events for the selected day
 */

private val HOUR_HEIGHT = 60.dp
private val TIME_COLUMN_WIDTH = 56.dp
private const val HOURS_IN_DAY = 24

@Composable
fun DayView(
    date: LocalDate,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onEmptySlotLongPress: (LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val isToday = date == today
    val scrollState = rememberScrollState()
    
    // Filter events and tasks for this day
    val dayEvents = events.filter { 
        it.startTime.atZone(zone).toLocalDate() == date 
    }
    val dayTasks = tasks.filter { task ->
        task.dueDate?.let { dueDateStr ->
            try {
                Instant.parse(dueDateStr).atZone(zone).toLocalDate() == date
            } catch (e: Exception) { false }
        } ?: false
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Day header
        DayViewHeader(date = date, isToday = isToday)
        
        HorizontalDivider()
        
        // All-day events section
        if (dayEvents.any { it.isAllDay }) {
            AllDayEventsSection(
                events = dayEvents.filter { it.isAllDay },
                onEventClick = onEventClick
            )
            HorizontalDivider()
        }
        
        // Scrollable time grid
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Time column
            DayTimeColumn()
            
            // Events/Tasks column
            DayEventsColumn(
                date = date,
                isToday = isToday,
                events = dayEvents.filter { !it.isAllDay },
                tasks = dayTasks,
                onTaskClick = onTaskClick,
                onEventClick = onEventClick,
                onEmptySlotLongPress = onEmptySlotLongPress,
                modifier = Modifier.weight(1f)
            )
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
private fun DayViewHeader(
    date: LocalDate,
    isToday: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Day name
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.titleMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Date with highlight if today
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (isToday) Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary)
                        else Modifier
                    )
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Month and year
            Text(
                text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AllDayEventsSection(
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "All-day",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        events.forEach { event ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { onEventClick(event) },
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DayTimeColumn() {
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
private fun DayEventsColumn(
    date: LocalDate,
    isToday: Boolean,
    events: List<CalendarEvent>,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onEmptySlotLongPress: (LocalDate, Int) -> Unit,
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
                                    onEmptySlotLongPress(date, hour)
                                }
                            )
                        }
                )
            }
        }
        
        // Current time indicator
        if (isToday) {
            DayCurrentTimeIndicator()
        }
        
        // Events
        events.forEach { event ->
            val startTime = event.startTime.atZone(zone).toLocalTime()
            val endTime = event.endTime.atZone(zone).toLocalTime()
            
            DayEventBlock(
                event = event,
                startTime = startTime,
                endTime = endTime,
                onClick = { onEventClick(event) }
            )
        }
        
        // Tasks
        tasks.forEach { task ->
            val dueTime = task.dueDate?.let { dueDateStr ->
                try {
                    Instant.parse(dueDateStr).atZone(zone).toLocalTime()
                } catch (e: Exception) { null }
            }
            
            if (dueTime != null) {
                DayTaskBlock(
                    task = task,
                    time = dueTime,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun DayCurrentTimeIndicator() {
    val currentTime = LocalTime.now()
    val minutesSinceMidnight = currentTime.hour * 60 + currentTime.minute
    val topOffset = (minutesSinceMidnight * HOUR_HEIGHT.value / 60).dp
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = topOffset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.error)
        )
        // Red line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.error)
        )
    }
}

@Composable
private fun DayEventBlock(
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
            .padding(horizontal = 4.dp)
            .offset(y = topOffset)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (height > 50.dp) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatTime(startTime)} - ${formatTime(endTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (event.location != null && height > 80.dp) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayTaskBlock(
    task: Task,
    time: LocalTime,
    onClick: () -> Unit
) {
    val minutes = time.hour * 60 + time.minute
    val topOffset = (minutes * HOUR_HEIGHT.value / 60).dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .offset(y = topOffset)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (task.priority) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.tertiaryContainer
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.priority) {
                Text(
                    text = "★ ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (task.priority) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTime(time),
                style = MaterialTheme.typography.labelSmall,
                color = if (task.priority) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
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
