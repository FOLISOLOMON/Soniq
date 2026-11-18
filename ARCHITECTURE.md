# SONIQ — Architecture (As Implemented)

**Last Updated**: 2026-11-18 (Commit `c5fabc0`, Stages 1–5 Complete)  
**Source of Truth**: Actual repository files under `D:\Projects\Soniq`  
*(This document describes only what is currently implemented in the repository; planned stages are tracked in `ROADMAP.md`.)*

---

## 1. Kotlin, Gradle & Android Configuration

- **Module layout**: Single Gradle application module `:app` (`settings.gradle.kts`) with strict package boundaries under `com.soniq.player`.
- **Toolchain**: Gradle Wrapper `9.9.1`, AGP `9.5.1` (built-in Kotlin; `org.jetbrains.kotlin.android` is not applied), Kotlin `2.4.21` (`kotlin-compose`, `kotlin-serialization`), KSP `2.3.14`, Hilt `2.60.1`, Room Gradle Plugin `2.8.5`.
- **JVM & SDK**: Both `compileOptions` and `kotlin.compilerOptions.jvmTarget` are pinned to **JVM 17** (verified under installed JDK `21.0.13` LTS). `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`.
- **Configurable Application ID**: `soniq.applicationId` Gradle property (default `com.soniq.player`, `.debug` suffix for debug builds); Kotlin namespace stays `com.soniq.player`.
- **Version catalog**: Centralized in `gradle/libs.versions.toml` (includes `coil-video:3.6.3`, `media3-ui-compose:1.11.1`, and `androidx.room:room-testing:2.8.5` for instrumented migration tests).
- **Release optimization**: `isMinifyEnabled = true`, `isShrinkResources = true`, ProGuard rules in `app/proguard-rules.pro`.
- **Robolectric Windows workaround**: Unit tests point `maven.repo.local` to `.gradle/robolectric-m2` in `app/build.gradle.kts` to avoid Robolectric 4.18 native runtime failures on Windows user directories containing spaces.

---

## 2. Actual Directory Structure (`app/src/main/kotlin/com/soniq/player`)

