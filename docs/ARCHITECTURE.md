# SONIQ Architecture

## 1. Guiding Principles

- **Offline-first & privacy-first**: SONIQ never declares `INTERNET` or `ACCESS_NETWORK_STATE` (and explicitly strips both with `tools:node="remove"` in `AndroidManifest.xml`). Coil is configured without any network fetcher. No analytics, telemetry, or advertising SDKs are included.
- **Lifecycle-independent playback**: Media playback is owned by `SoniqPlaybackService` (`MediaSessionService`) and exposed to the UI through an application-scoped `PlaybackController`. Neither `MainActivity` nor any Compose screen owns an `ExoPlayer` instance.
- **Separate Music & Video experiences, shared engine**: `feature.music` and `feature.video` have independent navigation graphs, permissions (`READ_MEDIA_AUDIO` vs `READ_MEDIA_VIDEO` on API 33+), and ViewModels, while sharing `core.playback`, `core.mediadiscovery`, and `core.designsystem`.
- **Single-module discipline (for now)**: Avoid unnecessary Gradle module proliferation at Stage 1.1. Strict package boundaries (`core.*`, `feature.*`, `navigation`) mirror future module splits if build times ever justify extracting Gradle subprojects.
- **Unidirectional UI state**: ViewModels expose immutable `StateFlow` UI states collected via `collectAsStateWithLifecycle`. Stateless `*Screen` composables are separated from `*Route` composables for straightforward UI testing.
- **Centralized design tokens**: All colors, spacing, radii, sizes, elevation, motion, and alpha values live in `core.designsystem.token`. No magic visual constants appear in feature code.

## 2. Package Structure (`com.soniq.player`)

```text
com.soniq.player
├── SoniqApplication.kt                 # Hilt application entry point
├── MainActivity.kt                     # Single-Activity Compose host + SplashScreen + edge-to-edge
├── MainViewModel.kt                    # Loads persisted appearance before first frame
├── SoniqApp.kt                         # Adaptive NavigationSuiteScaffold + SoniqBackground shell
├── navigation/                         # Type-safe @Serializable routes & SoniqNavHost
├── core/
│   ├── common/                         # Cancellation-safe Result helpers & defensive enum parsing
│   ├── model/                          # Pure domain models (AppearanceConfig, ThemePreset, MediaKind)
│   ├── designsystem/
│   │   ├── token/                      # SoniqTokens, SoniqNeutrals, SoniqAccents
│   │   ├── theme/                      # ColorMath, SoniqColorSchemes, SoniqTypography, SoniqTheme
│   │   └── component/                  # SoniqBackground, SoniqScaffold, SoniqTopAppBar, cards, states
│   ├── theme/                          # AppearanceRepository (domain), DefaultAppearanceRepository (data), Hilt module
│   ├── database/                       # SoniqDatabase (Room, schema export enabled), entities, DAOs, DI
│   ├── datastore/                      # AppearancePreferencesDataSource (Preferences DataStore), DI
│   ├── mediadiscovery/                 # MediaStore collection URI resolver & ContentObserver Flow boundary
│   ├── playback/                       # SoniqPlaybackService (Media3), PlaybackController, PlaybackState, DI
│   └── permissions/                    # API-aware MediaPermissions, PermissionChecker, Compose requester
└── feature/
    ├── music/                          # MusicRoute, MusicScreen, MusicViewModel
    ├── video/                          # VideoRoute, VideoScreen, VideoViewModel
    └── settings/                       # SettingsRoute, SettingsScreen, SettingsViewModel (live theme & presets)
```

## 3. Key Architectural Decisions (ADRs)

### ADR-001: Single `:app` Gradle module with strict package boundaries
- **Decision**: Keep a single `:app` module in Milestone 1 with strict `core/*` and `feature/*` packages instead of premature multi-module proliferation.
- **Rationale**: Faster Gradle configuration, simpler Hilt/Room/Compose setup, and clean refactorability into `:core:*` / `:feature:*` modules later if the codebase grows large enough to benefit from module-level build caching.

### ADR-002: AGP 9 Built-in Kotlin + Explicit JVM 17 Target + KSP
- **Decision**: Use AGP 9.5.1 with built-in Kotlin support (no external `kotlin-android` plugin), explicit `compileOptions` and `kotlin.compilerOptions.jvmTarget` pinned to JVM 17 (compatible with host JDK 21), and KSP (`2.3.14`) for Room and Hilt code generation.
- **Rationale**: Eliminates legacy `kapt` overhead, prevents implicit JVM bytecode drift across host JDK versions, and follows the current stable Android build toolchain.

### ADR-003: Dual persistence for Appearance (DataStore) and Presets (Room)
- **Decision**: Store the active `AppearanceConfig` in Preferences DataStore (`appearance.preferences_pb`) with a `ReplaceFileCorruptionHandler`, eagerly loaded by `MainViewModel` (`SharingStarted.Eagerly`) so `core-splashscreen` never depends on Compose subscription timing, and store saved `ThemePreset` rows in Room (`soniq.db`) with `exportSchema = true` and no destructive migrations.
- **Rationale**: Active settings are lightweight key-value state read before the first frame (preventing startup theme flicker), whereas presets are an unbounded ordered collection queried alongside future playlists. Stage 1.2's rebuildable MediaStore index cache will live in a separate non-backed-up database so cloud backup rules only cover user-authored data.

### ADR-004: Custom HSL Color Scheme Engine with WCAG Contrast Enforcement
- **Decision**: Generate Light, Dark, and AMOLED color schemes from any ARGB seed color in `SoniqColorSchemes` (backed by pure-Kotlin `ColorMath`), alongside optional API 31+ Material You dynamic colors. All visual constants (including border strokes and scrolled top-bar alpha) live exclusively in `SoniqTokens`.
- **Rationale**: Guarantees at least 4.58:1 WCAG contrast on accent surfaces across arbitrary user-selected accents, preserves pure `#000000` AMOLED surfaces, keeps styling strictly tokenized, and remains 100% JVM unit-testable without Android dependencies.

### ADR-005: Configurable Application ID
- **Decision**: Keep the Kotlin `namespace` fixed at `com.soniq.player` while allowing `applicationId` to be overridden at build time via `-Psoniq.applicationId=...` (with `.debug` suffix on debug builds).
- **Rationale**: Allows side-by-side debug/release installation and future release ID changes without touching Kotlin package names or generated R classes.

### ADR-006: Media3 `MediaSessionService` + Application-Scoped `PlaybackController`
- **Decision**: Host `ExoPlayer` and `MediaSession` exclusively inside `SoniqPlaybackService` (`WAKE_MODE_LOCAL`, audio focus, noisy audio handling) and connect from `Media3PlaybackController` using the application `Context`, with explicit `disconnect()` / `MediaController.releaseFuture` cleanup and `MediaController.Listener.onDisconnected` recovery.
- **Rationale**: Guarantees background audio continuity, system notification/lock-screen/Bluetooth integration, clean recovery if the OS recycles the service, and complete decoupling from Activity/Compose lifecycles.

### ADR-007: Android 14+ Partial Visual Media Access
- **Decision**: Declare `READ_MEDIA_VISUAL_USER_SELECTED` alongside `READ_MEDIA_VIDEO`, request both on API 34+ via `RequestMultiplePermissions`, and distinguish `MediaAccessGrant.FULL`, `PARTIAL`, and `NONE` cleanly from `READ_MEDIA_AUDIO`.
- **Rationale**: Respects user privacy choices on Android 14+ ("Select photos and videos") instead of treating partial access as denied, while letting users expand their selection anytime.
