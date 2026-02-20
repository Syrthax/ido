package com.ido.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility class for haptic feedback throughout the app.
 * 
 * Provides consistent haptic patterns for different interactions:
 * - Light: Subtle feedback for toggles, selections
 * - Medium: Standard feedback for button taps
 * - Heavy: Confirmation feedback for important actions
 * - Success: Positive feedback for completed actions
 * - Error: Warning feedback for errors
 */
object HapticUtils {
    
    /**
     * Perform light haptic feedback (checkbox toggle, selection change)
     */
    fun performLightHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * Perform medium haptic feedback (button tap, card tap)
     */
    fun performMediumHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Perform heavy haptic feedback (delete, important toggle)
     */
    fun performHeavyHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Perform success haptic feedback (task completed, sync success)
     */
    fun performSuccessHaptic(context: Context) {
        vibrate(context, longArrayOf(0, 30, 50, 30), intArrayOf(0, 100, 0, 200))
    }
    
    /**
     * Perform error haptic feedback (sync failed, validation error)
     */
    fun performErrorHaptic(context: Context) {
        vibrate(context, longArrayOf(0, 100, 50, 100), intArrayOf(0, 255, 0, 255))
    }
    
    /**
     * Perform tick haptic (list scroll snap, slider tick)
     */
    fun performTickHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        }
    }
    
    /**
     * Private helper for custom vibration patterns
     */
    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator = getVibrator(context) ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (vibrator.hasAmplitudeControl()) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
    
    /**
     * Get system vibrator service
     */
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
