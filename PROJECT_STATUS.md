# SONIQ — Project Status

**Last Updated**: 2026-11-18 (End of Autonomous Session 1)  
**Current Stage**: **Stage 1, Stage 2, and Stage 3 Complete**; Ready to Begin **Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Bluetooth Integration**  
**Latest Verified Code Commit**: `41c8fbc` (`main`)

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
| Playback | AndroidX Media3 `1.11.1` (`media3-exoplayer`, `media3-session`, `media3-common`, `media3-ui-compose`) |
| Persistence | Room `2.8.5` (`soniq.db` user DB + `soniq_media.db` rebuildable media cache, `exportSchema = true`), DataStore Preferences `1.3.0` |
| DI | Hilt `2.60.1` (KSP) |
| Images | Coil 3 `3.6.3` (`coil-compose` + `coil-video`, strictly offline/local fetchers) |
| Testing | JUnit `4.13.2`, Truth `1.4.5`, Turbine `1.2.1`, Coroutines Test `1.11.0`, Robolectric `4.18`, AndroidX Test & Compose UI Test |

---

## 3. Implementation Status Matrix

### Implemented & Verified
- **Stage 1 — Foundation & Hardening**:
  - `SoniqApplication`, `MainActivity` (edge-to-edge, eager `MainViewModel` splash gate), adaptive `SoniqApp` navigation shell.
  - Centralized tokenized design system (`SoniqTokens`, zero hardcoded visual literals), Light / Dark / AMOLED (`#000000`) modes, Material You dynamic color (API 31+), WCAG-safe HSL accent generator, typography & text scaling, Solid / Gradient / Image backgrounds, Room-backed saved theme presets.
  - API 26–37 runtime permissions including Android 14+ partial visual media access (`READ_MEDIA_VISUAL_USER_SELECTED`).
  - Verified offline/privacy enforcement (`INTERNET` and `ACCESS_NETWORK_STATE` stripped and absent from merged release manifest).
- **Stage 2 — Local Media Discovery & Library Management**:
  - Separate non-backed-up Room cache database (`soniq_media.db`, exported schema v1) with indexed `AudioTrackEntity` and `VideoItemEntity`, atomic chunked `syncWith` DAOs.
  - Real `MediaStoreAudioScanner` and `MediaStoreVideoScanner` (API 26–37 `RELATIVE_PATH`/`DATA` normalization, album art URIs, zero-duration filtering) + `DefaultMusicLibraryRepository` and `DefaultVideoLibraryRepository` with `Mutex`-guarded sync and debounced `MediaStoreChangeObserver` auto-resync.
  - Full `MusicScreen` (Songs, Albums, Artists, Folders tabs, drill-down filters, sorting, offline search, refresh, error retry) and `VideoScreen` (Videos, Folders tabs, Coil offline video thumbnails, duration/resolution/size metadata, Android 14+ partial-access management banner).
- **Stage 3 — Audio and Video Playback Engine**:
  - Rich `MediaItemFactory` metadata mapping (`MEDIA_TYPE_MUSIC` vs `MEDIA_TYPE_VIDEO`), dynamic `AudioAttributes` switching (`AUDIO_CONTENT_TYPE_MUSIC` vs `AUDIO_CONTENT_TYPE_MOVIE`) in `SoniqPlaybackService`.
  - Application-scoped `Media3PlaybackController` with cold-start command queuing, active-only 250ms position ticker, queue/transport/seek/repeat/shuffle/speed controls, `player: StateFlow<Player?>` surface handle, and clean disconnect/release/reconnect lifecycle.
  - Docked audio `MiniPlayerBar` on top-level screens, full-screen `NowPlayingScreen` honoring all three `PlayerLayout` presets (`CLASSIC`, `ARTWORK_FOCUS`, `COMPACT`), and dedicated full-screen `VideoPlayerScreen` using Media3 Compose `PlayerSurface`.

### Partially Implemented / Pending Deeper Stages
- **Stage 4 (Background Playback & System Media Integration)**: `SoniqPlaybackService` (`MediaSessionService`) and rich `MediaMetadata` are live, but `POST_NOTIFICATIONS` graceful flow on API 33+, custom notification button layout, lock-screen/Bluetooth/headset command integration tests, and background video preference toggles are **planned for Stage 4**.

### Planned (Not Yet Started)
- Stage 4: Notification customization, lock-screen, Bluetooth/media button QA, background playback controls.
- Stage 5: Custom playlists, favorites, global search, playback history & resume positions, Up Next editable queue sheet.
- Stage 6: Equalizer/audio effects, sleep timer, video gestures, subtitles, aspect ratio, PiP.
- Stage 7: Arbitrary custom accent color picker UI, background image broken-URI recovery, starter presets.
- Stage 8: Large-library profiling, accessibility audit, multi-API hardening.
- Stage 9: Release signing setup, physical-device QA, final release approval.

---

## 4. Current Verification Snapshot

| Check | Status | Details |
|---|---|---|
| Debug Build (`assembleDebug`) | **PASS** | 0 compiler warnings (`app-debug.apk`, 17.35 MB) |
| Release Build (`assembleRelease`) | **PASS** | R8 minified + shrunk, `lintVitalRelease` clean (`app-release-unsigned.apk`, 2.72 MB) |
| Unit & Robolectric Tests (`testDebugUnitTest`) | **PASS** | **39 / 39 passed** across 19 suites (0 failures, 0 errors, 0 skipped) |
| Android Lint (`lintDebug`) | **PASS** | **0 errors, 0 warnings** |
| Instrumented Tests (`connectedDebugAndroidTest`) | **PASS on Emulator** | 1 / 1 passed on `Medium_Phone_API_37.0` (Android 17) + clean launch check (`PID 4807`, 0 SONIQ crashes) |
| Physical Device Verification | **NOT VERIFIED** | Device `112173741O045288` remained `authorizing` in ADB throughout Session 1 |

---

## 5. Known Blockers & Next Highest-Priority Objectives

- **External Blocker (for physical-device testing only)**: Phone `112173741O045288` needs on-device USB debugging RSA authorization acceptance.
- **Next Highest-Priority Objectives**:
  1. **Stage 4**: Custom Media3 notification actions, `POST_NOTIFICATIONS` runtime handling on API 33+, media button / Bluetooth / lock-screen verification, and optional background-video playback preference.
  2. **Stage 5**: Extend `soniq.db` (tested Room migration `v1 → v2`) for custom playlists, favorites, playback history, and video resume positions + Up Next queue sheet.
