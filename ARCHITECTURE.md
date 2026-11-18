# SONIQ — Architecture (As Implemented)

**Last Updated**: 2026-11-18 (Commit `41c8fbc`, Stages 1–3 Complete)  
**Source of Truth**: Actual repository files under `D:\Projects\Soniq`  
*(This document describes only what is currently implemented in the repository; planned stages are tracked in `ROADMAP.md`.)*

---

## 1. Kotlin, Gradle & Android Configuration

- **Module layout**: Single Gradle application module `:app` (`settings.gradle.kts`) with strict package boundaries under `com.soniq.player`.
- **Toolchain**: Gradle Wrapper `9.9.1`, AGP `9.5.1` (built-in Kotlin; `org.jetbrains.kotlin.android` is not applied), Kotlin `2.4.21` (`kotlin-compose`, `kotlin-serialization`), KSP `2.3.14`, Hilt `2.60.1`, Room Gradle Plugin `2.8.5`.
- **JVM & SDK**: Both `compileOptions` and `kotlin.compilerOptions.jvmTarget` are pinned to **JVM 17** (verified under installed JDK `21.0.13` LTS). `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`.
- **Configurable Application ID**: `soniq.applicationId` Gradle property (default `com.soniq.player`, `.debug` suffix for debug builds); Kotlin namespace stays `com.soniq.player`.
- **Version catalog**: Centralized in `gradle/libs.versions.toml` (includes `coil-video:3.6.3` for offline video frame decoding and `media3-ui-compose:1.11.1` for native Compose `PlayerSurface`).
- **Release optimization**: `isMinifyEnabled = true`, `isShrinkResources = true`, ProGuard rules in `app/proguard-rules.pro`.
- **Robolectric Windows workaround**: Unit tests point `maven.repo.local` to `.gradle/robolectric-m2` in `app/build.gradle.kts` to avoid Robolectric 4.18 native runtime failures on Windows user directories containing spaces.

---

## 2. Actual Directory Structure (`app/src/main/kotlin/com/soniq/player`)

```text
com.soniq.player
├── SoniqApplication.kt                     # @HiltAndroidApp entry point
├── MainActivity.kt                         # Single-Activity Compose host, SplashScreen, edge-to-edge, PlaybackViewModel hookup
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
│   │       └── MediaItems.kt               # AudioTrack, VideoItem, AlbumSummary, ArtistSummary, MediaFolderSummary, sort orders, LibrarySyncState
│   ├── designsystem/
│   │   ├── token/SoniqTokens.kt            # SoniqNeutrals, SoniqAccents, Spacing, Radii, Sizes, Border, Elevation, Motion, Alpha, SoniqTokens
│   │   ├── theme/                          # ColorMath, SoniqColorSchemes, SoniqTypography, SoniqTheme
│   │   └── component/                      # SoniqBackground, SoniqScaffold, SoniqTopAppBar, cards, empty states, segmented selector
│   ├── theme/                              # AppearanceRepository (domain), DefaultAppearanceRepository (data), ThemePresetMapper, ThemeModule
│   ├── database/
│   │   ├── SoniqDatabase.kt                # Persistent user Room DB v1 (soniq.db: theme_presets; backed up; no destructive fallback)
│   │   ├── entity/ThemePresetEntity.kt
│   │   ├── dao/ThemePresetDao.kt
│   │   ├── media/
│   │   │   ├── SoniqMediaDatabase.kt       # Rebuildable media index Room DB v1 (soniq_media.db; excluded from backup)
│   │   │   ├── entity/AudioTrackEntity.kt  # Indexed local audio cache row
│   │   │   ├── entity/VideoItemEntity.kt   # Indexed local video cache row
│   │   │   ├── dao/AudioTrackDao.kt        # Queries + atomic chunked syncWith()
│   │   │   └── dao/VideoItemDao.kt         # Queries + atomic chunked syncWith()
│   │   └── di/DatabaseModule.kt            # Provides SoniqDatabase, SoniqMediaDatabase, and all DAOs
│   ├── datastore/                          # @AppearanceDataStore, AppearancePreferencesDataSource, DataStoreModule
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
│   ├── playback/
│   │   ├── MediaItemFactory.kt             # AudioTrack / VideoItem -> rich Media3 MediaItem + MediaKind resolver
│   │   ├── PlaybackController.kt           # Domain PlaybackStatus, PlaybackRepeatMode, PlaybackState, PlaybackController contract
│   │   ├── SoniqPlaybackService.kt         # MediaSessionService hosting ExoPlayer + dynamic MUSIC/MOVIE AudioAttributes + MediaSession
│   │   ├── Media3PlaybackController.kt     # Application-scoped MediaController with cold-start command queue, active-only position ticker, disconnect/reconnect
│   │   └── di/PlaybackModule.kt            # ServiceScoped ExoPlayer + Singleton PlaybackController
│   └── permissions/                        # API 26-37 MediaPermissions (incl. Android 14+ partial access), AndroidPermissionChecker, MediaPermissionController, DI
└── feature/
    ├── music/                              # MusicViewModel, MusicRoute, MusicScreen (Songs/Albums/Artists/Folders, sort, search, drill-down)
    ├── video/                              # VideoViewModel, VideoRoute, VideoScreen (Videos/Folders, thumbnails, partial-access banner, sort, search)
    ├── player/                             # PlaybackViewModel, MiniPlayerBar, NowPlayingScreen (3 PlayerLayouts), VideoPlayerScreen (PlayerSurface)
    └── settings/                           # SettingsViewModel, SettingsRoute, SettingsScreen (live theme customization & preset CRUD)
```

