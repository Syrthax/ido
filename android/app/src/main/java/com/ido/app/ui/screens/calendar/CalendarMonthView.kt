package com.ido.app.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ido.app.data.model.Task
import com.ido.app.ui.screens.edit.EditTaskSheet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Helper function to parse task dueDate String? to LocalDate?
 */
private fun Task.getDueDateAsLocalDate(): LocalDate? {
    return try {
        dueDate?.let { 
            // Handle ISO-8601 format (could be date-only or datetime)
            LocalDate.parse(it.substring(0, 10))
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Calendar Month View matching iDo design specs
 * 
 * Features:
 * - Year/Month header with search and add buttons
 * - View toggle tabs: Year, Month, Week, Day
 * - Calendar grid with event dots
 * - Selected day highlight with glow effect
 * - Today's Focus card at bottom
 */
@Composable
fun CalendarMonthView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    tasksWithDueDate: List<Task>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskToggleDone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    // View mode state (tabs)
    var selectedViewMode by remember { mutableStateOf("Month") }
    val viewModes = listOf("Year", "Month", "Week", "Day")
    
    // Get tasks for selected date
    val todayTasks = tasksWithDueDate.filter { task ->
        task.getDueDateAsLocalDate() == selectedDate && !task.done
    }
    
    // Build event dots map (date -> list of colors)
    val eventDots: Map<LocalDate, List<Color>> = remember(tasksWithDueDate) {
        tasksWithDueDate
            .mapNotNull { task -> 
                task.getDueDateAsLocalDate()?.let { date -> date to task }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, tasks) ->
                tasks.take(3).map { task ->
                    when {
                        task.priority -> Color(0xFFEF4444) // Red for priority
                        else -> Color(0xFF5048E5) // Primary for normal
                    }
                }
            }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        CalendarHeader(
            currentMonth = currentMonth,
            onSearchClick = onSearchClick,
            onAddClick = onAddClick,
            isDark = isDark
        )
        
        // View mode tabs
        ViewModeTabs(
            modes = viewModes,
            selectedMode = selectedViewMode,
            onModeSelected = { selectedViewMode = it }
        )
        
        // Calendar grid
        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            today = LocalDate.now(),
            eventDots = eventDots,
            onDateSelected = onDateSelected,
            isDark = isDark,
            modifier = Modifier.weight(1f)
        )
        
        // Today's Focus card
        if (todayTasks.isNotEmpty()) {
            TodaysFocusCard(
                tasks = todayTasks,
                onTaskClick = onTaskClick,
                onTaskToggleDone = onTaskToggleDone,
                isDark = isDark
            )
        }
        
        // Bottom padding for dock
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currentMonth.year.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Change month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Search button
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Add button
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ViewModeTabs(
    modes: List<String>,
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            modes.forEach { mode ->
                val isSelected = mode == selectedMode
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surface
                            else Color.Transparent
                        )
                        .clickable { onModeSelected(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventDots: Map<LocalDate, List<Color>>,
    onDateSelected: (LocalDate) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar days grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val lastDayOfMonth = currentMonth.atEndOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
        val daysInMonth = currentMonth.lengthOfMonth()
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - firstDayOfWeek + 1
                    
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayOfMonth)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val dots = eventDots[date] ?: emptyList()
                        
                        CalendarDayCell(
                            dayOfMonth = dayOfMonth,
                            isSelected = isSelected,
                            isToday = isToday,
                            isCurrentMonth = true,
                            eventDots = dots,
                            onClick = { onDateSelected(date) },
                            isDark = isDark
                        )
                    } else {
                        // Empty cell or previous/next month day
                        Box(modifier = Modifier.size(44.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayOfMonth: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    eventDots: List<Color>,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> MaterialTheme.colorScheme.primary
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            // Event dots
            if (eventDots.isNotEmpty() && !isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    eventDots.take(3).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(color, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaysFocusCard(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskToggleDone: (String) -> Unit,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = if (isDark) 0.dp else 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Focus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${tasks.size} tasks remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { /* Navigate to tasks */ },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "View all",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task list
            tasks.take(3).forEach { task ->
                FocusTaskItem(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onToggleDone = { onTaskToggleDone(task.id) }
                )
            }
        }
    }
}

@Composable
private fun FocusTaskItem(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit
) {
    val accentColor = when {
        task.priority -> Color(0xFF5048E5) // Primary for priority
        else -> Color(0xFFF59E0B) // Amber for normal
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .background(
                    color = accentColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Task content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            task.dueDate?.let { dueDate ->
                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                Text(
                    text = dueDate.format(timeFormatter) + " • " + (if (task.priority) "Priority" else "Task"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
                .clickable(onClick = onToggleDone),
            contentAlignment = Alignment.Center
        ) {
            if (task.done) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Complete",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Wrapper composable that bridges ViewModels to CalendarMonthView
 * This is used from MainActivity
 */
@Composable
fun CalendarMonthView(
    viewModel: com.ido.app.ui.screens.home.HomeViewModel,
    calendarViewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val tasksWithDueDate by calendarViewModel.tasksWithDueDate.collectAsState()
    val selectedDate by calendarViewModel.selectedDate.collectAsState()
    val isCreatingTask by calendarViewModel.isCreatingTask.collectAsState()
    val prefilledDate by calendarViewModel.prefilledDate.collectAsState()
    
    // Track current month based on selected date
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    
    // Update current month when selected date changes significantly
    LaunchedEffect(selectedDate) {
        if (selectedDate.year != currentMonth.year || selectedDate.monthValue != currentMonth.monthValue) {
            currentMonth = YearMonth.from(selectedDate)
        }
    }
    
    CalendarMonthView(
        currentMonth = currentMonth,
        selectedDate = selectedDate,
        tasksWithDueDate = tasksWithDueDate,
        onDateSelected = { date -> 
            calendarViewModel.showDayView(date)
        },
        onPreviousMonth = { 
            currentMonth = currentMonth.minusMonths(1)
        },
        onNextMonth = { 
            currentMonth = currentMonth.plusMonths(1)
        },
        onToday = { 
            calendarViewModel.goToToday()
            currentMonth = YearMonth.now()
        },
        onAddClick = { 
            calendarViewModel.showCreateTask(date = selectedDate)
        },
        onSearchClick = { 
            // TODO: Implement search
        },
        onTaskClick = { task ->
            calendarViewModel.selectTask(task)
        },
        onTaskToggleDone = { taskId ->
            viewModel.toggleTaskDone(taskId)
        },
        modifier = modifier
    )
    
    // Create task bottom sheet
    if (isCreatingTask) {
        EditTaskSheet(
            task = null,
            prefilledDate = prefilledDate,
            onDismiss = { calendarViewModel.clearSelection() },
            onSave = { text, priority, dueDate, reminderTime ->
                viewModel.saveTask(text, priority, dueDate, reminderTime)
                calendarViewModel.clearSelection()
            }
        )
    }
}
