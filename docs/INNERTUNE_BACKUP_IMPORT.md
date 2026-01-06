# InnerTune Backup Import Guide

This document describes the step-by-step sequence used to import a backup file from InnerTune into Metrolist.

## Overview

Metrolist supports importing backup files created by InnerTune (the predecessor app). The backup file is a ZIP archive containing the app's settings and database, which can be restored to migrate all your data including playlists, songs, preferences, and playback history.

## Backup File Format

An InnerTune/Metrolist backup file (`.backup` extension) is a ZIP archive containing:

1. **`settings.preferences_pb`** - Protocol Buffer file containing app preferences and settings
2. **`song.db`** - SQLite database file containing:
   - Songs and their metadata
   - Artists
   - Albums
   - Playlists
   - Song-Artist-Album relationships
   - Play history and statistics
   - Search history
   - Lyrics cache
   - And other app data

## Step-by-Step Import Process

### User Interface Flow

1. **Navigate to Backup & Restore Screen**
   - Open Metrolist app
   - Go to Settings
   - Select "Backup and restore" option
   
2. **Initiate Restore**
   - Tap on the "Restore" button (with restore icon)
   - Android's file picker will open

3. **Select Backup File**
   - Browse to your InnerTune backup file location
   - Select the `.backup` file (e.g., `InnerTune_20231115120000.backup`)
   - The file picker accepts files with MIME type `application/octet-stream`

4. **Automatic Restoration**
   - The app will process the backup file
   - App will restart automatically after successful restoration
   - If restoration fails, an error toast message will appear

### Technical Implementation

The restoration process is handled by the `BackupRestoreViewModel.restore()` method. Here's the detailed sequence:

#### 1. File Reading and Validation

```kotlin
context.applicationContext.contentResolver.openInputStream(uri)
```
- Opens an input stream from the selected backup file URI
- Wraps the stream in a ZipInputStream for ZIP archive extraction

#### 2. ZIP Entry Extraction

The restore process iterates through each entry in the ZIP file:

```kotlin
var entry = tryOrNull { inputStream.nextEntry }
while (entry != null) {
    // Process each entry
    entry = tryOrNull { inputStream.nextEntry }
}
```

The `tryOrNull` wrapper prevents `ZipException` from crashing the app if the ZIP is corrupted.

#### 3. Settings Restoration

When `settings.preferences_pb` entry is found:

```kotlin
when (entry.name) {
    SETTINGS_FILENAME -> {
        (context.filesDir / "datastore" / SETTINGS_FILENAME)
            .outputStream()
            .use { outputStream ->
                inputStream.copyTo(outputStream)
            }
    }
}
```

- Extracts the settings file from the ZIP
- Writes it to the app's datastore directory: `{app_files_dir}/datastore/settings.preferences_pb`
- This overwrites existing settings with the backed-up preferences

#### 4. Database Restoration

When `song.db` entry is found:

```kotlin
InternalDatabase.DB_NAME -> {
    // 1. Checkpoint current database
    runBlocking(Dispatchers.IO) {
        database.checkpoint()
    }
    
    // 2. Close database connection
    database.close()
    
    // 3. Replace database file
    FileOutputStream(database.openHelper.writableDatabase.path)
        .use { outputStream ->
            inputStream.copyTo(outputStream)
        }
}
```

**Step 4.1: Database Checkpoint**
- Forces all pending database writes to disk
- Ensures database is in a consistent state before replacement
- Runs on IO dispatcher to avoid blocking the main thread

**Step 4.2: Close Database**
- Closes all active database connections
- Prevents file access conflicts during replacement

**Step 4.3: File Replacement**
- Extracts the database from ZIP
- Overwrites the existing database file at its system path
- The database contains all songs, playlists, artists, albums, and relationships

#### 5. Cleanup and Restart

After all entries are processed:

```kotlin
// 1. Stop music service
context.stopService(Intent(context, MusicService::class.java))

// 2. Delete persistent queue file
context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()

// 3. Restart app
context.startActivity(Intent(context, MainActivity::class.java))
exitProcess(0)
```

**Step 5.1: Stop Music Service**
- Stops the background music playback service
- Ensures no active playback is using the old database

**Step 5.2: Clear Queue**
- Deletes the persistent queue file
- The queue will be recreated with data from the restored database

