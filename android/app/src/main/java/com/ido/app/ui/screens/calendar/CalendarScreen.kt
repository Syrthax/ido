package com.ido.app.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ido.app.data.remote.CalendarEvent
import com.ido.app.data.repository.SyncStatus
import com.ido.app.ui.screens.home.HomeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Calendar screen with Week View and Day View
 * 
 * Features:
 * - Week View: 6-day view (Mon-Sat) with 24-hour timeline
 * - Day View: Single day focused view (tap day header to enter)
 * - Google Calendar events display + CRUD
 * - Tasks with dueDate display
 * - Long-press empty slot to create task/event
 * - Tap item to edit
 * - FAB to create new task/event
 * - Cloud icon force sync
 * 
 * Data flow:
 * - Tasks: From HomeViewModel (synced via Drive)
 * - Events: From CalendarViewModel (Google Calendar API)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: HomeViewModel,
    calendarViewModel: CalendarViewModel
) {
    val homeUiState by viewModel.uiState.collectAsState()
    val viewMode by calendarViewModel.viewMode.collectAsState()
    val weekStart by calendarViewModel.currentWeekStart.collectAsState()
    val selectedDate by calendarViewModel.selectedDate.collectAsState()
    val events by calendarViewModel.events.collectAsState()
    val tasksWithDueDate by calendarViewModel.tasksWithDueDate.collectAsState()
    val isLoading by calendarViewModel.isLoading.collectAsState()
    val error by calendarViewModel.error.collectAsState()
    
    // Item sheet states
    val selectedItem by calendarViewModel.selectedItem.collectAsState()
    val isCreatingEvent by calendarViewModel.isCreatingEvent.collectAsState()
    val isCreatingTask by calendarViewModel.isCreatingTask.collectAsState()
    val prefilledDate by calendarViewModel.prefilledDate.collectAsState()
    val prefilledHour by calendarViewModel.prefilledHour.collectAsState()
    
    // FAB menu expanded state
    var fabExpanded by remember { mutableStateOf(false) }
    
    // Load events when screen appears or week/day changes
    LaunchedEffect(viewMode, weekStart, selectedDate) {
        when (viewMode) {
            CalendarViewMode.WEEK -> calendarViewModel.loadWeekEvents()
            CalendarViewMode.DAY -> calendarViewModel.loadDayEvents(selectedDate)
        }
    }
    
    // Refresh events when sync completes
    LaunchedEffect(homeUiState.syncStatus) {
        if (homeUiState.syncStatus == SyncStatus.Synced) {
            calendarViewModel.refreshEvents()
        }
    }
    
    Scaffold(
        topBar = {
            CalendarTopBar(
                viewMode = viewMode,
                weekRange = calendarViewModel.getWeekRangeString(),
                selectedDateString = calendarViewModel.getSelectedDateString(),
                syncStatus = homeUiState.syncStatus,
                onPrevious = { 
                    when (viewMode) {
                        CalendarViewMode.WEEK -> calendarViewModel.previousWeek()
                        CalendarViewMode.DAY -> calendarViewModel.previousDay()
                    }
                },
                onToday = { calendarViewModel.goToToday() },
                onNext = {
                    when (viewMode) {
                        CalendarViewMode.WEEK -> calendarViewModel.nextWeek()
                        CalendarViewMode.DAY -> calendarViewModel.nextDay()
                    }
                },
                onViewModeToggle = { calendarViewModel.toggleViewMode() },
                onForceSync = {
                    // Force sync Drive tasks + Calendar events
                    viewModel.syncNow()
                    calendarViewModel.refreshEvents()
                }
            )
        },
        floatingActionButton = {
            // FAB for creating new task or event
            Box {
                // Expanded menu
                if (fabExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 72.dp)
                    ) {
                        // Create Task mini-FAB
                        SmallFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                calendarViewModel.showCreateTask()
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = "Create Task"
                            )
                        }
                        
                        // Create Event mini-FAB
                        SmallFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                calendarViewModel.showCreateEvent()
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Create Event"
                            )
                        }
                    }
                }
                
                // Main FAB
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = if (fabExpanded) 
                        MaterialTheme.colorScheme.surfaceVariant 
                        else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (fabExpanded) "Close menu" else "Add"
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View based on mode
            when (viewMode) {
                CalendarViewMode.WEEK -> {
                    WeekView(
                        weekStart = weekStart,
                        events = events,
                        tasks = tasksWithDueDate,
                        onTaskClick = { task -> 
                            calendarViewModel.selectTask(task)
                        },
                        onEventClick = { event ->
                            calendarViewModel.selectEvent(event)
                        },
                        onDayHeaderClick = { date ->
                            calendarViewModel.showDayView(date)
                        },
                        onEmptySlotLongPress = { date, hour ->
                            calendarViewModel.showCreateEvent(date, hour)
                        }
                    )
                }
                CalendarViewMode.DAY -> {
                    DayView(
                        date = selectedDate,
                        events = events,
                        tasks = tasksWithDueDate,
                        onTaskClick = { task ->
                            calendarViewModel.selectTask(task)
                        },
                        onEventClick = { event ->
                            calendarViewModel.selectEvent(event)
                        },
                        onEmptySlotLongPress = { date, hour ->
                            calendarViewModel.showCreateEvent(date, hour)
                        }
                    )
                }
            }
            
            // Loading overlay
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
            
            // Error snackbar
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { calendarViewModel.refreshEvents() }) {
                            Text("Retry")
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    }
    
    // Bottom sheet for selected item or creating event
    val showSheet = selectedItem != null || isCreatingEvent
    if (showSheet) {
        CalendarItemSheet(
            item = selectedItem,
            onDismiss = { calendarViewModel.clearSelection() },
            onSaveTask = { text, priority, done, dueDate, reminderTime ->
                // Update the task through HomeViewModel
                selectedItem?.let { item ->
                    if (item is CalendarItem.TaskItem) {
                        val updatedTask = item.task.copy(
                            text = text,
                            priority = priority,
                            done = done,
                            dueDate = dueDate,
                            reminderTime = reminderTime,
                            updatedAt = java.time.Instant.now().toString()
                        )
                        viewModel.saveTask(updatedTask)
                    }
                }
                calendarViewModel.clearSelection()
            },
            onDeleteTask = { taskId ->
                viewModel.deleteTask(taskId)
                calendarViewModel.clearSelection()
            },
            onSaveEvent = { event, title, description, startTime, endTime, isAllDay ->
                calendarViewModel.updateEvent(event, title, description, startTime, endTime, isAllDay)
            },
            onDeleteEvent = { eventId ->
                calendarViewModel.deleteEvent(eventId)
            },
            onCreateEvent = { title, description, startTime, endTime, isAllDay ->
                calendarViewModel.createEvent(title, description, startTime, endTime, isAllDay)
            },
            isCreatingEvent = isCreatingEvent,
            prefilledDate = prefilledDate,
            prefilledHour = prefilledHour
        )
    }
    
    // Create task sheet (uses HomeViewModel)
    if (isCreatingTask) {
        CreateTaskSheet(
            prefilledDate = prefilledDate ?: LocalDate.now(),
            prefilledHour = prefilledHour,
            onDismiss = { calendarViewModel.clearSelection() },
            onCreate = { text, priority, dueDate, reminderTime ->
                viewModel.createTask(text, priority, dueDate, reminderTime)
                calendarViewModel.clearSelection()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    viewMode: CalendarViewMode,
    weekRange: String,
    selectedDateString: String,
    syncStatus: SyncStatus,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    onViewModeToggle: () -> Unit,
    onForceSync: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Previous button
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = if (viewMode == CalendarViewMode.WEEK) "Previous week" else "Previous day"
                    )
                }
                
                // Today button
                TextButton(onClick = onToday) {
                    Text("Today")
                }
                
                // Date range display
                Text(
                    text = when (viewMode) {
                        CalendarViewMode.WEEK -> weekRange
                        CalendarViewMode.DAY -> selectedDateString
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Next button
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = if (viewMode == CalendarViewMode.WEEK) "Next week" else "Next day"
                    )
                }
            }
        },
        navigationIcon = {
            // View mode toggle
            IconButton(onClick = onViewModeToggle) {
                Icon(
                    imageVector = when (viewMode) {
                        CalendarViewMode.WEEK -> Icons.Default.ViewDay
                        CalendarViewMode.DAY -> Icons.Default.ViewWeek
                    },
                    contentDescription = when (viewMode) {
                        CalendarViewMode.WEEK -> "Switch to Day View"
                        CalendarViewMode.DAY -> "Switch to Week View"
                    }
                )
            }
        },
        actions = {
            // Sync status indicator (clickable for force sync)
            IconButton(onClick = onForceSync) {
                when (syncStatus) {
                    is SyncStatus.Syncing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is SyncStatus.Synced -> {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced - Tap to refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    is SyncStatus.Error -> {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Sync error - Tap to retry",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Tap to sync"
                        )
                    }
                }
            }
        }
    )
}

