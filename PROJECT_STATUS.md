# SONIQ — Project Status

**Last Updated**: 2026-11-20 (End of Autonomous Session 5 — Stage 9 Engineering Complete)  
**Current Stage**: **Stages 1–9 Engineering Complete** (Sleep Timer, Picture-in-Picture, Externalized Release Signing Template, R8 Release APK & AAB Generated) → Ready for **Physical-Device QA & Post-Roadmap UI/UX Visual Redesign Phase**  
**Latest Verified Code Commit**: `c1c710d` (`main`)

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

### Implemented & Verified (Stages 1–8)
- **Stage 1 — Foundation & Hardening (`2b8937f`, `46be15f`)**: `SoniqApplication`, `MainActivity`, adaptive `SoniqApp` shell, centralized `SoniqTokens`, Light/Dark/AMOLED/Dynamic Color, offline/privacy manifest stripping.
- **Stage 2 — Local Media Discovery & Library Management (`53cbae1`)**: `soniq_media.db`, `MediaStoreAudioScanner`, `MediaStoreVideoScanner`, `Mutex`-guarded repositories, debounced `MediaStoreChangeObserver`, `MusicScreen`, `VideoScreen`, Android 14+ partial-access banner.
- **Stage 3 — Audio & Video Playback Engine (`41c8fbc`)**: `MediaItemFactory`, dynamic `MUSIC`/`MOVIE` `AudioAttributes`, `Media3PlaybackController`, `MiniPlayerBar`, `NowPlayingScreen` (3 layouts), `VideoPlayerScreen` (`PlayerSurface`).
- **Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Media Buttons (`057fb56`, `c5fabc0`, `13983a8`)**: `DefaultMediaNotificationProvider`, `SoniqOfflineBitmapLoader`, `MediaButtonReceiver`, `POST_NOTIFICATIONS` flow, `NonCancellable` queue persistence & paused cold-start restoration.
- **Stage 5 — Playlists, Favorites, History, Resume Positions & Queue Management (`f265823`)**: `soniq.db` `MIGRATION_1_2`, `UserLibraryRepository`, playlists CRUD, Favorites, playback history, video resume tracking, Up Next `ModalBottomSheet` queue editor.
- **Stage 6 — Advanced Audio & Video (`13983a8`, `ae90175`)**: `SoniqAudioEffectsManager` (`Equalizer` presets/bands + `BassBoost`) with explicit unsupported-state UI, Gapless + Crossfade, `VideoResizeMode` (`FIT`/`FILL`/`ZOOM`), embedded & external `.srt`/`.vtt` subtitles + cue overlay, audio track selection, speed selector, immersive orientation toggle, and brightness/volume/seek gestures.
- **Stage 7 — Complete Theme Customization (`3f1fd87`, `c2b9264`)**: Custom HSL accent picker with Hex & WCAG contrast preview (`DEBT-003`), unreadable SAF background-image URI detection & recovery (`DEBT-004`), background scrim (`35%..95%`) & Android 12+ blur (`0..24 dp`) controls, `soniq.db` `MIGRATION_2_3`, preset Rename/Duplicate, and 4 curated non-destructive starter presets (`SoniqStarterPresets`).
- **Stage 8 — Performance, Accessibility & Compatibility (`c2b9264`)**:
  - **Performance**: Off-main-thread 10k+ track aggregation (`.flowOn(Dispatchers.Default)` & `withContext(Dispatchers.IO)` in `DefaultMediaLibraryRepositories`), periodic `ensureActive()` scan cancellation checks in `MediaStoreAudioScanner`/`MediaStoreVideoScanner`, verified 10,000-track stress test.
  - **Accessibility**: Minimum `48.dp` touch target on accent swatches, `heading()` semantics on `SoniqSectionHeader` and modal sheet titles (`AudioEffectsSheet`, `Up Next`, `Audio & Subtitles`).
  - **Compatibility**: Migrated `SoniqApp` to `currentWindowAdaptiveInfoV2()` (`DEBT-008`, 0 compiler warnings).

### Remaining Roadmap
- **Physical-Device & Hardware QA (`DEBT-005`)**: Re-run `connectedDebugAndroidTest` on `emulator-5554` (once host hypervisor acceleration is restored) and execute physical-device + Bluetooth/headset QA on `Infinix_X6528`.
- **Post-Roadmap**: Dedicated **UI/UX Redesign Phase** (explicitly frozen until engineering roadmap completes).
- **Release Approval**: Remains **UNAPPROVED** until user supplies production signing `keystore.properties` and gives explicit approval.

---

## 4. Current Verification Snapshot

| Check | Status | Details |
|---|---|---|
| Debug Build (`assembleDebug`) | **PASS** | `0` compiler warnings (`app-debug.apk`, **18.07 MB**) |
| Release APK (`assembleRelease`) | **PASS** | R8 minified + shrunk, `lintVitalRelease` clean (`app-release-unsigned.apk`, **2.92 MB**) |
| Release Bundle (`bundleRelease`) | **PASS** | R8 minified + shrunk (`app-release.aab`, **7.10 MB**) |
| Unit & Robolectric Tests (`testDebugUnitTest`) | **PASS** | **56 / 56 passed** across 27 suites (0 failures, 0 errors, 0 skipped) |
| Android Lint (`lintDebug`) | **PASS** | **0 errors, 0 code/manifest warnings** (3 informational `GradleDependency` notices only) |
| Instrumented Tests (`connectedDebugAndroidTest`) | **PASS at `c2b9264` / Pending re-run on `c1c710d`** | **3 / 3 passed** on `Medium_Phone_API_37.0` at `c2b9264`; emulator failed to boot in Session 5 due to missing host hypervisor acceleration after reboot |
| Physical Device Verification (`Infinix_X6528`, `112173741O045288`) | **NOT VERIFIED** | Device not attached to ADB in Session 5 (`DEBT-005`) |
