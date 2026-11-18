# SONIQ — Project Status

**Last Updated**: 2026-11-18  
**Current Stage**: Stage 1 Complete (Stage 1.1 + Stage 1.1b Hardening); Ready to Begin **Stage 2 — Local Media Discovery and Library Management**  
**Latest Verified Commit**: `46be15f` (`main`)

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
| Playback | AndroidX Media3 `1.11.1` (`media3-exoplayer`, `media3-session`, `media3-common`) |
| Persistence | Room `2.8.5` (`exportSchema = true`), DataStore Preferences `1.3.0` |
| DI | Hilt `2.60.1` (KSP) |
| Images | Coil 3 `3.6.3` (`coil-compose`, local/offline fetchers only) |
| Testing | JUnit `4.13.2`, Truth `1.4.5`, Turbine `1.2.1`, Coroutines Test `1.11.0`, Robolectric `4.18`, AndroidX Test & Compose UI Test |

---

## 3. Implementation Status Matrix

### Implemented & Verified
- **Application initialization**: `SoniqApplication`, `MainActivity` (edge-to-edge, `SplashScreen` synced to eager `MainViewModel`), `SoniqApp`.
- **Navigation shell**: Adaptive `NavigationSuiteScaffold` switching cleanly between separate `Music`, `Video`, and `Settings` top-level routes with saved tab state.
- **Design system & theming**: Centralized `SoniqTokens` (zero hardcoded visual constants), Light / Dark / AMOLED (`#000000`) modes, Material You dynamic color (API 31+), HSL accent color generator (`ColorMath`, WCAG ≥ 4.58:1), font family & text scaling, Solid / Gradient / Image backgrounds with persistable SAF URI permission and scrim, player layout preference persistence, and Room-backed theme preset CRUD in `SettingsScreen`.
- **Runtime media permissions**: API 26–37 permission resolution including Android 14+ Selected Photos/Videos Access (`READ_MEDIA_VISUAL_USER_SELECTED`), distinguishing `FULL`, `PARTIAL`, `NONE`, and `DENIED_PERMANENTLY` independently for Audio and Video.
- **Offline/privacy enforcement**: `INTERNET` and `ACCESS_NETWORK_STATE` stripped via `tools:node="remove"` (verified absent in merged release manifest); zero network fetchers, analytics, or ad SDKs.

### Partially Implemented (Foundational Infrastructure Only)
- **Media discovery (`core.mediadiscovery`)**: MediaStore collection URI resolver (`MediaStoreCollections`) and `ContentObserver` Flow (`AndroidMediaStoreChangeObserver`) exist; actual MediaStore audio/video scanners, Room media index DB, and library repositories are **not yet implemented**.
- **Playback engine (`core.playback`)**: `SoniqPlaybackService` (`MediaSessionService` + `ExoPlayer`) and `Media3PlaybackController` (with disconnect/release/reconnect recovery) exist; media-item loading, queue management, playback UI, video rendering surface, and dynamic audio content types (`MUSIC` vs `MOVIE`) are **not yet implemented**.
- **Music & Video screens**: Permission-aware shells exist; media lists, tabs, artwork/thumbnails, and playback triggers are **not yet implemented**.

### Planned (Not Yet Started)
- Stage 2: Local media discovery, indexing (`soniq_media.db`), Music & Video library views.
- Stage 3: Full audio & video playback engine, Now Playing (with `PlayerLayout` variants), mini-player, video player screen.
- Stage 4: Full MediaSession metadata/artwork, notification controls, lock-screen, Bluetooth/media button integration.
- Stage 5: Playlists, favorites, search, history, queue management.
- Stage 6: Equalizer/audio effects, playback speed, sleep timer, video gestures, subtitles, aspect ratio, PiP.
- Stage 7: Arbitrary custom accent color picker UI, custom background recovery & tuning, additional presets.
- Stage 8: Performance profiling, accessibility pass, compatibility hardening.
- Stage 9: Release signing setup, final APK generation, end-to-end physical-device QA, user release approval.

---

## 4. Current Verification Snapshot

| Check | Status | Details |
|---|---|---|
| Debug Build (`assembleDebug`) | **PASS** | Zero compiler warnings; `app-debug.apk` built |
| Release Build (`assembleRelease`) | **PASS** (Stage 1.1; re-verify after Stage 2) | R8 minified + shrunk (`app-release-unsigned.apk`, 2.45 MB at Stage 1.1) |
| Unit & Robolectric Tests (`testDebugUnitTest`) | **PASS** | **21 / 21 passed** across 11 suites |
| Android Lint (`lintDebug`) | **PASS** | **0 errors, 0 warnings** |
| Instrumented Tests (`connectedDebugAndroidTest`) | **PASS on Emulator** | 1 / 1 passed on `Medium_Phone_API_37.0` (Android 17) + clean launch check (`PID 7792`, 0 crashes) |
| Physical Device Verification | **NOT VERIFIED** | Device `112173741O045288` detected as `authorizing` in ADB (RSA prompt pending on phone screen) |

---

## 5. Known Blockers & Highest-Priority Next Objectives

- **Blocker (non-blocking for automated development)**: Physical phone `112173741O045288` requires on-device USB debugging authorization confirmation before physical-device deployment/tests can run. Emulator `Medium_Phone_API_37.0` is operational as an automated verification target.
- **Next Highest-Priority Objective**: **Stage 2 — Local Media Discovery and Library Management** (separate non-backed-up `SoniqMediaDatabase`, MediaStore audio & video scanners, incremental sync via `MediaStoreChangeObserver`, Music library UI with Songs/Albums/Artists/Folders, Video library UI with Videos/Folders, and full unit/Room/UI tests).
