# SONIQ — Architecture (As Implemented)

**Last Updated**: 2026-11-18 (Commit `46be15f`)  
**Source of Truth**: Actual repository files under `D:\Projects\Soniq`  
*(This document describes only what is currently implemented in the repository; planned modules are tracked in `ROADMAP.md`.)*

---

## 1. Kotlin, Gradle & Android Configuration

- **Module layout**: Single Gradle application module `:app` (`settings.gradle.kts`) with strict package boundaries under `com.soniq.player`.
- **Toolchain**: Gradle Wrapper `9.9.1`, AGP `9.5.1` (built-in Kotlin enabled; `org.jetbrains.kotlin.android` is not applied), Kotlin `2.4.21` (`kotlin-compose` + `kotlin-serialization`), KSP `2.3.14`, Hilt `2.60.1`, Room Gradle Plugin `2.8.5`.
- **JVM & SDK**: `compileOptions` and `kotlin.compilerOptions.jvmTarget` both pinned to **JVM 17** (compatible with installed JDK `21.0.13` LTS). `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`.
- **Configurable Application ID**: `soniq.applicationId` Gradle property (default `com.soniq.player`, `.debug` suffix for debug builds); Kotlin namespace fixed at `com.soniq.player`.
- **Version catalog**: All dependency and plugin versions centralized in `gradle/libs.versions.toml`.
- **Release optimization**: `isMinifyEnabled = true`, `isShrinkResources = true`, ProGuard rules in `app/proguard-rules.pro`.
- **Robolectric Windows workaround**: `app/build.gradle.kts` sets `maven.repo.local` for unit tests to `.gradle/robolectric-m2` to avoid Robolectric 4.18 native runtime failures on Windows user profiles containing spaces.

---

## 2. Actual Directory Structure (`app/src/main/kotlin/com/soniq/player`)

```text
com.soniq.player
├── SoniqApplication.kt                     # @HiltAndroidApp entry point
├── MainActivity.kt                         # Single-Activity Compose host, SplashScreen, edge-to-edge
├── MainViewModel.kt                        # Eagerly loads AppearanceConfig for splash gate
├── SoniqApp.kt                             # SoniqBackground + adaptive NavigationSuiteScaffold
├── navigation/
│   ├── SoniqDestination.kt                 # @Serializable SoniqRoute & TopLevelDestination enum
│   └── SoniqNavHost.kt                     # Root NavHost & tab state-preserving navigation helpers
├── core/
│   ├── common/
│   │   └── Results.kt                      # suspendRunCatching (cancellation-safe) & enumValueOrDefault
│   ├── model/
│   │   ├── appearance/Appearance.kt        # ThemeMode, FontFamilyOption, TextScale, BackgroundStyle, PlayerLayout, AppearanceConfig, ThemePreset
│   │   └── media/MediaKind.kt              # AUDIO, VIDEO
│   ├── designsystem/
│   │   ├── token/SoniqTokens.kt            # SoniqNeutrals, SoniqAccents, Spacing, Radii, Sizes, Border, Elevation, Motion, Alpha, SoniqTokens
│   │   ├── theme/
│   │   │   ├── ColorMath.kt                # Pure-JVM HSL & WCAG contrast math (>= 4.58:1 guaranteed)
│   │   │   ├── SoniqColorSchemes.kt        # Light, Dark, and AMOLED (#000000) ColorScheme builders
│   │   │   ├── SoniqTypography.kt          # FontFamily mapping & multiplicative TextScale typography
│   │   │   └── SoniqTheme.kt               # SoniqTheme composable + LocalSoniqTokens / LocalAppearance
│   │   └── component/
│   │       ├── SoniqBackground.kt          # Solid, Gradient, or Coil local Image background with scrim
│   │       └── SoniqComponents.kt          # SoniqScaffold, SoniqTopAppBar, SoniqSectionHeader, SoniqSurfaceCard, SoniqEmptyState, SoniqSegmentedChoice
│   ├── theme/
│   │   ├── domain/AppearanceRepository.kt  # Domain repository contract
│   │   ├── data/ThemePresetMapper.kt       # Entity <-> Domain mappers
│   │   ├── data/DefaultAppearanceRepository.kt # Combines DataStore (active) + Room (presets)
│   │   └── di/ThemeModule.kt               # Binds AppearanceRepository & provides UTC Clock
│   ├── database/
│   │   ├── SoniqDatabase.kt                # Room v1 (ThemePresetEntity), exportSchema = true
│   │   ├── entity/ThemePresetEntity.kt     # theme_presets table
│   │   ├── dao/ThemePresetDao.kt           # observeAll, getById, insert, deleteById
│   │   └── di/DatabaseModule.kt            # Singleton Room DB (no destructive fallback) + DAO
│   ├── datastore/
│   │   ├── AppearanceDataStore.kt          # @AppearanceDataStore Hilt qualifier
│   │   ├── AppearancePreferencesDataSource.kt # Typed Preferences DataStore access + IO/enum resilience
│   │   └── di/DataStoreModule.kt           # ReplaceFileCorruptionHandler + appearance.preferences_pb
│   ├── mediadiscovery/
│   │   ├── MediaStoreCollections.kt        # API-aware Audio & Video MediaStore collection URIs
│   │   ├── MediaStoreChangeObserver.kt     # ContentObserver wrapped in callbackFlow<Unit>
│   │   └── di/MediaDiscoveryModule.kt      # Binds MediaStoreChangeObserver & provides ContentResolver
│   ├── playback/
│   │   ├── PlaybackController.kt           # Domain PlaybackStatus, PlaybackState, PlaybackController contract
│   │   ├── SoniqPlaybackService.kt         # @AndroidEntryPoint MediaSessionService hosting ExoPlayer + MediaSession
│   │   ├── Media3PlaybackController.kt     # Application-scoped MediaController with disconnect/reconnect recovery
│   │   └── di/PlaybackModule.kt            # ServiceScoped ExoPlayer + Singleton PlaybackController binding
│   └── permissions/
│       ├── MediaPermissions.kt             # API 26-37 permission resolution incl. Android 14+ partial visual access
│       ├── AndroidPermissionChecker.kt     # ContextCompat implementation of PermissionChecker
│       ├── MediaPermissionController.kt    # Compose RequestMultiplePermissions launcher + app settings intent
│       └── di/PermissionsModule.kt         # Binds PermissionChecker
└── feature/
    ├── music/                              # MusicViewModel, MusicRoute, MusicScreen (permission-aware shell)
    ├── video/                              # VideoViewModel, VideoRoute, VideoScreen (permission-aware shell incl. PARTIAL state)
    └── settings/                           # SettingsViewModel, SettingsRoute, SettingsScreen (live theme & preset CRUD)
```

