package com.ido.app.ui.screens.home

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ido.app.data.model.Task
import com.ido.app.data.repository.SyncStatus
import com.ido.app.ui.components.CircularCheckbox
import com.ido.app.ui.screens.edit.EditTaskSheet
import com.ido.app.util.NaturalDateParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val taskSections by viewModel.taskSections.collectAsState()
    
    // Section collapse states - completed starts collapsed
    var isPriorityExpanded by rememberSaveable { mutableStateOf(true) }
    var isTodayExpanded by rememberSaveable { mutableStateOf(true) }
    var isCompletedExpanded by rememberSaveable { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("iDo") },
                actions = {
                    // Sync status indicator
                    SyncStatusIndicator(
                        syncStatus = uiState.syncStatus,
                        onClick = { viewModel.syncNow() }
                    )
                    
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showCreateTask() },
                icon = { Icon(Icons.Default.Add, "Add task") },
                text = { Text("New Task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.tasks.isEmpty()) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                SectionedTaskList(
                    tasks = uiState.tasks,
                    isPriorityExpanded = isPriorityExpanded,
                    isTodayExpanded = isTodayExpanded,
                    isCompletedExpanded = isCompletedExpanded,
                    onTogglePrioritySection = { isPriorityExpanded = !isPriorityExpanded },
                    onToggleTodaySection = { isTodayExpanded = !isTodayExpanded },
                    onToggleCompletedSection = { isCompletedExpanded = !isCompletedExpanded },
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
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    onClick: () -> Unit
) {
    val view = LocalView.current
    
    IconButton(onClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        onClick()
    }) {
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
                    contentDescription = "Synced - tap to sync now",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            is SyncStatus.Error -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Sync error - tap to retry",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Tap to sync",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionedTaskList(
    tasks: List<Task>,
    isPriorityExpanded: Boolean,
    isTodayExpanded: Boolean,
    isCompletedExpanded: Boolean,
    onTogglePrioritySection: () -> Unit,
    onToggleTodaySection: () -> Unit,
    onToggleCompletedSection: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleDone: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    // Split tasks into sections
    val priorityTasks = tasks.filter { it.priority && !it.done }
    val todayTasks = tasks.filter { !it.priority && !it.done }
    val completedTasks = tasks.filter { it.done }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Priority Section
        if (priorityTasks.isNotEmpty()) {
            item(key = "priority_header") {
                SectionHeader(
                    title = "Priority",
                    count = priorityTasks.size,
                    isExpanded = isPriorityExpanded,
                    onClick = onTogglePrioritySection,
                    icon = Icons.Default.Star,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
            
            if (isPriorityExpanded) {
                items(priorityTasks, key = { "priority_${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onToggleDone = { onToggleDone(task.id) },
                        onTogglePriority = { onTogglePriority(task.id) },
                        onDelete = { onDelete(task.id) }
                    )
                }
            }
        }
        
        // Today Section
        if (todayTasks.isNotEmpty()) {
            item(key = "today_header") {
                SectionHeader(
                    title = "Today",
                    count = todayTasks.size,
                    isExpanded = isTodayExpanded,
                    onClick = onToggleTodaySection,
                    icon = Icons.Default.Today,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (isTodayExpanded) {
                items(todayTasks, key = { "today_${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onToggleDone = { onToggleDone(task.id) },
                        onTogglePriority = { onTogglePriority(task.id) },
                        onDelete = { onDelete(task.id) }
                    )
                }
            }
        }
        
        // Completed Section
        if (completedTasks.isNotEmpty()) {
            item(key = "completed_header") {
                SectionHeader(
                    title = "Completed",
                    count = completedTasks.size,
                    isExpanded = isCompletedExpanded,
                    onClick = onToggleCompletedSection,
                    icon = Icons.Default.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
            
            if (isCompletedExpanded) {
                items(completedTasks, key = { "completed_${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onToggleDone = { onToggleDone(task.id) },
                        onTogglePriority = { onTogglePriority(task.id) },
                        onDelete = { onDelete(task.id) },
                        isCompleted = true
                    )
                }
            }
        }
        
        // Empty state if all tasks are completed and collapsed
        if (priorityTasks.isEmpty() && todayTasks.isEmpty() && !isCompletedExpanded && completedTasks.isNotEmpty()) {
            item(key = "all_done_message") {
                Text(
                    text = "All caught up! 🎉",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)
                )
            }
        }
        
        // Bottom spacer for FAB
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    val view = LocalView.current
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chevronRotation"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                onClick()
            }
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onTogglePriority: () -> Unit,
    onDelete: () -> Unit,
    isCompleted: Boolean = false
) {
    val view = LocalView.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Completed tasks have reduced opacity
    val cardAlpha = if (isCompleted) 0.6f else 1f
    
    Card(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.isOverdue() && !isCompleted -> MaterialTheme.colorScheme.errorContainer
                task.priority && !isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Checkbox
            CircularCheckbox(
                checked = task.done,
                onCheckedChange = { onToggleDone() },
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
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Due date row
                if (task.dueDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (task.isOverdue() && !task.done) 
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
                            color = if (task.isOverdue() && !task.done) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Priority button (only show for non-completed tasks)
            if (!isCompleted) {
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        onTogglePriority()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (task.priority) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Toggle priority",
                        tint = if (task.priority) 
                            MaterialTheme.colorScheme.tertiary
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Delete button
            IconButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    showDeleteDialog = true
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete \"${task.text}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
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
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No tasks yet!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to create your first task",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
