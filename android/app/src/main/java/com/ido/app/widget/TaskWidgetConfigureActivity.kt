package com.ido.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ido.app.ui.theme.IDoTheme

/**
 * Configuration activity for the widget.
 * Allows user to select initial section (Priority or Today).
 */
class TaskWidgetConfigureActivity : ComponentActivity() {
    
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set result to CANCELED initially
        setResult(Activity.RESULT_CANCELED)
        
        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        // If the ID is invalid, finish immediately
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        setContent {
            IDoTheme {
                ConfigurationScreen(
                    onSectionSelected = { section ->
                        saveWidgetConfiguration(section)
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }
    
    private fun saveWidgetConfiguration(section: Int) {
        // Save the section preference
        TaskWidgetProvider.setSection(this, appWidgetId, section)
        
        // Update the widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        TaskWidgetProvider.updateAllWidgets(this)
        
        // Return the result
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationScreen(
    onSectionSelected: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(TaskWidgetProvider.SECTION_PRIORITY) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Widget") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select which tasks to show:",
                style = MaterialTheme.typography.bodyLarge
            )
            
            // Priority option
            FilterChip(
                selected = selectedSection == TaskWidgetProvider.SECTION_PRIORITY,
                onClick = { selectedSection = TaskWidgetProvider.SECTION_PRIORITY },
                label = { Text("Priority Tasks") },
                leadingIcon = if (selectedSection == TaskWidgetProvider.SECTION_PRIORITY) {
                    { Text("⭐") }
                } else null
            )
            
            // Today option  
            FilterChip(
                selected = selectedSection == TaskWidgetProvider.SECTION_TODAY,
                onClick = { selectedSection = TaskWidgetProvider.SECTION_TODAY },
                label = { Text("Today's Tasks") },
                leadingIcon = if (selectedSection == TaskWidgetProvider.SECTION_TODAY) {
                    { Text("📅") }
                } else null
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Tip: Tap the widget title to switch between sections anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                
                Button(onClick = { onSectionSelected(selectedSection) }) {
                    Text("Add Widget")
                }
            }
        }
    }
}