**Step 5.3: App Restart**
- Launches MainActivity with fresh intent
- Exits the current process with status 0
- Forces a clean app restart to initialize with restored data

#### 6. Error Handling

If any error occurs during restoration:

```kotlin
.onFailure {
    reportException(it)
    Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
}
```

- Logs the exception for debugging
- Shows a user-friendly error toast: "Failed to restore backup"
- App continues running with existing data unchanged

## Database Migration

### Database Schema Version

The database uses Room's migration system with the current version at **24**. The schema includes:

**Core Entities:**
- `SongEntity` - Song information (ID, title, duration, thumbnails, etc.)
- `ArtistEntity` - Artist information
- `AlbumEntity` - Album information
- `PlaylistEntity` - User and online playlists
- Various mapping tables for relationships

**Key Features:**
- Automatic migrations from version 2 to 24
- Manual migration for version 1 to 2 (legacy InnerTune format)
- Foreign key constraints for data integrity
- Indexed columns for performance

### Compatibility

Metrolist can restore backups from:
- **InnerTune** - Original app (requires migration from older schema versions)
- **OuterTune** - Fork with compatible schema
- **Metrolist** - Native backups (latest schema version)

Room's auto-migration system handles schema differences automatically when opening the restored database.

## Data Preserved During Import

When importing an InnerTune backup, the following data is restored:

### Settings & Preferences
- Audio quality preferences
- Theme and appearance settings
- Playback settings (gapless, crossfade, etc.)
- Library organization preferences
- Account sync settings (if logged in)
- And all other app configurations

### Music Library
- **Songs**: All cached and library songs with metadata
- **Artists**: Artist information and thumbnails
- **Albums**: Album details, artwork, and track listings
- **Playlists**: All local and synced playlists with song orders

### Playback Data
- Play counts for each song
- Total play time statistics
- Recently played history
- Liked/favorited songs

### Other Data
- Search history
- Cached lyrics
- Format information (audio quality data)
- Download states
- Library timestamps

## Important Notes

### Data Loss Warning
⚠️ **The restore operation completely replaces your current data.** 

Before restoring:
1. Create a backup of your current Metrolist data if needed
2. Ensure the backup file is from a compatible version
3. Verify the backup file is not corrupted

### App Behavior
- The app **automatically restarts** after successful restoration
- Music playback is stopped during restoration
- The current playback queue is cleared
- Network connections are closed and re-established

### File Format
- Backup files use `.backup` extension
- MIME type: `application/octet-stream`
- ZIP compression is used to reduce file size
- Files are typically named with timestamp: `Metrolist_YYYYMMDDHHMMSS.backup`

### Troubleshooting

**Restore fails with error toast:**
- Verify the backup file is not corrupted
- Ensure the backup file is from InnerTune/OuterTune/Metrolist
- Check that you have sufficient storage space
- Try restarting the app and attempting again

**App crashes after restore:**
- The backup may be from an incompatible app version
- Clear app data and reinstall (you'll lose the restored data)
- Create a new backup from the source app

**Missing data after restore:**
- Some data may not have been included in the original backup
- Check if the backup was created successfully in the source app
- Online/synced content may need to be re-downloaded

## Technical Reference

### Source Files
- **ViewModel**: `/app/src/main/kotlin/com/metrolist/music/viewmodels/BackupRestoreViewModel.kt`
- **UI Screen**: `/app/src/main/kotlin/com/metrolist/music/ui/screens/settings/BackupAndRestore.kt`
- **Database**: `/app/src/main/kotlin/com/metrolist/music/db/MusicDatabase.kt`

### Constants
- `SETTINGS_FILENAME = "settings.preferences_pb"`
- `InternalDatabase.DB_NAME = "song.db"`
- `PERSISTENT_QUEUE_FILE` - Defined in MusicService

### Related Features
- Backup creation (creates compatible backup files)
- Playlist import from M3U files
- Playlist import from CSV files
- Database migrations for version compatibility

## Conclusion

The InnerTune backup import process is a robust, well-structured system that:
1. Safely extracts settings and database from a ZIP archive
2. Gracefully handles errors and corrupted files
3. Properly cleans up and restarts the app
4. Preserves all user data and preferences
5. Maintains compatibility across app versions through migrations

This allows users to seamlessly migrate from InnerTune to Metrolist without losing any data.
