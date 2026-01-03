package com.ido.app.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ido.app.data.model.Task
import com.ido.app.data.remote.CalendarEvent
import java.time.*
import java.time.format.DateTimeFormatter

/**
 * Bottom sheet for viewing/editing calendar items (tasks or events)
 * 
 * For Tasks: Full CRUD with Drive sync
 * For Events: Full CRUD with Google Calendar API
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarItemSheet(
    item: CalendarItem?,
    onDismiss: () -> Unit,
    onSaveTask: (text: String, priority: Boolean, done: Boolean, dueDate: String?, reminderTime: String?) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onSaveEvent: (event: CalendarEvent, title: String, description: String?, startTime: Instant, endTime: Instant, isAllDay: Boolean) -> Unit,
    onDeleteEvent: (eventId: String) -> Unit,
    onCreateEvent: (title: String, description: String?, startTime: Instant, endTime: Instant, isAllDay: Boolean) -> Unit,
    isCreatingEvent: Boolean = false,
    prefilledDate: LocalDate? = null,
    prefilledHour: Int? = null
) {
    if (item == null && !isCreatingEvent) return
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        when {
            isCreatingEvent -> EventCreateSheet(
                prefilledDate = prefilledDate,
                prefilledHour = prefilledHour,
                onDismiss = onDismiss,
                onCreate = onCreateEvent
            )
            item is CalendarItem.TaskItem -> TaskEditSheet(
                task = item.task,
                onDismiss = onDismiss,
                onSave = onSaveTask,
                onDelete = onDeleteTask
            )
            item is CalendarItem.EventItem -> EventEditSheet(
                event = item.event,
                onDismiss = onDismiss,
                onSave = onSaveEvent,
                onDelete = onDeleteEvent
            )
        }
    }
}

@Composable
private fun TaskEditSheet(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (text: String, priority: Boolean, done: Boolean, dueDate: String?, reminderTime: String?) -> Unit,
    onDelete: (taskId: String) -> Unit
) {
    val scrollState = rememberScrollState()
    val zone = ZoneId.systemDefault()
    
    var text by remember { mutableStateOf(task.text) }
    var priority by remember { mutableStateOf(task.priority) }
    var done by remember { mutableStateOf(task.done) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var reminderTime by remember { mutableStateOf(task.reminderTime) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Parse existing dueDate for pickers
    val existingDateTime = remember(task.dueDate) {
        task.dueDate?.let {
            try {
                Instant.parse(it).atZone(zone).toLocalDateTime()
            } catch (e: Exception) { null }
        }
    }
    
    var selectedDate by remember { mutableStateOf(existingDateTime?.toLocalDate() ?: LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(existingDateTime?.toLocalTime() ?: LocalTime.of(9, 0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Header
            Text(
                text = "Edit Task",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task text
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Task") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Done toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Completed", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = done, onCheckedChange = { done = it })
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Priority toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Priority", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = priority, onCheckedChange = { priority = it })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Due Date section
            Text("Due Date & Time", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date picker button
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, null)
                Spacer(Modifier.width(8.dp))
                Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Time picker button
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Schedule, null)
                Spacer(Modifier.width(8.dp))
                Text(selectedTime.format(DateTimeFormatter.ofPattern("h:mm a")))
            }
            
            // Clear due date
            if (dueDate != null) {
                TextButton(
                    onClick = { dueDate = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Due Date")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Delete button
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Task")
            }
        }
        
        // Action buttons (fixed at bottom)
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
                    // Build dueDate from selected date/time
                    val newDueDate = if (dueDate != null || showDatePicker) {
                        LocalDateTime.of(selectedDate, selectedTime)
                            .atZone(zone)
                            .toInstant()
                            .toString()
                    } else null
                    
                    onSave(text, priority, done, newDueDate, reminderTime)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { 
                selectedDate = it
                // Update dueDate
                dueDate = LocalDateTime.of(it, selectedTime)
                    .atZone(zone)
                    .toInstant()
                    .toString()
            },
            onDismiss = { showDatePicker = false }
        )
    }
    
    // Time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = {
                selectedTime = it
                // Update dueDate
                dueDate = LocalDateTime.of(selectedDate, it)
                    .atZone(zone)
                    .toInstant()
                    .toString()
            },
            onDismiss = { showTimePicker = false }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Task?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(task.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EventEditSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onSave: (event: CalendarEvent, title: String, description: String?, startTime: Instant, endTime: Instant, isAllDay: Boolean) -> Unit,
    onDelete: (eventId: String) -> Unit
) {
    val scrollState = rememberScrollState()
    val zone = ZoneId.systemDefault()
    
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    var isAllDay by remember { mutableStateOf(event.isAllDay) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val startDateTime = remember { event.startTime.atZone(zone).toLocalDateTime() }
    val endDateTime = remember { event.endTime.atZone(zone).toLocalDateTime() }
    
    var startDate by remember { mutableStateOf(startDateTime.toLocalDate()) }
    var startTime by remember { mutableStateOf(startDateTime.toLocalTime()) }
    var endDate by remember { mutableStateOf(endDateTime.toLocalDate()) }
    var endTime by remember { mutableStateOf(endDateTime.toLocalTime()) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Header
            Text(
                text = "Edit Event",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // All-day toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All-day", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start date/time
            Text("Start", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(startDate.format(DateTimeFormatter.ofPattern("MMM d")))
                }
                if (!isAllDay) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // End date/time
            Text("End", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(endDate.format(DateTimeFormatter.ofPattern("MMM d")))
                }
                if (!isAllDay) {
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(endTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                    }
                }
            }
            
            // Location (read-only for now)
            if (event.location != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Location", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Delete button
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Event")
            }
        }
        
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
                    val newStartTime = if (isAllDay) {
                        startDate.atStartOfDay(zone).toInstant()
                    } else {
                        LocalDateTime.of(startDate, startTime).atZone(zone).toInstant()
                    }
                    val newEndTime = if (isAllDay) {
                        endDate.plusDays(1).atStartOfDay(zone).toInstant()
                    } else {
                        LocalDateTime.of(endDate, endTime).atZone(zone).toInstant()
                    }
                    
                    onSave(
                        event,
                        title,
                        description.ifBlank { null },
                        newStartTime,
                        newEndTime,
                        isAllDay
                    )
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
    
    // Date/time picker dialogs
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { startDate = it },
            onDismiss = { showStartDatePicker = false }
        )
    }
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = { startTime = it },
            onDismiss = { showStartTimePicker = false }
        )
    }
    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = { endDate = it },
            onDismiss = { showEndDatePicker = false }
        )
    }
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = { endTime = it },
            onDismiss = { showEndTimePicker = false }
        )
    }
    
    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Event?") },
            text = { Text("This will remove the event from Google Calendar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(event.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EventCreateSheet(
    prefilledDate: LocalDate?,
    prefilledHour: Int?,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?, startTime: Instant, endTime: Instant, isAllDay: Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    val zone = ZoneId.systemDefault()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }
    
    var startDate by remember { mutableStateOf(prefilledDate ?: LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.of(prefilledHour ?: 9, 0)) }
    var endDate by remember { mutableStateOf(prefilledDate ?: LocalDate.now()) }
    var endTime by remember { mutableStateOf(LocalTime.of((prefilledHour ?: 9) + 1, 0)) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Header
            Text(
                text = "New Event",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add title") }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // All-day toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All-day", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start
            Text("Start", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                }
                if (!isAllDay) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // End
            Text("End", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                }
                if (!isAllDay) {
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(endTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                    }
                }
            }
        }
        
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
                    val newStartTime = if (isAllDay) {
                        startDate.atStartOfDay(zone).toInstant()
                    } else {
                        LocalDateTime.of(startDate, startTime).atZone(zone).toInstant()
                    }
                    val newEndTime = if (isAllDay) {
                        endDate.plusDays(1).atStartOfDay(zone).toInstant()
                    } else {
                        LocalDateTime.of(endDate, endTime).atZone(zone).toInstant()
                    }
                    
                    onCreate(title, description.ifBlank { null }, newStartTime, newEndTime, isAllDay)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        }
    }
    
    // Date/time pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { 
                startDate = it
                if (endDate.isBefore(it)) endDate = it
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = { 
                startTime = it
                // Auto-adjust end time to +1 hour
                endTime = it.plusHours(1)
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = { endDate = it },
            onDismiss = { showEndDatePicker = false }
        )
    }
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = { endTime = it },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
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
                    onDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    onDismiss()
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
    )
}