---

## 3. Data Layer & Dual-Database Separation

- **Preferences DataStore (`appearance.preferences_pb`)**: Stores active `AppearanceConfig` with `ReplaceFileCorruptionHandler` and eager loading in `MainViewModel`. Included in Android backup rules.
- **Persistent User Room DB (`soniq.db`, `SoniqDatabase` v1)**: Stores user-authored `ThemePresetEntity` rows (and future Stage 5 playlists/favorites). Exported schema committed at `app/schemas/com.soniq.player.core.database.SoniqDatabase/1.json`. No destructive migrations permitted. Included in Android backup rules.
- **Rebuildable Media Index Room DB (`soniq_media.db`, `SoniqMediaDatabase` v1)**: Stores indexed `AudioTrackEntity` and `VideoItemEntity` rows scanned from local MediaStore. Exported schema committed at `app/schemas/com.soniq.player.core.database.media.SoniqMediaDatabase/1.json`. **Excluded from backup rules** and allowed to rebuild cleanly on schema changes.

---

## 4. Media Discovery & Unidirectional UI Flow

1. `MusicViewModel` and `VideoViewModel` independently verify runtime permissions (`READ_MEDIA_AUDIO` vs `READ_MEDIA_VIDEO` / `READ_MEDIA_VISUAL_USER_SELECTED` / `READ_EXTERNAL_STORAGE`).
2. Once access is granted, each ViewModel triggers an initial `repository.sync()` and subscribes to debounced (750ms) `MediaStoreChangeObserver` updates.
3. Scanners query `ContentResolver` on `@IoDispatcher`; repositories serialize scans with a `Mutex` and commit diffs atomically via `Dao.syncWith(...)`.
4. Room `Flow` emissions flow through repositories into ViewModel `combine(...)` pipelines (applying active filter, search query, and sort order) and render in stateless Compose `*Screen` views via `collectAsStateWithLifecycle()`.

---

## 5. Playback Lifecycle Architecture

- `SoniqPlaybackService` (`MediaSessionService`, `foregroundServiceType="mediaPlayback"`) is the sole owner of `ExoPlayer` and `MediaSession`.
- `SoniqPlaybackService` listens to `onMediaItemTransition` and dynamically switches `ExoPlayer` `AudioAttributes` between `C.AUDIO_CONTENT_TYPE_MUSIC` and `C.AUDIO_CONTENT_TYPE_MOVIE`.
- `Media3PlaybackController` binds via `@ApplicationContext`, queues any cold-start playback command until async connection completes, polls position every 250ms **only while `isPlaying == true`**, exposes `player: StateFlow<Player?>` strictly for attaching Media3's Compose `PlayerSurface` inside `VideoPlayerScreen`, and handles `onDisconnected` + `disconnect()` via `MediaController.releaseFuture`.
- Exiting `VideoPlayerScreen` pauses active video playback cleanly via `PlaybackViewModel.pauseIfVideo()`, while active audio playback continues uninterrupted in the background and surfaces `MiniPlayerBar` on top-level screens.

---

## 6. Offline-First & Privacy Architecture

- **Merged Release Manifest Permissions**: `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`.
- **Network Stripping**: `INTERNET` and `ACCESS_NETWORK_STATE` are stripped via `tools:node="remove"` and verified absent in the merged release manifest.
- **Images & Thumbnails**: Coil 3 (`coil-compose` + `coil-video`) loads local album art URIs, SAF background images, and local video frames strictly offline with zero network fetchers on the classpath.
