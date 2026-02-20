package com.ido.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ido.app.ui.navigation.Tab

/**
 * Floating pill-shaped navigation dock matching iDo design specs.
 * 
 * Design specs:
 * - Pill-shaped container with glass/blur effect
 * - 3 navigation icons: Tasks, Calendar, Settings/Profile
 * - Active icon is filled with primary color
 * - Inactive icons are outlined and muted
 * - Active indicator dot below selected item
 * - Smooth haptic feedback on selection
 * - Centered at bottom with safe area padding
 */
@Composable
fun FloatingDock(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dock container with glass effect
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(50),
                    spotColor = if (isDark) Color.Black else Color(0x33000000)
                )
                .clip(RoundedCornerShape(50)),
            color = if (isDark) {
                Color(0xCC1C1C1E) // Semi-transparent dark
            } else {
                Color(0xE6FFFFFF) // Semi-transparent white
            },
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tab.entries.forEach { tab ->
                    DockItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = {
                            // Haptic feedback
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            onTabSelected(tab)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animation for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dockItemScale"
    )
    
    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics {
                role = Role.Tab
                contentDescription = tab.label
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = getTabIcon(tab, isSelected),
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier
                .size((28 * scale).dp)
        )
        
        // Active indicator dot
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Returns the appropriate icon for each tab based on selection state
 */
private fun getTabIcon(tab: Tab, isSelected: Boolean): ImageVector {
    return when (tab) {
        Tab.TASKS -> if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
        Tab.CALENDAR -> if (isSelected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
        Tab.SETTINGS -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
    }
}
