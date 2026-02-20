package com.ido.app.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles notification permission state for Android 13+ (API 33+)
 * 
 * On older versions, notifications are enabled by default.
 * On API 33+, we need to request POST_NOTIFICATIONS permission.
 * 
 * This class:
 * - Tracks permission state
 * - Tracks if we've asked before (to avoid spamming)
 * - Provides guidance for settings redirect
 */
class NotificationPermissionHandler(private val context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    private val _permissionState = MutableStateFlow(checkPermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    /**
     * Check current permission state
     */
    fun checkPermissionState(): PermissionState {
        // On Android 12 and below, notifications are enabled by default
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Check if notifications are enabled via NotificationManager
            return if (notificationManager.areNotificationsEnabled()) {
                PermissionState.GRANTED
            } else {
                PermissionState.DENIED_SETTINGS
            }
        }
        
        // Android 13+ requires explicit permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        
        return when {
            hasPermission -> PermissionState.GRANTED
            hasAskedBefore() -> PermissionState.DENIED_SETTINGS
            else -> PermissionState.NOT_ASKED
        }
    }
    
    /**
     * Refresh permission state (call after returning from settings or permission dialog)
     */
    fun refreshState() {
        _permissionState.value = checkPermissionState()
    }
    
    /**
     * Mark that we've asked for permission
     */
    fun markAsked() {
        prefs.edit().putBoolean(KEY_HAS_ASKED, true).apply()
        refreshState()
    }
    
    /**
     * Check if we've asked for permission before
     */
    private fun hasAskedBefore(): Boolean {
        return prefs.getBoolean(KEY_HAS_ASKED, false)
    }
    
    /**
     * Check if notifications are effectively enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return _permissionState.value == PermissionState.GRANTED
    }
    
    /**
     * Should we prompt for permission?
     * Only if we haven't asked before and don't have permission
     */
    fun shouldPromptForPermission(): Boolean {
        return _permissionState.value == PermissionState.NOT_ASKED
    }
    
    companion object {
        private const val PREFS_NAME = "notification_permission_prefs"
        private const val KEY_HAS_ASKED = "has_asked_notification_permission"
        
        @Volatile
        private var INSTANCE: NotificationPermissionHandler? = null
        
        fun getInstance(context: Context): NotificationPermissionHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPermissionHandler(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * Permission state for notifications
 */
enum class PermissionState {
    /** Permission granted - can show notifications */
    GRANTED,
    
    /** Haven't asked yet - should request */
    NOT_ASKED,
    
    /** User denied - need to go to settings */
    DENIED_SETTINGS
}
