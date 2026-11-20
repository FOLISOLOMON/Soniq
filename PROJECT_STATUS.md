# SONIQ — Project Status

**Last Updated**: 2026-11-20 (End of Session 6 — UI/UX Visual Redesign Phase 1)  
**Current Stage**: **Stages 1–9 Engineering Complete + UI/UX Visual Redesign Phase 1 Complete** (`Liquid Glass` Design System, Home, Shell, Now Playing, MiniPlayer, My Library, Video) → Ready for **UI/UX Visual Redesign Phase 2 & Physical-Device QA**  
**Latest Verified Code Commit**: `47dc1a3` (`main`)

---

## 1. Product Overview

**SONIQ** (`com.soniq.player`) is an Android-native, offline-first, privacy-first, advertisement-free music and video player built with Jetpack Compose, Material 3, AndroidX Media3, Room, DataStore, Coil, Navigation Compose, and Hilt.

---

## 2. Confirmed Technology Stack

| Layer | Technology & Verified Version |
|---|---|
| Language / Toolchain | Kotlin `2.4.21` (AGP 9 built-in Kotlin), explicit JVM target `17`, host JDK `21.0.13+9-LTS` |
| Build System | Gradle Wrapper `9.9.1`, AGP `9.5.1`, KSP `2.3.14`, centralized `gradle/libs.versions.toml` |
| Android SDK | `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37` |
| UI | Jetpack Compose (`compose-bom:2026.09.00`), Material 3, Adaptive Navigation Suite (`currentWindowAdaptiveInfoV2`), Core SplashScreen `1.2.0` |
| Navigation | Navigation Compose `2.10.2` (type-safe `@Serializable` routes), `hilt-lifecycle-viewmodel-compose:1.4.0` |
| Playback | AndroidX Media3 `1.11.1` (`media3-exoplayer`, `media3-session`, `media3-common`, `media3-ui-compose`) + `android.media.audiofx.Equalizer` / `BassBoost` |
| Persistence | Room `2.8.5` (`soniq.db` v3 user DB with tested `MIGRATION_1_2` & `MIGRATION_2_3` + `soniq_media.db` v1 rebuildable media cache, `exportSchema = true`), DataStore Preferences `1.3.0` (`appearance.preferences_pb` + `playback_state.preferences_pb`) |
| DI | Hilt `2.60.1` (KSP) |
| Images | Coil 3 `3.6.3` (`coil-compose` + `coil-video`, strictly offline/local fetchers) + `SoniqOfflineBitmapLoader` |
| Testing | JUnit `4.13.2`, Truth `1.4.5`, Turbine `1.2.1`, Coroutines Test `1.11.0`, Robolectric `4.18`, Room Testing `2.8.5`, AndroidX Test & Compose UI Test |

---

## 3. Implementation Status Matrix

