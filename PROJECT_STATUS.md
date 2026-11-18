# SONIQ — Project Status

**Last Updated**: 2026-11-18 (End of Autonomous Session 2)  
**Current Stage**: **Stages 1, 2, 3, 4, and 5 Complete**; Ready to Begin **Stage 6 — Advanced Audio and Video Features**  
**Latest Verified Code Commit**: `c5fabc0` (`main`)

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
| Persistence | Room `2.8.5` (`soniq.db` v2 user DB with tested `MIGRATION_1_2` + `soniq_media.db` v1 rebuildable media cache, `exportSchema = true`), DataStore Preferences `1.3.0` (`appearance.preferences_pb` + `playback_state.preferences_pb`) |
| DI | Hilt `2.60.1` (KSP) |
| Images | Coil 3 `3.6.3` (`coil-compose` + `coil-video`, strictly offline/local fetchers) + `SoniqOfflineBitmapLoader` |
| Testing | JUnit `4.13.2`, Truth `1.4.5`, Turbine `1.2.1`, Coroutines Test `1.11.0`, Robolectric `4.18`, Room Testing `2.8.5`, AndroidX Test & Compose UI Test |

---

## 3. Implementation Status Matrix

### Implemented & Verified
- **Stage 1 — Foundation & Hardening (`2b8937f`, `46be15f`)**:
  - `SoniqApplication`, `MainActivity` (edge-to-edge, eager `MainViewModel` splash gate), adaptive `SoniqApp` navigation shell.
  - Centralized tokenized design system (`SoniqTokens`, zero hardcoded visual literals), Light / Dark / AMOLED (`#000000`) modes, Material You dynamic color (API 31+), WCAG-safe HSL accent generator, typography & text scaling, Solid / Gradient / Image backgrounds, Room-backed saved theme presets.
  - API 26–37 runtime permissions including Android 14+ partial visual media access (`READ_MEDIA_VISUAL_USER_SELECTED`).
  - Verified offline/privacy enforcement (`INTERNET` and `ACCESS_NETWORK_STATE` stripped and absent from merged release manifest).
- **Stage 2 — Local Media Discovery & Library Management (`53cbae1`)**:
  - Separate non-backed-up Room cache database (`soniq_media.db`, exported schema v1) with indexed `AudioTrackEntity` and `VideoItemEntity`, atomic chunked `syncWith` DAOs.
  - Real `MediaStoreAudioScanner` and `MediaStoreVideoScanner` (API 26–37 `RELATIVE_PATH`/`DATA` normalization, album art URIs, zero-duration filtering) + `DefaultMusicLibraryRepository` and `DefaultVideoLibraryRepository` with `Mutex`-guarded sync and debounced `MediaStoreChangeObserver` auto-resync.
  - Full `MusicScreen` and `VideoScreen` with sorting, offline search, refresh, error retry, Coil offline video thumbnails, and Android 14+ partial-access management banner.
- **Stage 3 — Audio and Video Playback Engine (`41c8fbc`)**:
  - Rich `MediaItemFactory` metadata mapping (`MEDIA_TYPE_MUSIC` vs `MEDIA_TYPE_VIDEO`), dynamic `AudioAttributes` switching (`AUDIO_CONTENT_TYPE_MUSIC` vs `AUDIO_CONTENT_TYPE_MOVIE`) in `SoniqPlaybackService`.
  - Application-scoped `Media3PlaybackController` with cold-start command queuing, active-only 250ms position ticker, queue/transport/seek/repeat/shuffle/speed controls, `player: StateFlow<Player?>` surface handle, and clean disconnect/release/reconnect lifecycle.
  - Docked audio `MiniPlayerBar`, full-screen `NowPlayingScreen` (all 3 `PlayerLayout` variants), and dedicated full-screen `VideoPlayerScreen` (`PlayerSurface`).
- **Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Media Buttons (`057fb56`, `c5fabc0`)**:
  - Custom `DefaultMediaNotificationProvider` (`soniq_playback_channel`, `ic_music_note`), strictly offline `SoniqOfflineBitmapLoader` (rejecting `http`/`https`, downsampling local/embedded art to `512px`), and custom `MediaSession.Callback` (`COMMAND_TOGGLE_SHUFFLE`, `COMMAND_CYCLE_REPEAT` in `SLOT_OVERFLOW`, preserving compact Prev/Play-Pause/Next slots).
  - Exported `androidx.media3.session.MediaButtonReceiver` (`MEDIA_BUTTON`) for headset/Bluetooth/lock-screen transport routing, audio focus (`handleAudioFocus = true`), headphone disconnect handling (`handleAudioBecomingNoisy = true`), and wake lock (`WAKE_MODE_LOCAL`).
  - `onTaskRemoved` keeps active background audio alive while cleanly stopping idle/paused sessions; leak-free `onDestroy` releases `MediaSession` and `ExoPlayer`.
  - Playback session persistence & restoration via `@PlaybackDataStore` (`playback_state.preferences_pb`, excluded from backup) + `PlaybackRestorationManager` (`onPlaybackResumption` + cold-start paused restoration in `Media3PlaybackController`).
  - Optional non-blocking Android 13+ `POST_NOTIFICATIONS` permission prompt + Settings "Playback" card (background video audio continuation toggle + notification permission Allow button).
