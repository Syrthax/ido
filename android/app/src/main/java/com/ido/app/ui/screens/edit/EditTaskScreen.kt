package com.ido.app.ui.screens.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ido.app.data.model.Task
import com.ido.app.util.NaturalDateParser
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskSheet(
    task: Task?,
    prefilledDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String?, String?) -> Unit
) {
    val zone = ZoneId.systemDefault()
    
    var text by remember { mutableStateOf(task?.text ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: false) }
    var dueDate by remember { 
        val initial = task?.dueDate ?: prefilledDate?.atStartOfDay(zone)?.toInstant()?.toString()
        mutableStateOf(initial) 
    }
    var reminderTime by remember { mutableStateOf(task?.reminderTime) }
    
    var selectedTab by remember { mutableStateOf(0) }
    var naturalInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(task?.reminderTime != null) }
    var reminderOffset by remember { mutableStateOf(ReminderOffset.AT_TIME) }
    
    // Debug logging
    android.util.Log.d("EditTaskSheet", "Initializing: task=${task?.id}, dueDate=${task?.dueDate}, prefilledDate=$prefilledDate, reminderTime=${task?.reminderTime}")
    android.util.Log.d("EditTaskSheet", "State: reminderEnabled=$reminderEnabled")
    
    // Parse existing dueDate for pickers
    val existingDateTime = remember(dueDate) {
        dueDate?.let {
            try {
                Instant.parse(it).atZone(zone).toLocalDateTime()
            } catch (e: Exception) { null }
        }
    }
    
    var selectedDate by remember(existingDateTime, prefilledDate) { 
        mutableStateOf(existingDateTime?.toLocalDate() ?: prefilledDate ?: LocalDate.now()) 
    }
    var selectedTime by remember(existingDateTime) { 
        mutableStateOf(existingDateTime?.toLocalTime() ?: LocalTime.of(9, 0)) 
    }
    
    // CRITICAL FIX: Recalculate reminderTime when dueDate changes AND reminder is enabled
    // This fixes the bug where changing 7am to 7pm didn't update the reminder
    LaunchedEffect(dueDate, reminderEnabled, reminderOffset) {
        android.util.Log.d("EditTaskSheet", "LaunchedEffect: dueDate=$dueDate, reminderEnabled=$reminderEnabled, reminderOffset=$reminderOffset")
        if (reminderEnabled && dueDate != null) {
            try {
                val due = Instant.parse(dueDate)
                reminderTime = when (reminderOffset) {
                    ReminderOffset.AT_TIME -> due.toString()
                    ReminderOffset.FIVE_MIN -> due.minus(Duration.ofMinutes(5)).toString()
                    ReminderOffset.FIFTEEN_MIN -> due.minus(Duration.ofMinutes(15)).toString()
                    ReminderOffset.ONE_HOUR -> due.minus(Duration.ofHours(1)).toString()
                }
                android.util.Log.d("EditTaskSheet", "LaunchedEffect: Updated reminderTime=$reminderTime")
            } catch (e: Exception) {
                android.util.Log.e("EditTaskSheet", "Failed to calculate reminderTime", e)
            }
        } else if (!reminderEnabled) {
            reminderTime = null
            android.util.Log.d("EditTaskSheet", "LaunchedEffect: Cleared reminderTime (reminder disabled)")
        }
    }
    
    val scrollState = rememberScrollState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
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
                text = if (task == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task text input
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Task") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                singleLine = false,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Priority switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Priority", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = priority,
                    onCheckedChange = { priority = it }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Date/Time section
            Text(
                "Due Date & Time",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tabs for Natural vs Manual
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Natural") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pick Date") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (selectedTab) {
                0 -> NaturalLanguageTab(
                    value = naturalInput,
                    onValueChange = { 
                        naturalInput = it
                        // Parse and update dueDate
                        NaturalDateParser.parse(it)?.let { instant ->
                            dueDate = instant.toString()
                            // Update selected date/time for picker sync
                            val dt = instant.atZone(zone).toLocalDateTime()
                            selectedDate = dt.toLocalDate()
                            selectedTime = dt.toLocalTime()
                        }
                    },
                    currentDueDate = dueDate
                )
                1 -> DateTimePickerTab(
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,
                    dueDate = dueDate,
                    onDateChange = { dueDate = it },
                    onShowDatePicker = { showDatePicker = true },
                    onShowTimePicker = { showTimePicker = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick shortcuts
            Text("Quick Add", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        val instant = LocalDateTime.now()
                            .plusDays(1)
                            .withHour(9)
                            .withMinute(0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                        dueDate = instant.toString()
                    },
                    label = { Text("Tomorrow 9am") }
                )
                AssistChip(
                    onClick = {
                        val instant = LocalDateTime.now()
                            .withHour(18)
                            .withMinute(0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                        dueDate = instant.toString()
                    },
                    label = { Text("Today 6pm") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reminder section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remind me", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it }
                )
            }
            
            if (reminderEnabled && dueDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                ReminderOffsetSelector(
                    selected = reminderOffset,
                    onSelect = { 
                        reminderOffset = it
                        // LaunchedEffect will handle the reminderTime calculation
                    }
                )
            }
            
            // Extra space before buttons
            Spacer(modifier = Modifier.height(16.dp))
            } // End of scrollable Column
            
            // Action buttons (fixed at bottom, outside scroll)
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
                            val finalReminderTime = if (reminderEnabled) reminderTime else null
                            android.util.Log.d("EditTaskSheet", "SAVE CLICKED:")
                            android.util.Log.d("EditTaskSheet", "  text=$text")
                            android.util.Log.d("EditTaskSheet", "  priority=$priority")
                            android.util.Log.d("EditTaskSheet", "  dueDate=$dueDate")
                            android.util.Log.d("EditTaskSheet", "  reminderEnabled=$reminderEnabled")
                            android.util.Log.d("EditTaskSheet", "  reminderTime=$reminderTime")
                            android.util.Log.d("EditTaskSheet", "  finalReminderTime=$finalReminderTime")
                            
                            onSave(
                                text,
                                priority,
                                dueDate,
                                finalReminderTime
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = text.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                // Update dueDate with new date, keeping existing time
                dueDate = LocalDateTime.of(date, selectedTime)
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
            onTimeSelected = { time ->
                selectedTime = time
                // Update dueDate with new time, keeping existing date
                dueDate = LocalDateTime.of(selectedDate, time)
                    .atZone(zone)
                    .toInstant()
                    .toString()
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun NaturalLanguageTab(
    value: String,
    onValueChange: (String) -> Unit,
    currentDueDate: String?
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Type naturally...") },
            placeholder = { Text("tomorrow at 3pm") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text("Examples: \"tomorrow 9am\", \"next monday 3pm\", \"in 2 hours\"")
            }
        )
        
        if (currentDueDate != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "📅 ${Instant.parse(currentDueDate).let { NaturalDateParser.formatHumanReadable(it) }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DateTimePickerTab(
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    dueDate: String?,
    onDateChange: (String?) -> Unit,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onShowDatePicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DateRange, null)
            Spacer(Modifier.width(8.dp))
            Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
        }
        
        OutlinedButton(
            onClick = onShowTimePicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Schedule, null)
            Spacer(Modifier.width(8.dp))
            Text(selectedTime.format(DateTimeFormatter.ofPattern("h:mm a")))
        }
        
        if (dueDate != null) {
            TextButton(
                onClick = { onDateChange(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun ReminderOffsetSelector(
    selected: ReminderOffset,
    onSelect: (ReminderOffset) -> Unit
) {
    Column {
        ReminderOffset.values().forEach { offset ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == offset,
                    onClick = { onSelect(offset) }
                )
                Text(
                    text = offset.label,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

enum class ReminderOffset(val label: String) {
    AT_TIME("At time of task"),
    FIVE_MIN("5 minutes before"),
    FIFTEEN_MIN("15 minutes before"),
    ONE_HOUR("1 hour before")
}

/**
 * Date picker dialog wrapper for Material 3
 */
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
    
    androidx.compose.material3.DatePickerDialog(
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

/**
 * Time picker dialog wrapper for Material 3
 */
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
