package com.ido.app.ui.screens.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ido.app.data.model.Task
import com.ido.app.data.repository.TaskSection
import com.ido.app.ui.components.CircularCheckbox
import com.ido.app.ui.screens.edit.EditTaskSheet
import com.ido.app.ui.screens.home.HomeViewModel
import java.time.LocalDate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Revamped Tasks Screen matching iDo design specs
 * 
 * Features:
 * - Greeting header with user avatar
 * - Quick add task input
 * - Filter chips (All Tasks, Personal, Work)
 * - Today's Focus and Upcoming sections
 * - Glass-morphic task cards
 * - No Scaffold/TopAppBar - clean edge-to-edge design
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreenRevamped(
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val taskSections by viewModel.taskSections.collectAsState()
    val account by viewModel.signedInAccount.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    // Filter state - removed Personal/Work per requirements
    var selectedFilter by remember { mutableStateOf("All Tasks") }
    val filters = listOf("All Tasks")
    
    // Completed tasks section state
    var completedSectionExpanded by remember { mutableStateOf(false) }
    
    // Delete confirmation dialog state
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp) // Space for floating dock
        ) {
            // Header section
            item {
                TasksHeader(
                    account = account,
                    isDark = isDark
                )
            }
            
            // Quick add task input (dark mode design)
            if (isDark) {
                item {
                    QuickAddTaskInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = { viewModel.showCreateTask() }
                    )
                }
            }
            
            // Filter chips
            item {
                FilterChipsRow(
                    filters = filters,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            // Today's Focus section
            val priorityTasks = taskSections[TaskSection.PRIORITY] ?: emptyList()
            val todayTasks = taskSections[TaskSection.TODAY] ?: emptyList()
            val focusTasks = (priorityTasks + todayTasks).filter { !it.done }
            
            if (focusTasks.isNotEmpty()) {
                item {
                    SectionLabel(
                        title = "TODAY'S FOCUS",
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                }
                
                items(focusTasks.take(5), key = { "focus_${it.id}" }) { task ->
                    TaskCardRevamped(
                        task = task,
                        isPriority = task.priority,
                        onClick = { viewModel.showEditTask(task) },
                        onToggleDone = { viewModel.toggleTaskDone(task.id) },
                        onLongPress = { taskToDelete = task },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Upcoming section
            val laterTasks = taskSections[TaskSection.LATER] ?: emptyList()
            val unscheduledTasks = taskSections[TaskSection.UNSCHEDULED] ?: emptyList()
            val upcomingTasks = (laterTasks + unscheduledTasks).filter { !it.done }
            
            if (upcomingTasks.isNotEmpty()) {
                item {
                    SectionLabel(
                        title = "UPCOMING",
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                }
                
                items(upcomingTasks, key = { "upcoming_${it.id}" }) { task ->
                    TaskCardRevamped(
                        task = task,
                        isPriority = false,
                        isUpcoming = true,
                        onClick = { viewModel.showEditTask(task) },
                        onToggleDone = { viewModel.toggleTaskDone(task.id) },
                        onLongPress = { taskToDelete = task },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Empty state (only show if both pending and completed are empty)
            val allCompletedTasks = taskSections.values.flatten().filter { it.done }
            if (focusTasks.isEmpty() && upcomingTasks.isEmpty() && allCompletedTasks.isEmpty()) {
                item {
                    EmptyStateRevamped(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp)
                    )
                }
            }
            
            // Completed Tasks Section (collapsible)
            val completedTasks = uiState.tasks.filter { it.done && !it.deleted }
            if (completedTasks.isNotEmpty()) {
                item {
                    CompletedTasksHeader(
                        isExpanded = completedSectionExpanded,
                        taskCount = completedTasks.size,
                        onClick = { completedSectionExpanded = !completedSectionExpanded },
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                }
                
                if (completedSectionExpanded) {
                    items(completedTasks, key = { "completed_${it.id}" }) { task ->
                        TaskCardRevamped(
                            task = task,
                            isPriority = false,
                            onClick = { viewModel.showEditTask(task) },
                            onToggleDone = { viewModel.toggleTaskDone(task.id) },
                            onLongPress = { taskToDelete = task },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.showCreateTask() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 100.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add task")
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
    
    // Delete confirmation dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TasksHeader(
    account: GoogleSignInAccount?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val currentTime = remember { LocalTime.now() }
    val greeting = when {
        currentTime.hour < 12 -> "Good morning"
        currentTime.hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    val today = LocalDate.now().format(dateFormatter)
    val userName = account?.givenName ?: "there"
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 56.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            if (account?.photoUrl != null) {
                AsyncImage(
                    model = account.photoUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Calendar icon
            IconButton(onClick = { /* Navigate to calendar */ }) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date label (dark mode)
        if (isDark) {
            Text(
                text = today.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Greeting
        Text(
            text = "$greeting,",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDark) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground
        )
        
        // Date (light mode only)
        if (!isDark) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = today,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAddTaskInput(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.AddCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Add a new task...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = null,
                shape = RoundedCornerShape(50)
            )
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

@Composable
private fun CompletedTasksHeader(
    isExpanded: Boolean,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "COMPLETED TASKS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = taskCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TaskCardRevamped(
    task: Task,
    isPriority: Boolean,
    isUpcoming: Boolean = false,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular checkbox
            CircularCheckbox(
                checked = task.done,
                onCheckedChange = { 
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleDone() 
                },
                checkedColor = if (isPriority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                uncheckedBorderColor = if (isPriority) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Task content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
                )
                
                // Time or subtitle
                val subtitle = buildTaskSubtitle(task, isUpcoming)
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Priority/status indicator
            if (isPriority) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
            
            // More menu
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun buildTaskSubtitle(task: Task, isUpcoming: Boolean): String {
    return when {
        task.dueDate != null -> {
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
            
            if (isUpcoming) {
                task.dueDate!!.format(dateFormatter)
            } else {
                task.dueDate!!.format(timeFormatter)
            }
        }
        isUpcoming -> "Tomorrow"
        else -> ""
    }
}

@Composable
private fun EmptyStateRevamped(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "All caught up!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap + to add a new task",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
