# SONIQ — Project Status

**Last Updated**: 2026-11-18 (End of Autonomous Session 3)  
**Current Stage**: **Stages 1–6 Complete + Stage 7 Foundation Complete**; Ready to Finish **Stage 7 (Starter Presets & Scrim/Blur)** & **Stage 8 (Performance, Accessibility & Compatibility)**  
**Latest Verified Code Commit**: `3f1fd87` (`main`)

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
| UI | Jetpack Compose (`compose-bom:2026.09.00`), Material 3, Adaptive Navigation Suite, Core SplashScreen `1.2.0` |
| Navigation | Navigation Compose `2.10.2` (type-safe `@Serializable` routes), `hilt-lifecycle-viewmodel-compose:1.4.0` |
| Playback | AndroidX Media3 `1.11.1` (`media3-exoplayer`, `media3-session`, `media3-common`, `media3-ui-compose`) + `android.media.audiofx.Equalizer` / `BassBoost` |
| Persistence | Room `2.8.5` (`soniq.db` v2 user DB with tested `MIGRATION_1_2` + `soniq_media.db` v1 rebuildable media cache, `exportSchema = true`), DataStore Preferences `1.3.0` (`appearance.preferences_pb` + `playback_state.preferences_pb`) |
| DI | Hilt `2.60.1` (KSP) |
| Images | Coil 3 `3.6.3` (`coil-compose` + `coil-video`, strictly offline/local fetchers) + `SoniqOfflineBitmapLoader` |
| Testing | JUnit `4.13.2`, Truth `1.4.5`, Turbine `1.2.1`, Coroutines Test `1.11.0`, Robolectric `4.18`, Room Testing `2.8.5`, AndroidX Test & Compose UI Test |

---

## 3. Implementation Status Matrix

### Implemented & Verified
- **Stage 1 — Foundation & Hardening (`2b8937f`, `46be15f`)**:
  - `SoniqApplication`, `MainActivity`, adaptive `SoniqApp` shell, centralized `SoniqTokens`, Light / Dark / AMOLED (`#000000`) modes, Material You dynamic color (API 31+), WCAG-safe HSL accent generator, typography & text scaling, Solid / Gradient / Image backgrounds, Room-backed saved theme presets, offline/privacy manifest stripping.
- **Stage 2 — Local Media Discovery & Library Management (`53cbae1`)**:
  - Dedicated non-backed-up `SoniqMediaDatabase` (`soniq_media.db`), `MediaStoreAudioScanner`, `MediaStoreVideoScanner`, `Mutex`-guarded repositories, debounced `MediaStoreChangeObserver`, `MusicScreen`, `VideoScreen`, Android 14+ partial-access banner.
- **Stage 3 — Audio & Video Playback Engine (`41c8fbc`)**:
  - `MediaItemFactory`, dynamic `AUDIO_CONTENT_TYPE_MUSIC`/`MOVIE` switching in `SoniqPlaybackService`, `Media3PlaybackController`, `MiniPlayerBar`, `NowPlayingScreen` (all 3 `PlayerLayout`s), `VideoPlayerScreen` (`PlayerSurface`).
- **Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Media Buttons (`057fb56`, `c5fabc0`, `13983a8`)**:
  - `DefaultMediaNotificationProvider`, strictly offline `SoniqOfflineBitmapLoader`, `MediaButtonReceiver`, audio focus, noisy-audio pause, `WAKE_MODE_LOCAL`, `POST_NOTIFICATIONS` non-blocking one-time prompt + Settings Allow button, and hardened `NonCancellable` queue persistence (`onTimelineChanged`, `onPositionDiscontinuity`, empty-queue clear, paused cold-start restoration).
- **Stage 5 — Playlists, Favorites, History, Resume Positions & Queue Management (`f265823`)**:
  - Non-destructive Room migration `soniq.db` `v1 → v2` (`MIGRATION_1_2`), `UserLibraryRepository`, custom playlists CRUD, Favorites tab & heart toggle, track overflow actions, Up Next `ModalBottomSheet` queue editor, automatic playback history & video resume tracking + thumbnail progress bars.
- **Stage 6 — Advanced Audio & Video (`13983a8`, `ae90175`)**:
  - **Audio**: `SoniqAudioEffectsManager` + `AndroidAudioEffectsBackend` (`Equalizer` presets/bands + `BassBoost`) attached to ExoPlayer's `audioSessionId` with explicit device capability detection & unsupported-state UI (`AudioEffectsSheet`), Gapless toggle, and configurable volume-envelope Crossfade (`Off`, `2s`–`10s`).
  - **Video**: `VideoResizeMode` (`FIT`, `FILL`, `ZOOM`), embedded & external local `.srt`/`.vtt` subtitle selection + cue overlay rendering, audio track selection, speed chip bar (`0.5x`–`2.0x`), immersive system bars & landscape orientation toggle, and horizontal-seek / left-brightness / right-volume / double-tap gestures + playback error banner.
- **Stage 7 Foundation — Theme Personalization (`3f1fd87`)**:
  - Custom HSL Accent Color Picker dialog with live WCAG contrast preview & Hex readout (resolving `DEBT-003`), broken SAF background-image URI detection & recovery banner (resolving `DEBT-004`), and theme preset Rename & Duplicate actions.

### Planned / Remaining
- **Stage 7 Remainder**: Curated built-in starter theme presets and background image scrim/blur controls.
- **Stage 8**: Large-library (10k+ tracks) profiling, accessibility audit, `currentWindowAdaptiveInfoV2` migration (`DEBT-008`), multi-API hardening.
- **Stage 9**: Release signing setup, physical-device & Bluetooth/headset QA, final release approval.

---

## 4. Current Verification Snapshot

| Check | Status | Details |
|---|---|---|
| Debug Build (`assembleDebug`) | **PASS** | `app-debug.apk` (**18.06 MB**) |
| Release Build (`assembleRelease`) | **PASS** | R8 minified + shrunk, `lintVitalRelease` clean (`app-release-unsigned.apk`, **2.90 MB**) |
| Unit & Robolectric Tests (`testDebugUnitTest`) | **PASS** | **54 / 54 passed** across 26 suites (0 failures, 0 errors, 0 skipped) |
| Android Lint (`lintDebug`) | **PASS** | **0 errors, 0 warnings** (`Lint results count: 0`) |
| Instrumented Tests (`connectedDebugAndroidTest`) | **PASS on Emulator** | **3 / 3 passed** on `Medium_Phone_API_37.0` (Android 17): `SoniqDatabaseMigrationTest`, `SoniqInstrumentedSmokeTest`, `SoniqPlaybackServiceInstrumentedTest` + clean launch check (`PID 4321`, 0 SONIQ crashes) |
| Physical Device Verification (`Infinix_X6528`, `112173741O045288`) | **NOT VERIFIED** | Device disconnected in Session 3 (`adb devices` empty); previously blocked by Infinix XOS interactive "Install via USB" prompt (`DEBT-005`) |
| Physical Bluetooth / Headset Hardware | **NOT VERIFIED** | Pending physical-device hardware QA |
