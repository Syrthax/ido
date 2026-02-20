package com.ido.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Theme mode options
 */
enum class ThemeMode {
    AUTO,   // Follow system (default)
    LIGHT,  // Always light
    DARK    // Always dark
}

/**
 * Manages theme preferences with persistence
 * 
 * Default: AUTO (follows system)
 * Changes apply immediately without restart
 */
class ThemePreferences private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    /**
     * Load saved theme mode from SharedPreferences
     */
    private fun loadThemeMode(): ThemeMode {
        val savedValue = prefs.getString(KEY_THEME_MODE, ThemeMode.AUTO.name)
        return try {
            ThemeMode.valueOf(savedValue ?: ThemeMode.AUTO.name)
        } catch (e: Exception) {
            ThemeMode.AUTO
        }
    }
    
    /**
     * Set and persist theme mode
     * Immediately updates the StateFlow for reactive UI updates
     */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }
    
    companion object {
        private const val PREFS_NAME = "ido_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        
        @Volatile
        private var instance: ThemePreferences? = null
        
        fun getInstance(context: Context): ThemePreferences {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferences(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
