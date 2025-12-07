package com.ido.app.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.ido.app.data.model.Task
import com.ido.app.data.model.TaskCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

/**
 * Google Drive remote data source
 * Handles authentication and file operations in Drive App Data folder
 */
class DriveDataSource(private val context: Context) {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private var driveService: Drive? = null
    private var fileId: String? = null
    
    /**
     * Get Google Sign-In options for Drive access
     */
    fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }
    
    /**
     * Initialize Drive service with signed-in account
     */
    fun initializeDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName(APP_NAME)
            .build()
    }
    
    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && driveService != null
    }
    
    /**
     * Get current signed-in account
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * Load tasks from Google Drive
     */
    suspend fun loadTasks(): List<Task>? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: run {
                android.util.Log.e("DriveDataSource", "Drive service not initialized")
                return@withContext null
            }
            
            // Find or create the file
            val id = findOrCreateFile(service)
            fileId = id
            android.util.Log.d("DriveDataSource", "Loading tasks from file: $id")
            
            // Download file content
            val outputStream = ByteArrayOutputStream()
            service.files().get(id)
                .executeMediaAndDownloadTo(outputStream)
            
            val jsonString = outputStream.toString("UTF-8")
            android.util.Log.d("DriveDataSource", "Loaded JSON: ${jsonString.take(200)}...")
            
            val collection = json.decodeFromString<TaskCollection>(jsonString)
            android.util.Log.d("DriveDataSource", "Loaded ${collection.tasks.size} tasks from Drive")
            
            collection.tasks
        } catch (e: Exception) {
            android.util.Log.e("DriveDataSource", "Error loading tasks", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save tasks to Google Drive
     */
    suspend fun saveTasks(tasks: List<Task>): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: run {
                android.util.Log.e("DriveDataSource", "Drive service not initialized")
                return@withContext false
            }
            
            // Find or create the file
            val id = fileId ?: findOrCreateFile(service)
            fileId = id
            android.util.Log.d("DriveDataSource", "Saving ${tasks.size} tasks to file: $id")
            
            // Prepare content
            val collection = TaskCollection(tasks)
            val jsonString = json.encodeToString(collection)
            android.util.Log.d("DriveDataSource", "JSON to save: ${jsonString.take(200)}...")
            
            val content = com.google.api.client.http.ByteArrayContent(
                "application/json",
                jsonString.toByteArray()
            )
            
            // Update file
            service.files().update(id, null, content).execute()
            android.util.Log.d("DriveDataSource", "Successfully saved tasks to Drive")
            
            true
        } catch (e: Exception) {
            android.util.Log.e("DriveDataSource", "Error saving tasks", e)
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Find existing file or create new one in App Data folder
     */
    private fun findOrCreateFile(service: Drive): String {
        try {
            // Search for existing file
            val query = "name='$FILE_NAME' and trashed=false"
            val result = service.files().list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .setFields("files(id, name)")
                .execute()
            
            val files = result.files
            if (files != null && files.isNotEmpty()) {
                return files[0].id
            }
            
            // File doesn't exist, create it
            return createFile(service)
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Create new file in App Data folder
     */
    private fun createFile(service: Drive): String {
        val fileMetadata = File().apply {
            name = FILE_NAME
            parents = listOf("appDataFolder")
        }
        
        // Initial empty content
        val initialContent = TaskCollection(emptyList())
        val jsonString = json.encodeToString(initialContent)
        val content = com.google.api.client.http.ByteArrayContent(
            "application/json",
            jsonString.toByteArray()
        )
        
        val file = service.files().create(fileMetadata, content)
            .setFields("id")
            .execute()
        
        return file.id
    }
    
    /**
     * Sign out and clear Drive service
     */
    fun signOut() {
        driveService = null
        fileId = null
    }
    
    companion object {
        private const val APP_NAME = "iDo Task Manager"
        private const val FILE_NAME = "ido_tasks.json"
    }
}
