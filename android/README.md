# iDo Android App

Material 3 Android app for task management with Google Drive sync and offline-first architecture.

## Features

- **Material You Design**: Dynamic theming with Material 3
- **Offline-First**: Works without internet, syncs when connected
- **Google Drive Sync**: Cross-platform sync with web app via Drive
- **Natural Language Dates**: Type "tomorrow at 3pm" or "next Monday"
- **Smart Notifications**: Task reminders with customizable offsets
- **Conflict Resolution**: Last-write-wins based on timestamps

## Prerequisites

1. **Android Studio**: Hedgehog (2023.1.1) or newer
2. **JDK**: 17 or newer
3. **Min SDK**: 26 (Android 8.0)
4. **Target SDK**: 34 (Android 14)

## Setup Instructions

### 1. Google Cloud Console Configuration

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable **Google Drive API**:
   - Navigate to "APIs & Services" → "Library"
   - Search for "Google Drive API"
   - Click "Enable"

4. Create OAuth 2.0 credentials:
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "OAuth client ID"
   - Application type: **Android**
   - Package name: `com.ido.app`
   - SHA-1 certificate fingerprint: Get from Android Studio or keystore

5. Get SHA-1 fingerprint:
   ```bash
   # Debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # Or use Android Studio: Build → Generate Signed Bundle → View Details
   ```

### 2. Project Configuration

1. Clone the repository
2. Open `android` folder in Android Studio
3. Sync Gradle files
4. Update `build.gradle.kts` if needed with your signing configuration

### 3. Build & Run

```bash
# From android directory
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or use Android Studio: Run → Run 'app'
```

## Architecture

```
app/
├── data/
│   ├── model/Task.kt              # Task data class with schema v2.0
│   ├── local/LocalDataSource.kt   # JSON file storage + migration
│   ├── remote/DriveDataSource.kt  # Google Drive API integration
│   └── repository/TaskRepository.kt # Offline-first repository
├── ui/
│   ├── screens/
│   │   ├── home/HomeScreen.kt     # Task list UI
│   │   └── edit/EditTaskScreen.kt # Task editor with natural date input
│   └── theme/Theme.kt             # Material 3 theming
├── sync/SyncManager.kt            # Background sync coordination
├── notifications/TaskNotificationManager.kt # Reminder scheduling
└── util/NaturalDateParser.kt      # "tomorrow at 3pm" → Date
```

## JSON Schema v2.0

Matches web app schema exactly:

```json
{
  "tasks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "text": "Complete project",
      "done": false,
      "priority": "medium",
      "dueDate": "2024-01-15T18:00:00Z",
      "reminderTime": "2024-01-15T17:00:00Z",
      "notified": false,
      "createdAt": "2024-01-10T10:00:00Z",
      "updatedAt": "2024-01-10T10:00:00Z",
      "deleted": false
    }
  ]
}
```

## Sync Behavior

- **On App Launch**: Loads local data immediately, syncs in background
- **On CRUD Operation**: Debounced sync after 2 seconds
- **Periodic Sync**: Every 1 hour in background
- **Conflict Resolution**: Last-write-wins based on `updatedAt` timestamp
- **Network Handling**: Requires Wi-Fi or cellular for sync

## Natural Language Examples

- `tomorrow at 3pm` → Tomorrow at 3:00 PM
- `next monday 9am` → Next Monday at 9:00 AM  
- `in 2 hours` → 2 hours from now
- `friday` → This Friday at current time
- `jan 15 at 2:30pm` → January 15 at 2:30 PM

## Notifications

- **Channel**: "Task Reminders" (high priority)
- **Scheduling**: WorkManager (reliable background execution)
- **Trigger**: Based on `reminderTime` field
- **Content**: Task text + due date
- **Actions**: Mark as done, snooze (future enhancement)

## Troubleshooting

### Sign-in fails
- Verify SHA-1 fingerprint matches in Google Cloud Console
- Check package name is `com.ido.app`
- Enable Google Drive API in Cloud Console

### Sync not working
- Check internet connection
- Verify Google Drive API is enabled
- Check Logcat for error messages

### Build errors
- Sync Gradle files: File → Sync Project with Gradle Files
- Invalidate caches: File → Invalidate Caches / Restart
- Check JDK version: File → Project Structure → SDK Location

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Release Build

1. Generate signing key:
   ```bash
   keytool -genkey -v -keystore ido-release.keystore -alias ido -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Update `app/build.gradle.kts` with signing config
3. Build release APK:
   ```bash
   ./gradlew assembleRelease
   ```

## License

MIT License - See web app for details

## Support

For issues or questions, check the documentation in `/docs`:
- `ANDROID_MIGRATION_GUIDE.md` - Detailed technical guide
- `SCHEMA_QUICK_REFERENCE.md` - API reference
