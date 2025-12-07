# Android Build & Setup Guide

## Quick Start

### Option 1: Open in Android Studio (Recommended)

1. **Open Android Studio**
2. **File → Open** → Select `/android` folder
3. Android Studio will detect Gradle and prompt to sync
4. Click **"Sync Project with Gradle Files"**
5. Android Studio will download Gradle wrapper automatically

### Option 2: Command Line Setup

```bash
cd android

# Initialize Gradle wrapper (requires Gradle installed)
gradle wrapper --gradle-version 8.2

# Or download wrapper manually
mkdir -p gradle/wrapper
curl -L https://services.gradle.org/distributions/gradle-8.2-bin.zip -o gradle/wrapper/gradle-8.2-bin.zip
```

## Google OAuth Configuration

**Critical**: The app won't run without Google OAuth credentials.

### Steps:

1. **Go to**: [Google Cloud Console](https://console.cloud.google.com/)

2. **Create/Select Project**: "iDo Tasks" (or any name)

3. **Enable Google Drive API**:
   - Left menu → APIs & Services → Library
   - Search "Google Drive API"
   - Click Enable

4. **Create OAuth Credentials**:
   - APIs & Services → Credentials
   - Create Credentials → OAuth client ID
   - Application type: **Android**
   - Package name: `com.ido.app`
   - SHA-1 fingerprint: See below

5. **Get SHA-1 Fingerprint**:

   ```bash
   # For debug builds (development)
   keytool -list -v \
     -keystore ~/.android/debug.keystore \
     -alias androiddebugkey \
     -storepass android \
     -keypass android
   
   # Copy the SHA-1 value and paste into Google Cloud Console
   ```

6. **Save Credentials**: Download JSON (optional for reference)

## First Build

```bash
# Open in Android Studio, then:
# Build → Make Project

# Or command line (after wrapper setup):
./gradlew assembleDebug
```

## Run on Device

1. **Enable Developer Options** on Android device:
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → System → Developer Options → Enable USB Debugging

2. **Connect Device** via USB

3. **Run in Android Studio**:
   - Run → Run 'app' (Shift+F10)
   - Select your device

4. **Or via command line**:
   ```bash
   ./gradlew installDebug
   ```

## Testing Sign-In

1. Launch app on device
2. Tap "Sign in with Google"
3. Select Google account
4. Grant Drive permissions
5. App should load and show empty task list

## Common Issues

### "Failed to sign in"
- Verify SHA-1 matches in Google Cloud Console
- Check package name is exactly `com.ido.app`
- Ensure Drive API is enabled

### "Cannot resolve symbol"
- File → Invalidate Caches / Restart
- File → Sync Project with Gradle Files

### Gradle sync fails
- Check internet connection (downloads dependencies)
- Update Android Studio to latest version
- Check `gradle.properties` JVM settings

## Project Structure

```
android/
├── app/
│   ├── build.gradle.kts           # App dependencies
│   ├── src/main/
│   │   ├── AndroidManifest.xml    # App config
│   │   ├── java/com/ido/app/      # Kotlin source
│   │   └── res/                   # Resources
│   └── proguard-rules.pro         # Release optimization
├── build.gradle.kts               # Project config
├── settings.gradle.kts            # Module settings
├── gradle.properties              # Build properties
└── README.md                      # Full documentation
```

## Next Steps

1. ✅ Complete OAuth setup
2. ✅ Build and install app
3. ✅ Sign in and grant permissions
4. Create your first task
5. Test sync with web app (both must use same Google account)

## Documentation

- `README.md` - Complete app documentation
- `/docs/ANDROID_MIGRATION_GUIDE.md` - Technical deep-dive
- `/docs/SCHEMA_QUICK_REFERENCE.md` - API reference
