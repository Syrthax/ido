package com.ido.app.ui.screens.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskSheet(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String?, String?) -> Unit
) {
    var text by remember { mutableStateOf(task?.text ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: false) }
    var dueDate by remember { mutableStateOf(task?.dueDate) }
    var reminderTime by remember { mutableStateOf(task?.reminderTime) }
    
    var selectedTab by remember { mutableStateOf(0) }
    var naturalInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(reminderTime != null) }
    var reminderOffset by remember { mutableStateOf(ReminderOffset.AT_TIME) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                        }
                    },
                    currentDueDate = dueDate
                )
                1 -> DateTimePickerTab(
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
                        // Calculate reminderTime based on offset
                        val due = Instant.parse(dueDate)
                        reminderTime = when (it) {
                            ReminderOffset.AT_TIME -> due.toString()
                            ReminderOffset.FIVE_MIN -> due.minus(Duration.ofMinutes(5)).toString()
                            ReminderOffset.FIFTEEN_MIN -> due.minus(Duration.ofMinutes(15)).toString()
                            ReminderOffset.ONE_HOUR -> due.minus(Duration.ofHours(1)).toString()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
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
                            onSave(
                                text,
                                priority,
                                dueDate,
                                if (reminderEnabled) reminderTime else null
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
            Text(
                if (dueDate != null) {
                    val date = LocalDateTime.ofInstant(Instant.parse(dueDate), ZoneId.systemDefault())
                    date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                } else {
                    "Select Date"
                }
            )
        }
        
        OutlinedButton(
            onClick = onShowTimePicker,
            modifier = Modifier.fillMaxWidth(),
            enabled = dueDate != null
        ) {
            Icon(Icons.Default.Schedule, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (dueDate != null) {
                    val date = LocalDateTime.ofInstant(Instant.parse(dueDate), ZoneId.systemDefault())
                    date.format(DateTimeFormatter.ofPattern("h:mm a"))
                } else {
                    "Select Time"
                }
            )
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