```text
com.soniq.player
├── SoniqApplication.kt                     # @HiltAndroidApp entry point
├── MainActivity.kt                         # Single-Activity Compose host, SplashScreen, edge-to-edge, PlaybackViewModel & POST_NOTIFICATIONS launcher
├── MainViewModel.kt                        # Eagerly loads AppearanceConfig for splash gate
├── SoniqApp.kt                             # SoniqBackground + adaptive NavigationSuiteScaffold + docked MiniPlayerBar + full-screen player chrome hiding
├── navigation/
│   ├── SoniqDestination.kt                 # @Serializable SoniqRoute (Music, Video, Settings, AudioPlayer, VideoPlayer) & TopLevelDestination
│   └── SoniqNavHost.kt                     # Root NavHost, state-preserving tab navigation, full-screen route helpers
├── core/
│   ├── common/
│   │   ├── Results.kt                      # Cancellation-safe suspendRunCatching & enumValueOrDefault
│   │   ├── MediaFormatters.kt              # Pure-Kotlin duration, file size, and resolution formatters
│   │   └── di/DispatchersModule.kt         # Injectable @IoDispatcher
│   ├── model/
│   │   ├── appearance/Appearance.kt        # ThemeMode, FontFamilyOption, TextScale, BackgroundStyle, PlayerLayout, AppearanceConfig, ThemePreset
│   │   └── media/
│   │       ├── MediaKind.kt                # AUDIO, VIDEO
│   │       ├── MediaItems.kt               # AudioTrack, VideoItem, AlbumSummary, ArtistSummary, MediaFolderSummary, sort orders, LibrarySyncState
│   │       └── UserMedia.kt                # mediaKey(), PlaylistSummary, PlaybackHistoryEntry, MediaResumePosition
│   ├── designsystem/
│   │   ├── token/SoniqTokens.kt            # SoniqNeutrals, SoniqAccents, Spacing, Radii, Sizes, Border, Elevation, Motion, Alpha, SoniqTokens
│   │   ├── theme/                          # ColorMath, SoniqColorSchemes, SoniqTypography, SoniqTheme
│   │   └── component/                      # SoniqBackground, SoniqScaffold, SoniqTopAppBar, cards, empty states, segmented selector
│   ├── theme/                              # AppearanceRepository (domain), DefaultAppearanceRepository (data), ThemePresetMapper, ThemeModule
│   ├── database/
│   │   ├── SoniqDatabase.kt                # Persistent user Room DB v2 (soniq.db; backed up; explicit non-destructive migrations only)
│   │   ├── SoniqDatabaseMigrations.kt      # Tested non-destructive MIGRATION_1_2 preserving theme_presets
│   │   ├── entity/
│   │   │   ├── ThemePresetEntity.kt
│   │   │   └── UserMediaEntities.kt        # PlaylistEntity, PlaylistTrackEntity (FK cascade), FavoriteMediaEntity, PlaybackHistoryEntity, PlaybackResumeEntity
│   │   ├── dao/
│   │   │   ├── ThemePresetDao.kt
│   │   │   └── UserMediaDaos.kt            # PlaylistDao & UserMediaDao
│   │   ├── media/
│   │   │   ├── SoniqMediaDatabase.kt       # Rebuildable media index Room DB v1 (soniq_media.db; excluded from backup)
│   │   │   ├── entity/AudioTrackEntity.kt  # Indexed local audio cache row
│   │   │   ├── entity/VideoItemEntity.kt   # Indexed local video cache row
│   │   │   ├── dao/AudioTrackDao.kt        # Queries, getByIds(), and atomic chunked syncWith()
│   │   │   └── dao/VideoItemDao.kt         # Queries + atomic chunked syncWith()
│   │   └── di/DatabaseModule.kt            # Provides SoniqDatabase (with MIGRATION_1_2), SoniqMediaDatabase, and all DAOs
│   ├── datastore/
│   │   ├── AppearanceDataStore.kt          # @AppearanceDataStore (appearance.preferences_pb; backed up)
│   │   ├── AppearancePreferencesDataSource.kt
│   │   ├── PlaybackDataStore.kt            # @PlaybackDataStore (playback_state.preferences_pb; excluded from backup)
│   │   ├── PlaybackPreferencesDataSource.kt # PlaybackPreferences & PlaybackSessionSnapshot persistence
│   │   └── di/DataStoreModule.kt
│   ├── mediadiscovery/
│   │   ├── MediaStoreCollections.kt        # API-aware Audio & Video MediaStore collection URIs
│   │   ├── MediaStorePathNormalizer.kt     # Pure-Kotlin RELATIVE_PATH / legacy DATA / tag normalizer
│   │   ├── MediaStoreChangeObserver.kt     # ContentObserver wrapped in callbackFlow<Unit>
│   │   ├── MediaStoreAudioScanner.kt       # LocalAudioScanner querying MediaStore on @IoDispatcher
│   │   ├── MediaStoreVideoScanner.kt       # LocalVideoScanner querying MediaStore on @IoDispatcher
│   │   ├── domain/MediaLibraryRepositories.kt # Separate MusicLibraryRepository & VideoLibraryRepository contracts
│   │   ├── data/MediaLibraryMappers.kt     # Room Entity <-> Domain mappers
│   │   ├── data/DefaultMediaLibraryRepositories.kt # Mutex-guarded sync + reactive Album/Artist/Folder aggregations
│   │   └── di/MediaDiscoveryModule.kt      # Hilt bindings for scanners, observer, repositories, ContentResolver
│   ├── userlibrary/
│   │   ├── domain/UserLibraryRepository.kt # Playlists CRUD, Favorites, Playback History, Resume Positions
│   │   ├── data/DefaultUserLibraryRepository.kt # Validation, atomic reorder, history pruning, <5s/>=95% resume auto-clear
│   │   └── di/UserLibraryModule.kt
│   ├── playback/
│   │   ├── MediaItemFactory.kt             # AudioTrack / VideoItem -> rich Media3 MediaItem + MediaKind resolver
│   │   ├── SoniqOfflineBitmapLoader.kt     # Strictly offline Media3 BitmapLoader (rejects http/https, max 512px downsample)
│   │   ├── SoniqSessionCommands.kt         # COMMAND_TOGGLE_SHUFFLE, COMMAND_CYCLE_REPEAT, SLOT_OVERFLOW button prefs
│   │   ├── PlaybackRestorationManager.kt   # Saves & restores audio queue snapshots via PlaybackPreferencesDataSource + AudioTrackDao
│   │   ├── PlaybackController.kt           # Domain PlaybackStatus, PlaybackRepeatMode, QueueItem, PlaybackState, PlaybackController contract
│   │   ├── SoniqPlaybackService.kt         # MediaSessionService: ExoPlayer, dynamic AudioAttributes, DefaultMediaNotificationProvider, MediaSession.Callback, onTaskRemoved
│   │   ├── Media3PlaybackController.kt     # Application-scoped MediaController: cold-start queue + paused restoration, 250ms active ticker, Up Next queue mutations
│   │   └── di/PlaybackModule.kt            # ServiceScoped ExoPlayer + Singleton PlaybackController
│   └── permissions/                        # API 26-37 MediaPermissions (incl. Android 14+ partial access), NotificationPermissions (API 33+), AndroidPermissionChecker, DI
└── feature/
    ├── music/                              # MusicViewModel, MusicRoute, MusicScreen (Songs/Albums/Artists/Folders/Playlists/Favorites, dialogs, track overflow actions)
    ├── video/                              # VideoViewModel, VideoRoute, VideoScreen (Videos/Folders, thumbnails, saved resume progress overlay, partial-access banner)
    ├── player/                             # PlaybackViewModel, MiniPlayerBar, NowPlayingScreen (3 PlayerLayouts + Favorite + Up Next sheet), VideoPlayerScreen (PlayerSurface)
    └── settings/                           # SettingsViewModel, SettingsRoute, SettingsScreen (theme customization, preset CRUD, Playback & Notification controls)
```

