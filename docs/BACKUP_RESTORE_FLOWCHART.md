# Backup & Restore Process Flowchart

This document provides visual flowcharts for the backup and restore processes in Metrolist.

## Restore Process Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    User Initiates Restore                    │
│                                                               │
│  Settings → Backup & Restore → Tap "Restore" Button         │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Android File Picker Opens                       │
│                                                               │
│  - Filter: application/octet-stream                          │
│  - User selects .backup file                                 │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│         BackupRestoreViewModel.restore() Called              │
│                                                               │
│  Input: Uri of selected backup file                          │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Open Input Stream from URI                      │
│                                                               │
│  contentResolver.openInputStream(uri)                        │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│           Wrap Stream as ZipInputStream                      │
│                                                               │
│  it.zipInputStream()                                         │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│            Begin Iterating ZIP Entries                       │
│                                                               │
│  while (entry != null) { ... }                               │
└───────────────────────────┬─────────────────────────────────┘
                            │
                ┌───────────┴───────────┐
                │                       │
                ▼                       ▼
    ┌───────────────────┐   ┌─────────────────────┐
    │ Entry: settings   │   │  Entry: song.db     │
    │  .preferences_pb  │   │                     │
    └─────────┬─────────┘   └──────────┬──────────┘
              │                         │
              ▼                         ▼
┌──────────────────────┐   ┌───────────────────────────────┐
│  Restore Settings    │   │   Restore Database            │
│                      │   │                               │
│  1. Extract to       │   │  1. database.checkpoint()     │
│     datastore/       │   │     - Flush pending writes    │
│     settings         │   │                               │
│     .preferences_pb  │   │  2. database.close()          │
│                      │   │     - Close connections       │
│  2. Overwrites       │   │                               │
│     existing         │   │  3. Replace song.db file      │
│     settings         │   │     - Extract from ZIP        │
│                      │   │     - Overwrite at path       │
└──────────┬───────────┘   └──────────┬────────────────────┘
           │                          │
           └────────────┬─────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│               All Entries Processed                          │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                 Cleanup Operations                           │
│                                                               │
│  1. context.stopService(MusicService)                        │
│     - Stop background playback                               │
│                                                               │
│  2. Delete PERSISTENT_QUEUE_FILE                             │
│     - Clear saved playback queue                             │
│                                                               │
│  3. context.startActivity(MainActivity)                      │
│     - Launch fresh app instance                              │
│                                                               │
│  4. exitProcess(0)                                           │
│     - Kill current process                                   │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  App Restarts                                │
│                                                               │
│  - MainActivity launches                                     │
│  - Database opens with restored data                         │
│  - Settings loaded from restored preferences                 │
│  - User sees restored library                                │
└─────────────────────────────────────────────────────────────┘
```

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────────┐
│              Any Step in Restore Process                     │
└───────────────────────────┬─────────────────────────────────┘
                            │
                  ┌─────────┴─────────┐
                  │                   │
                  ▼                   ▼
          ┌──────────────┐   ┌────────────────┐
          │   Success    │   │     Error      │
          └──────┬───────┘   └────────┬───────┘
                 │                    │
                 │                    ▼
                 │          ┌──────────────────────┐
                 │          │  reportException(e)  │
                 │          │  - Log to analytics  │
                 │          └──────────┬───────────┘
                 │                     │
                 │                     ▼
                 │          ┌──────────────────────┐
                 │          │  Show Error Toast    │
                 │          │  "Failed to restore  │
                 │          │   backup"            │
                 │          └──────────┬───────────┘
                 │                     │
                 │                     ▼
                 │          ┌──────────────────────┐
                 │          │  Continue Running    │
                 │          │  - No data changed   │
                 │          │  - User can retry    │
                 │          └──────────────────────┘
                 │
                 ▼
      ┌──────────────────────┐
      │   Continue Process   │
      └──────────────────────┘
```

## Backup Creation Flow (for reference)

```
┌─────────────────────────────────────────────────────────────┐
│                 User Initiates Backup                        │
│                                                               │
│  Settings → Backup & Restore → Tap "Backup" Button          │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│           Android Save File Dialog Opens                     │
│                                                               │
│  Default name: Metrolist_YYYYMMDDHHMMSS.backup              │
│  User chooses save location                                  │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│        BackupRestoreViewModel.backup() Called                │
│                                                               │
│  Input: Uri of save location                                 │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│         Open Output Stream to URI                            │
│         Create ZipOutputStream                               │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Add Settings to ZIP                             │
│                                                               │
│  1. putNextEntry("settings.preferences_pb")                  │
│  2. Read from: filesDir/datastore/settings.preferences_pb   │
│  3. Write to ZIP entry                                       │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Add Database to ZIP                             │
│                                                               │
│  1. database.checkpoint()                                    │
│     - Ensure consistent state                                │
│                                                               │
│  2. putNextEntry("song.db")                                  │
│                                                               │
│  3. Read from: database.openHelper.writableDatabase.path    │
│                                                               │
│  4. Write to ZIP entry                                       │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                Close ZIP Stream                              │
│                                                               │
│  - Finalize ZIP archive                                      │
│  - Flush buffers                                             │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│            Show Success Toast                                │
│                                                               │
│  "Backup created successfully"                               │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow During Restore

```
┌───────────────┐
│  .backup File │
│   (ZIP)       │
└───────┬───────┘
        │
        │ Extract
        │
        ├──────────────────────────────────┐
        │                                  │
        ▼                                  ▼
