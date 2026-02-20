package com.ido.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular checkbox with animated checkmark and haptic feedback.
 * 
 * Design specs:
 * - Unchecked: Empty circle with themed border
 * - Checked: Filled circle with animated checkmark
 * - Smooth spring animation on state change
 * - Haptic feedback on toggle
 */
@Composable
fun CircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedBorderColor: Color = MaterialTheme.colorScheme.outline,
    checkmarkColor: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true
) {
    val view = LocalView.current
    
    // Animation for fill transition
    val fillAnimation by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fillAnimation"
    )
    
    // Animation for checkmark drawing
    val checkmarkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = if (checked) 100 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "checkmarkProgress"
    )
    
    // Scale bounce animation
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scaleAnimation"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                role = Role.Checkbox
                contentDescription = if (checked) "Checked" else "Unchecked"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    // Trigger haptic feedback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    onCheckedChange(!checked)
                }
            )
    ) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        val radius = (this.size.minDimension / 2) * scale
        val strokeWidth = 2.dp.toPx()
        
        // Draw border circle (always visible)
        drawCircle(
            color = if (fillAnimation > 0f) {
                checkedColor.copy(alpha = 1f - fillAnimation * 0.3f)
            } else {
                uncheckedBorderColor
            },
            radius = radius - strokeWidth / 2,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth)
        )
        
        // Draw filled circle (animated)
        if (fillAnimation > 0f) {
            drawCircle(
                color = checkedColor,
                radius = (radius - strokeWidth) * fillAnimation,
                center = Offset(centerX, centerY)
            )
        }
        
        // Draw checkmark (animated)
        if (checkmarkProgress > 0f) {
            val checkPath = Path().apply {
                // Checkmark proportions relative to size
                val checkStartX = centerX - radius * 0.3f
                val checkStartY = centerY
                val checkMidX = centerX - radius * 0.05f
                val checkMidY = centerY + radius * 0.25f
                val checkEndX = centerX + radius * 0.35f
                val checkEndY = centerY - radius * 0.2f
                
                // First segment of checkmark
                if (checkmarkProgress <= 0.5f) {
                    val progress = checkmarkProgress * 2
                    moveTo(checkStartX, checkStartY)
                    lineTo(
                        checkStartX + (checkMidX - checkStartX) * progress,
                        checkStartY + (checkMidY - checkStartY) * progress
                    )
                } else {
                    // First segment complete, draw second segment
                    val progress = (checkmarkProgress - 0.5f) * 2
                    moveTo(checkStartX, checkStartY)
                    lineTo(checkMidX, checkMidY)
                    lineTo(
                        checkMidX + (checkEndX - checkMidX) * progress,
                        checkMidY + (checkEndY - checkMidY) * progress
                    )
                }
            }
            
            drawPath(
                path = checkPath,
                color = checkmarkColor,
                style = Stroke(
                    width = strokeWidth * 1.2f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Preview-friendly version with default parameters
 */
@Composable
fun CircularCheckboxPreview() {
    var checked by remember { mutableStateOf(false) }
    CircularCheckbox(
        checked = checked,
        onCheckedChange = { checked = it }
    )
}