---

## 3. Data Layer & Separation of Concerns

- **Appearance Preferences DataStore (`appearance.preferences_pb`)**: Stores active `AppearanceConfig` with `ReplaceFileCorruptionHandler` and eager loading in `MainViewModel`. Included in Android backup rules.
- **Playback State DataStore (`playback_state.preferences_pb`)**: Stores `PlaybackPreferences` (`continueVideoInBackground`, `notificationPromptShown`) and `PlaybackSessionSnapshot` (active audio queue IDs, index, position, repeat, shuffle, speed). **Excluded from backup rules** because MediaStore track IDs are device-specific.
- **Persistent User Room DB (`soniq.db`, `SoniqDatabase` v2)**: Stores user-authored `theme_presets`, `playlists`, `playlist_tracks` (FK `ON DELETE CASCADE`), `favorite_media`, `playback_history`, and `playback_resume`. Exported schemas committed at `app/schemas/com.soniq.player.core.database.SoniqDatabase/1.json` and `2.json`. **No destructive migrations permitted**; `MIGRATION_1_2` is verified by both unit and instrumented `MigrationTestHelper` tests. Included in Android backup rules.
- **Rebuildable Media Index Room DB (`soniq_media.db`, `SoniqMediaDatabase` v1)**: Stores indexed `AudioTrackEntity` and `VideoItemEntity` rows scanned from local MediaStore. Exported schema committed at `app/schemas/com.soniq.player.core.database.media.SoniqMediaDatabase/1.json`. **Excluded from backup rules** and allowed to rebuild cleanly on schema changes.