- **Stage 5 — Playlists, Favorites, History, Resume Positions & Queue Management (`f265823`)**:
  - Non-destructive Room database migration **`soniq.db` `v1 → v2`** (`SoniqDatabaseMigrations.MIGRATION_1_2`, exported schema `SoniqDatabase/2.json`, verified via both Robolectric unit migration test and real Android `MigrationTestHelper` preserving existing user `theme_presets`).
  - Persistent `playlists`, `playlist_tracks` (FK cascade), `favorite_media`, `playback_history` (auto-pruned), and `playback_resume` tables backed by `UserLibraryRepository` / `DefaultUserLibraryRepository`.
  - Custom playlists CRUD (create, rename, delete, add track, remove track, drill-down view), Favorites tab & `NowPlayingScreen` heart toggle, track overflow actions (`Play next`, `Add to queue`, `Favorite`, `Add to playlist`), Up Next `ModalBottomSheet` queue manager (select, reorder up/down, remove), automatic playback history logging, and automatic video resume position save/restore with progress indicator overlays on `VideoScreen` thumbnails.

### Planned (Not Yet Started)
- Stage 6: Equalizer/audio effects, sleep timer, video gestures, subtitles, aspect ratio, PiP.
- Stage 7: Arbitrary custom accent color picker UI, background image broken-URI recovery, starter presets.
- Stage 8: Large-library profiling, accessibility audit, multi-API hardening.
- Stage 9: Release signing setup, physical-device QA, final release approval.

---

## 4. Current Verification Snapshot

| Check | Status | Details |
|---|---|---|
| Debug Build (`assembleDebug`) | **PASS** | `app-debug.apk` (**18.04 MB**) |
| Release Build (`assembleRelease`) | **PASS** | R8 minified + shrunk, `lintVitalRelease` clean (`app-release-unsigned.apk`, **2.85 MB**) |
| Unit & Robolectric Tests (`testDebugUnitTest`) | **PASS** | **49 / 49 passed** across 25 suites (0 failures, 0 errors, 0 skipped) |
| Android Lint (`lintDebug`) | **PASS** | **0 errors, 0 warnings** (`Lint results count: 0`) |
| Instrumented Tests (`connectedDebugAndroidTest`) | **PASS on Emulator** | **3 / 3 passed** on `Medium_Phone_API_37.0` (Android 17): `SoniqInstrumentedSmokeTest`, `SoniqPlaybackServiceInstrumentedTest`, `SoniqDatabaseMigrationTest` + clean launch check (`PID 4611`, 0 SONIQ crashes) |
| Physical Device Verification | **NOT VERIFIED** | Device `Infinix_X6528` (`112173741O045288`, Android 13) is ADB-authorized, but `adb install` times out waiting for Infinix XOS interactive "Install via USB" approval |
| Physical Bluetooth / Headset Hardware | **NOT VERIFIED** | Manifest `MediaButtonReceiver`, noisy audio, and `MediaSession` commands are unit/emulator tested; real Bluetooth/headset hardware testing remains pending |

---

## 5. Known Blockers & Next Highest-Priority Objectives

- **External Blocker (for physical-device testing only)**: On `Infinix_X6528` (`112173741O045288`), enable Developer Options → **"Install via USB"** (and disable XOS USB-install confirmation prompt) or tap Allow on screen during `adb install` so physical-device instrumented tests and real Bluetooth/headset QA can run.
- **Next Highest-Priority Objectives**:
  1. **Stage 6**: Audio sleep timer & `AudioEffect` 5-band Equalizer / Bass Boost attached to ExoPlayer `audioSessionId`; Video aspect ratio / resize modes, brightness/volume/seek gestures, local subtitle/audio track selection, and Picture-in-Picture (PiP).
  2. **Stage 7**: Interactive HSL/Hex accent color picker dialog, broken SAF background URI recovery banner, and curated starter theme presets.
