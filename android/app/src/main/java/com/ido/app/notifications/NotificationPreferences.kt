package com.ido.app.notifications

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User preferences for notification and sync settings
 * 
 * Manages:
 * - Background sync interval (15min, 30min, 1hr)
 * - Notification display preferences
 */
class NotificationPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _syncInterval = MutableStateFlow(getSyncIntervalFromPrefs())
    val syncInterval: StateFlow<SyncInterval> = _syncInterval.asStateFlow()
    
    /**
     * Get sync interval from preferences
     */
    private fun getSyncIntervalFromPrefs(): SyncInterval {
        val ordinal = prefs.getInt(KEY_SYNC_INTERVAL, SyncInterval.FIFTEEN_MINUTES.ordinal)
        return SyncInterval.entries.getOrElse(ordinal) { SyncInterval.FIFTEEN_MINUTES }
    }
    
    /**
     * Set sync interval
     */
    fun setSyncInterval(interval: SyncInterval) {
        prefs.edit().putInt(KEY_SYNC_INTERVAL, interval.ordinal).apply()
        _syncInterval.value = interval
    }
    
    /**
     * Check if user has seen the sync explanation
     */
    fun hasSeenSyncExplanation(): Boolean {
        return prefs.getBoolean(KEY_SEEN_SYNC_EXPLANATION, false)
    }
    
    /**
     * Mark sync explanation as seen
     */
    fun markSyncExplanationSeen() {
        prefs.edit().putBoolean(KEY_SEEN_SYNC_EXPLANATION, true).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
        private const val KEY_SEEN_SYNC_EXPLANATION = "seen_sync_explanation"
        
        @Volatile
        private var INSTANCE: NotificationPreferences? = null
        
        fun getInstance(context: Context): NotificationPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * Background sync interval options
 */
enum class SyncInterval(val minutes: Long, val displayName: String) {
    FIFTEEN_MINUTES(15, "15 minutes"),
    THIRTY_MINUTES(30, "30 minutes"),
    ONE_HOUR(60, "1 hour")
}
