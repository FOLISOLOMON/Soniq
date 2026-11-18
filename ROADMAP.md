# SONIQ — Development Roadmap

**Last Updated**: 2026-11-18 (End of Autonomous Session 3)  
**Active Stage**: Stages 1–6 **COMPLETED**, Stage 7 **IN PROGRESS (Foundation Complete)** → Next: **Finish Stage 7 & Stage 8**

---

## Stage 1 — Foundation and Project Hardening — **COMPLETED** (`2b8937f`, `46be15f`)
- Adaptive navigation shell, tokenized design system (`SoniqTokens`), Light/Dark/AMOLED/Dynamic Color, offline privacy manifest enforcement, Android 14+ partial visual media access, JVM 17 target.

## Stage 2 — Local Media Discovery and Library Management — **COMPLETED** (`53cbae1`)
- Dedicated `SoniqMediaDatabase` (`soniq_media.db`), `MediaStoreAudioScanner`, `MediaStoreVideoScanner`, `Mutex`-guarded sync + debounced `MediaStoreChangeObserver`, `MusicScreen`, `VideoScreen`, `coil-video`.

## Stage 3 — Audio and Video Playback Engine — **COMPLETED** (`41c8fbc`)
- `MediaItemFactory`, dynamic `MUSIC`/`MOVIE` `AudioAttributes`, `Media3PlaybackController`, `MiniPlayerBar`, `NowPlayingScreen` (3 layouts), `VideoPlayerScreen` (`PlayerSurface`).

## Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Bluetooth — **COMPLETED** (`057fb56`, `c5fabc0`, `13983a8`)
- `DefaultMediaNotificationProvider`, `SoniqOfflineBitmapLoader`, `MediaButtonReceiver`, `POST_NOTIFICATIONS` flow, audio focus, noisy audio, `NonCancellable` queue persistence (`onTimelineChanged`, `onPositionDiscontinuity`, empty-queue clear) & paused cold-start restoration.

## Stage 5 — Playlists, Favorites, Search, History & Queue Management — **COMPLETED** (`f265823`)
- Tested Room migration `soniq.db` `v1 → v2` (`MIGRATION_1_2`), `UserLibraryRepository`, playlists CRUD, Favorites, playback history, video resume tracking, Up Next `ModalBottomSheet` queue editor.

---

## Stage 6 — Advanced Audio and Video Features — **COMPLETED** (`13983a8`, `ae90175`)
- **Delivered**:
  1. Audio: `SoniqAudioEffectsManager` + `AndroidAudioEffectsBackend` (`Equalizer` presets/bands + `BassBoost`) bound to ExoPlayer's `audioSessionId` with explicit device capability detection and unsupported-state UI (`AudioEffectsSheet`), Gapless toggle, and configurable Crossfade (`Off`, `2s`–`10s`).
  2. Video: Fit / Fill / Zoom aspect modes (`VideoResizeMode`), embedded & external `.srt`/`.vtt` subtitle selection + cue overlay rendering, audio track selection, speed selector (`0.5x`–`2.0x`), immersive fullscreen & landscape orientation toggle, brightness/volume/seek/double-tap gestures, and playback error overlay.
  3. Deferred to future polish if desired: Android Picture-in-Picture (PiP) mode & Sleep Timer.
- **Verified**: 54/54 unit & Robolectric tests, 0 Lint warnings, R8 release build PASS (`2.90 MB`), 3/3 emulator instrumented tests PASS on `Medium_Phone_API_37.0`.

---

## Stage 7 — Complete Theme Customization and Personalization — **IN PROGRESS (`3f1fd87`)**
- **Completed in Session 3 (Foundation)**:
  1. Custom HSL Accent Color Picker dialog with live WCAG contrast preview & Hex readout (resolving `DEBT-003`).
  2. Unreadable background-image URI detection & recovery banner in Settings (resolving `DEBT-004`).
  3. Theme preset Rename & Duplicate actions (`ThemePresetDao.renameById`, `AppearanceRepository.renamePreset` / `duplicatePreset`).
- **Remaining for Stage 7**:
  1. Curated built-in starter theme presets & preset overwrite-current action.
  2. Background image scrim/blur intensity controls.

---

## Stage 8 — Performance, Reliability, Privacy, Accessibility & Compatibility — **NEXT**
- **Objectives**:
  1. Large-library profiling (10k+ tracks), Compose stability/recomposition audit, StrictMode & memory leak checks.
  2. Full TalkBack / contentDescription / touch-target (≥ 48dp) / large-font accessibility pass.
  3. Migrate `SoniqApp.kt` to `currentWindowAdaptiveInfoV2` (`DEBT-008`) & multi-API compatibility checks across API 26–37.

---

## Stage 9 — Release Preparation, APK Generation & Final Verification — **PLANNED**
- **Objectives**:
  1. Externalized release signing template (`keystore.properties`, gitignored) without committing secrets.
  2. Final R8-minified release APK & AAB build, merged-manifest privacy audit, full regression suite, physical-device & Bluetooth/headset end-to-end QA checklist.
  3. Update `RELEASE_REPORT.md` and wait for **explicit user release approval**.