/**
 * Create Event bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventSheet(
    prefilledDate: LocalDate,
    prefilledHour: Int,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?, startTime: Instant, endTime: Instant, isAllDay: Boolean) -> Unit
) {
    val zone = ZoneId.systemDefault()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }
    
    // Start/end times
    var startDate by remember { mutableStateOf(prefilledDate) }
    var startHour by remember { mutableStateOf(prefilledHour) }
    var startMinute by remember { mutableStateOf(0) }
    var endDate by remember { mutableStateOf(prefilledDate) }
    var endHour by remember { mutableStateOf((prefilledHour + 1).coerceAtMost(23)) }
    var endMinute by remember { mutableStateOf(0) }
    
    // Pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Create Event",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // All day toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All-day event")
                Switch(
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start date/time
            Text("Start", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(startDate.toString())
                }
                if (!isAllDay) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatTime(startHour, startMinute))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // End date/time
            Text("End", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(endDate.toString())
                }
                if (!isAllDay) {
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatTime(endHour, endMinute))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val startTime = if (isAllDay) {
                                startDate.atStartOfDay(zone).toInstant()
                            } else {
                                startDate.atTime(startHour, startMinute).atZone(zone).toInstant()
                            }
                            val endTime = if (isAllDay) {
                                endDate.plusDays(1).atStartOfDay(zone).toInstant()
                            } else {
                                endDate.atTime(endHour, endMinute).atZone(zone).toInstant()
                            }
                            onCreate(title, description.ifBlank { null }, startTime, endTime, isAllDay)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { 
                startDate = it
                if (endDate.isBefore(startDate)) endDate = startDate
                showStartDatePicker = false
            }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { 
                endDate = it
                showEndDatePicker = false
            }
        )
    }
    
    // Time pickers
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = startHour,
            initialMinute = startMinute,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { h, m ->
                startHour = h
                startMinute = m
                showStartTimePicker = false
            }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = endHour,
            initialMinute = endMinute,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { h, m ->
                endHour = h
                endMinute = m
                showEndTimePicker = false
            }
        )
    }
}

/**
 * Create Task bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskSheet(
    prefilledDate: LocalDate,
    prefilledHour: Int?,
    onDismiss: () -> Unit,
    onCreate: (text: String, priority: Boolean, dueDate: Instant?, reminderTime: Instant?) -> Unit
) {
    val zone = ZoneId.systemDefault()
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(false) }
    var hasDueDate by remember { mutableStateOf(true) }
    var dueDate by remember { mutableStateOf(prefilledDate) }
    var dueHour by remember { mutableStateOf(prefilledHour ?: 9) }
    var dueMinute by remember { mutableStateOf(0) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Create Task",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task text
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Task description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Priority toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (priority) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Priority")
                }
                Switch(
                    checked = priority,
                    onCheckedChange = { priority = it }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Due date toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set due date")
                Switch(
                    checked = hasDueDate,
                    onCheckedChange = { hasDueDate = it }
                )
            }
            
            if (hasDueDate) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(dueDate.toString())
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(formatTime(dueHour, dueMinute))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            val dueDateInstant = if (hasDueDate) {
                                dueDate.atTime(dueHour, dueMinute).atZone(zone).toInstant()
                            } else null
                            onCreate(text, priority, dueDateInstant, null)
                        }
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Date picker
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = dueDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { 
                dueDate = it
                showDatePicker = false
            }
        )
    }
    
    // Time picker
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = dueHour,
            initialMinute = dueMinute,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { h, m ->
                dueHour = h
                dueMinute = m
                showTimePicker = false
            }
        )
    }
}

/**
 * Date picker dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Time picker dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    return "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
}