### Implemented & Verified (Stages 1–9 + Redesign Phase 1)
- **Stage 1 — Foundation & Hardening (`2b8937f`, `46be15f`)**: `SoniqApplication`, `MainActivity`, adaptive `SoniqApp` shell, centralized `SoniqTokens`, Light/Dark/AMOLED/Dynamic Color, offline/privacy manifest stripping.
- **Stage 2 — Local Media Discovery & Library Management (`53cbae1`)**: `soniq_media.db`, `MediaStoreAudioScanner`, `MediaStoreVideoScanner`, `Mutex`-guarded repositories, debounced `MediaStoreChangeObserver`, `MusicScreen`, `VideoScreen`, Android 14+ partial-access banner.
- **Stage 3 — Audio & Video Playback Engine (`41c8fbc`)**: `MediaItemFactory`, dynamic `MUSIC`/`MOVIE` `AudioAttributes`, `Media3PlaybackController`, `MiniPlayerBar`, `NowPlayingScreen` (3 layouts), `VideoPlayerScreen` (`PlayerSurface`).
- **Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Media Buttons (`057fb56`, `c5fabc0`, `13983a8`)**: `DefaultMediaNotificationProvider`, `SoniqOfflineBitmapLoader`, `MediaButtonReceiver`, `POST_NOTIFICATIONS` flow, `NonCancellable` queue persistence & paused cold-start restoration.
- **Stage 5 — Playlists, Favorites, History, Resume Positions & Queue Management (`f265823`)**: `soniq.db` `MIGRATION_1_2`, `UserLibraryRepository`, playlists CRUD, Favorites, playback history, video resume tracking, Up Next `ModalBottomSheet` queue editor.
- **Stage 6 — Advanced Audio & Video (`13983a8`, `ae90175`)**: `SoniqAudioEffectsManager` (`Equalizer` presets/bands + `BassBoost`) with explicit unsupported-state UI, Gapless + Crossfade, `VideoResizeMode` (`FIT`/`FILL`/`ZOOM`), embedded & external `.srt`/`.vtt` subtitles + cue overlay, audio track selection, speed selector, immersive orientation toggle, and brightness/volume/seek gestures.
- **Stage 7 — Complete Theme Customization (`3f1fd87`, `c2b9264`)**: Custom HSL accent picker with Hex & WCAG contrast preview (`DEBT-003`), unreadable SAF background-image URI detection & recovery (`DEBT-004`), background scrim (`35%..95%`) & Android 12+ blur (`0..24 dp`) controls, `soniq.db` `MIGRATION_2_3`, preset Rename/Duplicate, and 4 curated non-destructive starter presets (`SoniqStarterPresets`).
- **Stage 8 — Performance, Accessibility & Compatibility (`c2b9264`)**: Off-main-thread 10k+ track aggregation, periodic `ensureActive()` scan cancellation checks, `>= 48dp` touch targets + `heading()` semantics, `currentWindowAdaptiveInfoV2()` (`0` compiler warnings).
- **Stage 9 — Release Preparation (`c1c710d`)**: Service-coordinated Sleep Timer, Android PiP (`VideoPlayerScreen` + `MainActivity`), externalized release signing template (`keystore.properties.example`).
- **UI/UX Visual Redesign Phase 1 (`47dc1a3`)**:
  - **Liquid Glass Design System**: `SoniqGlassTokens` (`SoniqGlassColors`, `SoniqGlassMetrics`, `SoniqGlassPalettes`), `SoniqTheme.glass` / `glassMetrics`, `SoniqBackground` lavender/periwinkle/violet ambient gradients + radial glow orbs, `SoniqGlassComponents` (`SoniqGlassCard`, `SoniqGlassIconButton`, `SoniqGlassPillChip`, `SoniqGlassSearchBar`, `SoniqGlassMediaRow`, `SoniqGlassLinearProgress`), and frosted-glass `SoniqSurfaceCard`.
  - **Home & Navigation**: `HomeViewModel` + `HomeScreen` (greeting, live offline search, Continue Listening hero card, Quick Access categories, Recently Played) wired as `SoniqRoute.Home` start destination with floating `SoniqGlassBottomBar` & `SoniqGlassNavigationRail`.
  - **Player & Libraries**: Liquid Glass `MiniPlayerBar`, `NowPlayingScreen` (glowing artwork halo, pink-to-cyan scrubber, frosted utility dock with Queue/EQ/Sleep Timer across all 3 `PlayerLayout`s), `MusicScreen` ("My Library" + pill tabs + Create New Playlist card), and `VideoScreen` (pill tabs + glass cards).

### Remaining Roadmap
- **UI/UX Visual Redesign Phase 2**: Dedicated Settings visual polish, VideoPlayerScreen glass control styling, and Home Quick Access deep-linking directly into specific `MusicLibraryTab`s.
- **Physical-Device & Hardware QA (`DEBT-005`)**: Re-run `connectedDebugAndroidTest` on `emulator-5554` (once host hypervisor acceleration is restored) and enable "Install via USB" on `Infinix_X6528` (`112173741O045288`) for physical-device + Bluetooth/headset QA.
- **Release Approval**: Remains **UNAPPROVED** until user supplies production signing `keystore.properties` and gives explicit approval.

---

## 4. Current Verification Snapshot

| Check | Status | Details |
|---|---|---|
| Debug Build (`assembleDebug`) | **PASS** | `0` compiler warnings (`app-debug.apk`, **18.07 MB**) |
| Release APK (`assembleRelease`) | **PASS** | R8 minified + shrunk, `lintVitalRelease` clean (`app-release-unsigned.apk`, **2.91 MB**) |
| Unit & Robolectric Tests (`testDebugUnitTest`) | **PASS** | **57 / 57 passed** across **28 suites** (0 failures, 0 errors, 0 skipped) |
| Android Lint (`lintDebug`) | **PASS** | **0 errors, 0 code/manifest/resource warnings** (3 informational `GradleDependency` notices only) |
| Instrumented Tests (`connectedDebugAndroidTest`) | **BLOCKED ON DEVICE (`DEBT-005`)** | `Infinix_X6528` (`112173741O045288`) attached, but `adb install` failed due to Infinix XOS USB-install restriction; last emulator run **3 / 3 passed** at `c2b9264` |
