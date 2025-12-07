package com.ido.app.sync

import android.content.Context
import androidx.work.*
import com.ido.app.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Sync manager for background synchronization with Google Drive
 * Implements debounced sync to avoid excessive API calls
 */
class SyncManager(context: Context, private val repository: TaskRepository) {
    
    private val workManager = WorkManager.getInstance(context)
    private val _lastSyncTime = MutableStateFlow<Long>(0)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime
    
    /**
     * Request immediate sync
     */
    suspend fun requestSync() {
        if (!repository.isSignedIn()) return
        
        val result = repository.syncWithDrive()
        if (result is com.ido.app.data.repository.SyncResult.Success) {
            _lastSyncTime.value = System.currentTimeMillis()
        }
    }
    
    /**
     * Request debounced sync (waits 2 seconds before syncing)
     */
    suspend fun requestDebouncedSync() {
        if (!repository.isSignedIn()) return
        
        // Cancel any pending sync work
        workManager.cancelAllWorkByTag(SYNC_WORK_TAG)
        
        // Schedule new sync work with delay
        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS)
            .addTag(SYNC_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueue(syncWork)
    }
    
    /**
     * Schedule periodic background sync
     */
    fun schedulePeriodicSync() {
        if (!repository.isSignedIn()) return
        
        val periodicWork = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(PERIODIC_SYNC_TAG)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
    
    /**
     * Cancel all scheduled sync work
     */
    fun cancelSync() {
        workManager.cancelAllWorkByTag(SYNC_WORK_TAG)
        workManager.cancelAllWorkByTag(PERIODIC_SYNC_TAG)
    }
    
    companion object {
        private const val SYNC_WORK_TAG = "sync_work"
        private const val PERIODIC_SYNC_TAG = "periodic_sync"
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
        private const val DEBOUNCE_DELAY_MS = 2000L
        private const val PERIODIC_SYNC_INTERVAL_HOURS = 1L
    }
}

/**
 * Worker for background sync
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Initialize repository
            val repository = TaskRepository(applicationContext)
            
            if (!repository.isSignedIn()) {
                return@withContext Result.success()
            }
            
            // Perform sync
            val result = repository.syncWithDrive()
            
            when (result) {
                is com.ido.app.data.repository.SyncResult.Success -> Result.success()
                is com.ido.app.data.repository.SyncResult.Error -> Result.retry()
                is com.ido.app.data.repository.SyncResult.NotSignedIn -> Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