┌───────────────────┐            ┌─────────────────┐
│ settings          │            │    song.db      │
│ .preferences_pb   │            │                 │
└─────────┬─────────┘            └────────┬────────┘
          │                               │
          ▼                               ▼
┌─────────────────────┐          ┌──────────────────────┐
│  DataStore          │          │  Room Database       │
│  Preferences        │          │                      │
│                     │          │  ┌────────────────┐  │
│  - Audio Settings   │          │  │ Songs          │  │
│  - Theme           │          │  ├────────────────┤  │
│  - Playback        │          │  │ Artists        │  │
│  - Library         │          │  ├────────────────┤  │
│  - Account         │          │  │ Albums         │  │
│  - UI Prefs        │          │  ├────────────────┤  │
└─────────────────────┘          │  │ Playlists      │  │
                                 │  ├────────────────┤  │
                                 │  │ Relationships  │  │
                                 │  ├────────────────┤  │
                                 │  │ Play History   │  │
                                 │  ├────────────────┤  │
                                 │  │ Search History │  │
                                 │  ├────────────────┤  │
                                 │  │ Lyrics         │  │
                                 │  └────────────────┘  │
                                 └──────────────────────┘
```

## Threading Model

```
┌──────────────────────────────────────────────────────────┐
│                    Main Thread                            │
│                                                            │
│  - UI interaction (button tap)                            │
│  - Launch file picker                                     │
│  - Receive selected URI                                   │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│              ViewModel (Default Scope)                    │
│                                                            │
│  - Opens streams                                          │
│  - Extracts ZIP entries                                   │
│  - Writes settings file                                   │
│  - Calls database operations                              │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│             IO Dispatcher (Blocking)                      │
│                                                            │
│  runBlocking(Dispatchers.IO) {                            │
│      database.checkpoint()                                │
│  }                                                        │
│                                                            │
│  - Ensures database is flushed before replacement         │
│  - Blocks until checkpoint completes                      │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│          File System Operations (Blocking)                │
│                                                            │
│  - database.close()                                       │
│  - FileOutputStream write                                 │
│  - stopService()                                          │
│  - delete()                                               │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│                  Process Exit                             │
│                                                            │
│  exitProcess(0)                                           │
└──────────────────────────────────────────────────────────┘
```

## State Transitions

```
┌─────────────┐
│   Initial   │  User opens Backup & Restore screen
│    State    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Waiting    │  Buttons are enabled and ready
│  for User   │
└──────┬──────┘
       │
       │ User taps "Restore"
       ▼
┌─────────────┐
│   Picker    │  File picker is displayed
│   Shown     │
└──────┬──────┘
       │
       │ User selects file (or cancels)
       │
       ├─────────────┐
       │             │ Cancel
       ▼             ▼
┌─────────────┐  ┌─────────────┐
│ Processing  │  │  Waiting    │
│  Restore    │  │  for User   │
└──────┬──────┘  └─────────────┘
       │
       │ Processing backup
       │
       ├─────────────────┐
       │                 │
       ▼                 ▼
┌─────────────┐   ┌─────────────┐
│   Success   │   │    Error    │
└──────┬──────┘   └──────┬──────┘
       │                 │
       │ App restarts    │ Show toast
       │                 │
       ▼                 ▼
┌─────────────┐   ┌─────────────┐
│   Fresh     │   │  Waiting    │
│   Start     │   │  for User   │
└─────────────┘   └─────────────┘
       │
       │ Database and settings loaded
       │
       ▼
┌─────────────┐
│  Restored   │  User sees all restored data
│   State     │
└─────────────┘
```

## Key Observations

### Critical Operations
1. **Database Checkpoint**: Ensures data consistency before closing
2. **Database Close**: Required before file replacement to avoid locks
3. **Process Exit**: Clean restart ensures fresh initialization

### Error Safety
- `tryOrNull` wrapper prevents ZIP corruption crashes
- Database not modified if restoration fails
- User can retry after errors

### Performance
- File operations are I/O bound
- Checkpoint operation may take time for large databases
- ZIP compression reduces backup file size
- No progress indication during restore (instantaneous for small backups)

### Data Integrity
- Atomic file replacement (all-or-nothing)
- Database checkpoint ensures consistency
- Foreign key constraints maintained through migrations
- Settings restored separately from database
