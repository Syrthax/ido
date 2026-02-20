package com.ido.app.ui.screens.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ido.app.data.model.Task
import com.ido.app.data.repository.SyncStatus
import com.ido.app.data.repository.TaskSection
import com.ido.app.ui.components.CircularCheckbox
import com.ido.app.ui.screens.edit.EditTaskSheet
import com.ido.app.ui.screens.home.HomeViewModel
import com.ido.app.util.NaturalDateParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val taskSections by viewModel.taskSections.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("iDo") },
                actions = {
                    // Sync status indicator
                    SyncStatusIndicator(uiState.syncStatus)
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showCreateTask() },
                icon = { Icon(Icons.Default.Add, "Add task") },
                text = { Text("New Task") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (taskSections.values.all { it.isEmpty() }) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                SectionedTaskList(
                    sections = taskSections,
                    onTaskClick = { viewModel.showEditTask(it) },
                    onToggleDone = { viewModel.toggleTaskDone(it) },
                    onTogglePriority = { viewModel.toggleTaskPriority(it) },
                    onDelete = { viewModel.deleteTask(it) }
                )
            }
        }
    }
    
    // Edit/Create task bottom sheet
    if (uiState.showEditSheet) {
        EditTaskSheet(
            task = uiState.editingTask,
            onDismiss = { viewModel.hideEditSheet() },
            onSave = { text, priority, dueDate, reminderTime ->
                viewModel.saveTask(text, priority, dueDate, reminderTime)
            }
        )
    }
}

@Composable
fun SyncStatusIndicator(syncStatus: SyncStatus) {
    when (syncStatus) {
        is SyncStatus.Syncing -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp
            )
        }
        is SyncStatus.Synced -> {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Synced",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        is SyncStatus.Error -> {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Sync error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        else -> {}
    }
}

@Composable
fun SectionedTaskList(
    sections: Map<TaskSection, List<Task>>,
    onTaskClick: (Task) -> Unit,
    onToggleDone: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Priority section
        val priorityTasks = sections[TaskSection.PRIORITY] ?: emptyList()
        if (priorityTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Priority",
                    icon = Icons.Default.Star
                )
            }
            items(priorityTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onToggleDone = { onToggleDone(task.id) },
                    onTogglePriority = { onTogglePriority(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        
        // Today section
        val todayTasks = sections[TaskSection.TODAY] ?: emptyList()
        if (todayTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Today",
                    icon = Icons.Default.Today
                )
            }
            items(todayTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onToggleDone = { onToggleDone(task.id) },
                    onTogglePriority = { onTogglePriority(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        
        // Later section
        val laterTasks = sections[TaskSection.LATER] ?: emptyList()
        if (laterTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Later",
                    icon = Icons.Default.DateRange
                )
            }
            items(laterTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onToggleDone = { onToggleDone(task.id) },
                    onTogglePriority = { onTogglePriority(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        
        // Unscheduled section
        val unscheduledTasks = sections[TaskSection.UNSCHEDULED] ?: emptyList()
        if (unscheduledTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Unscheduled",
                    icon = Icons.Default.Inbox
                )
            }
            items(unscheduledTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onToggleDone = { onToggleDone(task.id) },
                    onTogglePriority = { onTogglePriority(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
        }
        
        // Completed section - collapsible with strikethrough styling
        val completedTasks = sections[TaskSection.COMPLETED] ?: emptyList()
        if (completedTasks.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                CollapsibleSectionHeader(
                    title = "Completed",
                    count = completedTasks.size,
                    icon = Icons.Default.CheckCircle
                )
            }
            items(completedTasks, key = { it.id }) { task ->
                CompletedTaskCard(
                    task = task,
                    onToggleDone = { onToggleDone(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Collapsible section header for Completed tasks
 * Shows count badge and can expand/collapse
 */
@Composable
fun CollapsibleSectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Completed task card with strikethrough styling
 * Only allows undo (toggle done) and delete actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTaskCard(
    task: Task,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular checkbox (allows undo - marked as completed)
            CircularCheckbox(
                checked = true,
                onCheckedChange = { 
                    onToggleDone() 
                },
                checkedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.surface
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Task text with strikethrough
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            
            // Delete button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete this completed task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onTogglePriority: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.isOverdue() -> MaterialTheme.colorScheme.errorContainer
                task.priority -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular checkbox matching app design
            CircularCheckbox(
                checked = task.done,
                onCheckedChange = { 
                    onToggleDone() 
                },
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedBorderColor = if (task.priority) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Task content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null
                )
                
                if (task.dueDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (task.isOverdue()) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.getDueDateInstant()?.let { 
                                NaturalDateParser.formatHumanReadable(it) 
                            } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (task.isOverdue()) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Priority button
            IconButton(onClick = onTogglePriority) {
                Icon(
                    imageVector = if (task.priority) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = "Toggle priority",
                    tint = if (task.priority) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Delete button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tasks yet!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to create your first task",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