---

## 3. Core Architectural Layers & Data Flow

- **Presentation Layer (`feature/*`, `navigation`, `MainActivity`, `SoniqApp`)**:
  - Unidirectional Data Flow: ViewModels expose immutable `StateFlow<*UiState>` collected via `collectAsStateWithLifecycle()`.
  - Every feature separates a Hilt-aware `*Route` composable from a stateless `*Screen` composable for isolated UI testing.
  - `MainViewModel` uses `SharingStarted.Eagerly` so `SplashScreen.setKeepOnScreenCondition` exits cleanly without cold-start theme flicker.
- **Domain Layer (`core/model`, `core/theme/domain`, `core/playback/PlaybackController`, `core/permissions`, `core/mediadiscovery`)**:
  - Pure domain models and interfaces free of UI/framework coupling (`AppearanceConfig`, `ThemePreset`, `MediaKind`, `PlaybackState`, `MediaAccessGrant`).
  - Use-case classes have not yet been introduced (current logic is thin repository delegation; use cases will be added when multi-repository coordination appears in Stages 2–5).
- **Data Layer (`core/database`, `core/datastore`, `core/theme/data`)**:
  - Active appearance lives in Preferences DataStore (`appearance.preferences_pb`, corruption-safe).
  - Saved presets live in Room (`soniq.db`, version 1, exported schema at `app/schemas/com.soniq.player.core.database.SoniqDatabase/1.json`).
  - `DefaultAppearanceRepository` coordinates both stores using an injected `java.time.Clock`.

---

## 4. Playback Lifecycle Architecture

- `SoniqPlaybackService` (`MediaSessionService`, `foregroundServiceType="mediaPlayback"`, exported for system controllers) is the sole owner of `ExoPlayer` and `MediaSession`.
- `ExoPlayer` is configured with `USAGE_MEDIA` / `AUDIO_CONTENT_TYPE_MUSIC`, audio focus handling, `handleAudioBecomingNoisy = true`, and `C.WAKE_MODE_LOCAL` (offline CPU wake lock only).
- `SoniqPlaybackService.onTaskRemoved` stops the service if playback is idle/ended; `onDestroy` releases both `ExoPlayer` and `MediaSession`.
- `Media3PlaybackController` connects using `@ApplicationContext` (never an Activity/Compose context), handles `MediaController.Listener.onDisconnected`, and exposes `disconnect()` using `MediaController.releaseFuture` to prevent stale references or leaks.

---

## 5. Permission, Offline-First & Privacy Architecture

- **Permissions declared in `AndroidManifest.xml`**:
  - `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+ partial access), `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`.
- **Network removal**: `INTERNET` and `ACCESS_NETWORK_STATE` are explicitly stripped via `tools:node="remove"` and verified absent in the merged release manifest.
- **Images**: Coil 3 (`coil-compose`) is included without any network fetcher artifact, so image loading is strictly local (`content://`, `file://`, resources).
- **Backup rules**: `data_extraction_rules.xml` and `backup_rules.xml` include `appearance.preferences_pb` and `soniq.db` (user-authored settings & presets only). Future Stage 2 media index tables will live in a separate non-backed-up database.

*(Historical ADR notes are also preserved in `docs/ARCHITECTURE.md`.)*