---

## 4. Media Discovery, User Library & Unidirectional UI Flow

1. `MusicViewModel` and `VideoViewModel` independently verify runtime permissions (`READ_MEDIA_AUDIO` vs `READ_MEDIA_VIDEO` / `READ_MEDIA_VISUAL_USER_SELECTED` / `READ_EXTERNAL_STORAGE`).
2. Once access is granted, each ViewModel triggers an initial `repository.sync()` and subscribes to debounced (750ms) `MediaStoreChangeObserver` updates.
3. Scanners query `ContentResolver` on `@IoDispatcher`; repositories serialize scans with a `Mutex` and commit diffs atomically via `Dao.syncWith(...)`.
4. `UserLibraryRepository` combines reactive Room flows for playlists, favorites, history, and resume positions with MediaStore tracks/videos in ViewModel `combine(...)` pipelines so UI screens remain stateless and lifecycle-aware (`collectAsStateWithLifecycle()`).

---

## 5. Playback Service, MediaSession & Restoration Architecture

- `SoniqPlaybackService` (`MediaSessionService`, `foregroundServiceType="mediaPlayback"`) is the sole owner of `ExoPlayer` and `MediaSession` and holds zero Activity or Compose references.
- **Audio Focus, Noisy Audio & Wake Lock**: Configured via `AudioAttributes` (`USAGE_MEDIA`, dynamically switched between `AUDIO_CONTENT_TYPE_MUSIC` and `AUDIO_CONTENT_TYPE_MOVIE` on `onMediaItemTransition`), `handleAudioFocus = true`, `handleAudioBecomingNoisy = true`, and `C.WAKE_MODE_LOCAL`.
- **Notifications, Artwork & Custom Actions**: Configured with `DefaultMediaNotificationProvider` (`soniq_playback_channel`, `ic_music_note`), `SoniqOfflineBitmapLoader` (offline-only, `512px` cap), and custom `MediaSession.Callback` (`AcceptedResultBuilder(session, controller)`) advertising `COMMAND_TOGGLE_SHUFFLE` and `COMMAND_CYCLE_REPEAT` in `SLOT_OVERFLOW` while preserving compact Prev/Play-Pause/Next controls.
- **Media Buttons & Background Lifecycle**: Exported `androidx.media3.session.MediaButtonReceiver` (`MEDIA_BUTTON`) routes headset/Bluetooth/lock-screen media buttons to `SoniqPlaybackService`. `onTaskRemoved` keeps active background audio playing while stopping idle/paused sessions cleanly; `onDestroy` saves the latest snapshot and releases `MediaSession` and `ExoPlayer`.
- **Playback Restoration**: `PlaybackRestorationManager` snapshots audio queue IDs, index, position, repeat, shuffle, and speed into `@PlaybackDataStore` and resolves surviving tracks via `AudioTrackDao.getByIds(...)` for both `MediaSession.Callback.onPlaybackResumption` (Android 13+ system media resumption) and cold-start paused restoration in `Media3PlaybackController`.
- **Video Exit, History & Resume**: `PlaybackViewModel` logs playback history on media transitions, persists resume positions on pause/exit (auto-clearing `<5s` or `>=95%`), resumes videos automatically from saved positions, and respects the user's `continueVideoInBackground` preference when exiting `VideoPlayerScreen`.

---

## 6. Offline-First & Privacy Architecture

- **Merged Release Manifest Permissions**: `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `WAKE_LOCK`.
- **Network Stripping**: `INTERNET` and `ACCESS_NETWORK_STATE` are stripped via `tools:node="remove"` and verified absent in the merged release manifest.
- **Images & Artwork**: Both Coil 3 (`coil-compose` + `coil-video`) and `SoniqOfflineBitmapLoader` operate strictly offline; `SoniqOfflineBitmapLoader` explicitly throws `SecurityException` on `http`/`https` URIs.
