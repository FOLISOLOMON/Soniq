# SONIQ — Development Roadmap

**Last Updated**: 2026-11-18  
**Active Stage**: Transitioning from **Stage 1 (Complete)** → **Stage 2 (Next)**

---

## Stage 1 — Foundation and Project Hardening — **COMPLETED**
- **Sub-stages**:
  - **1.1 Project Initialization & Architecture** (`2b8937f`): Gradle 9.9.1 / AGP 9.5.1 / Kotlin 2.4.21 / KSP / Compose BOM / Media3 / Room / DataStore / Hilt / Coil, single `:app` module with strict package boundaries, adaptive navigation shell, centralized tokenized design system, theme presets, offline privacy enforcement.
  - **1.1b Hardening Pass** (`46be15f`): Android 14+ `READ_MEDIA_VISUAL_USER_SELECTED` partial video access, `Media3PlaybackController` disconnect/release/reconnect lifecycle, eager `MainViewModel` splash loading, removal of all hardcoded visual constants, explicit Kotlin JVM 17 target, 0 Lint warnings, emulator instrumented verification.
- **Verification**: 21/21 unit & Robolectric tests, 1/1 emulator instrumented test, 0 Lint errors/warnings.

---

## Stage 2 — Local Media Discovery and Library Management — **NEXT (IN PROGRESS IN SESSION 1)**
- **Objectives**:
  1. Introduce a dedicated rebuildable Room cache database (`soniq_media.db`, excluded from cloud backup rules) with exported schema v1 for `AudioTrackEntity` and `VideoItemEntity`.
  2. Define clean domain models (`AudioTrack`, `AlbumSummary`, `ArtistSummary`, `MediaFolder`, `VideoItem`) and repositories (`MusicLibraryRepository`, `VideoLibraryRepository`) keeping Music and Video logically separate.
  3. Implement production `MediaStoreAudioScanner` and `MediaStoreVideoScanner` querying `ContentResolver` on `Dispatchers.IO` with defensive null/zero-duration/missing-column handling, album-art content URIs (`content://media/external/audio/albumart/<albumId>`), and folder path extraction.
  4. Implement incremental/on-demand library sync coordinated with `MediaStoreChangeObserver` and exposed sync state (`Idle`, `Scanning`, `Error`).
  5. Upgrade `MusicScreen` (Songs, Albums, Artists, Folders tabs + pull/manual refresh + empty/error/loading states + sorting) and `VideoScreen` (Videos, Folders tabs + duration/resolution/size badges + Coil thumbnails + partial-access management banner).
- **Dependencies**: Stage 1 complete.
- **Acceptance Criteria**:
  - Real MediaStore queries (no fake data) populate Room cache and flow reactively to Compose UI.
  - Zero `INTERNET` permissions; artwork & video thumbnails load purely offline.
  - Music and Video remain independently permission-gated and logically decoupled.
- **Verification**: Room DAO tests, MediaStore cursor-mapping tests, Repository sync tests, ViewModel tests, Compose UI tests, `lintDebug` 0 warnings, `assembleDebug`, and emulator/device verification.

---

## Stage 3 — Audio and Video Playback Engine — **PLANNED**
- **Objectives**:
  1. Expand `PlaybackController` / `Media3PlaybackController` with queue loading (`playTracks`, `playVideo`), transport (`skipToNext`, `skipToPrevious`, `togglePlayPause`, `seekTo`), repeat/shuffle modes, real-time playback position updates, and dynamic `AudioAttributes` (`AUDIO_CONTENT_TYPE_MUSIC` vs `AUDIO_CONTENT_TYPE_MOVIE`).
  2. Implement `MiniPlayer` docked cleanly above the bottom navigation bar/rail when audio is active.
  3. Implement full-screen Audio `NowPlayingScreen` honoring all three persisted `PlayerLayout` options (`CLASSIC`, `ARTWORK_FOCUS`, `COMPACT`).
  4. Implement dedicated `VideoPlayerScreen` (Media3 `Player` surface integration in Compose, play/pause/seek/scrub controls, fullscreen immersive system bars, orientation support, resume position tracking).
- **Dependencies**: Stage 2 complete.
- **Acceptance Criteria**: Selecting any local track or video plays cleanly via `SoniqPlaybackService` with lifecycle independence across rotation and navigation.
- **Verification**: Controller/state unit tests, Compose UI tests for all `PlayerLayout` modes, emulator/device playback smoke verification.

---

## Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Bluetooth — **PLANNED**
- **Objectives**:
  1. Populate rich `MediaMetadata` (title, artist, album, local artwork URI) on every queued `MediaItem`.
  2. Configure `SoniqPlaybackService` custom notification actions, foreground service transitions, `POST_NOTIFICATIONS` graceful handling on API 33+, Bluetooth/headset media button commands, and audio focus ducking/pause behavior.
  3. Verify continuous background audio playback when navigating away or locking the screen, and automatic pause/stop behavior for video backgrounding per user preference.
- **Dependencies**: Stage 3 complete.
- **Verification**: Service lifecycle tests, MediaSession command tests, on-device/emulator notification & background verification.

---

## Stage 5 — Playlists, Favorites, Search, History & Queue Management — **PLANNED**
- **Objectives**:
  1. Extend persistent user database `soniq.db` (tested Room migration `v1 → v2` verified with `MigrationTestHelper`) with `PlaylistEntity`, `PlaylistTrackCrossRef`, `FavoriteMediaEntity`, `PlaybackHistoryEntity`, and `PlaybackResumeEntity`.
  2. Implement custom playlist creation, renaming, deletion, track addition/removal, and drag-to-reorder.
  3. Implement instant local offline search across songs, albums, artists, and videos.
  4. Implement editable Up Next queue sheet (reorder, remove, play next, add to queue).
- **Dependencies**: Stages 2 & 3 complete.
- **Verification**: Room `MigrationTestHelper` tests (`1 → 2`), DAO/Repository/ViewModel tests, UI tests.

---

## Stage 6 — Advanced Audio and Video Features — **PLANNED**
- **Objectives**:
  1. Audio: Playback speed & pitch controls, sleep timer, 5-band Equalizer & Bass Boost backed by Android `AudioEffect` attached to ExoPlayer's `audioSessionId`, gapless/crossfade options.
  2. Video: Aspect ratio / resize modes (Fit, Fill, Zoom), swipe gestures for brightness/volume/seek, local subtitle track selection (`.srt`/`.vtt`/embedded), audio track selection, Picture-in-Picture (PiP) support.
- **Dependencies**: Stages 3 & 4 complete.
- **Verification**: Unit tests for equalizer presets/sleep timer/subtitle selection, UI tests, device verification.

---

## Stage 7 — Complete Theme Customization and Personalization — **PLANNED**
- **Objectives**:
  1. Interactive custom ARGB/HSL accent color picker dialog in Settings (beyond the 8 curated swatches).
  2. Custom background image blur/scrim opacity tuning and graceful stale/deleted URI detection & recovery banner.
  3. Built-in curated starter presets alongside user presets, plus preset rename/overwrite support.
- **Dependencies**: Stage 1 complete (can be scheduled after Stage 5/6).
- **Verification**: Color contrast tests, preset CRUD tests, Compose UI tests.

---

## Stage 8 — Performance, Reliability, Privacy, Accessibility & Compatibility — **PLANNED**
- **Objectives**:
  1. Large-library profiling (10k+ tracks) with paging/indexed SQLite queries and Compose stability/recomposition audit.
  2. StrictMode verification, memory-leak checks, battery/wake-lock audit.
  3. Full TalkBack / contentDescription / touch-target (≥ 48dp) / large-font accessibility pass.
  4. Multi-API compatibility verification across API 26–37, foldable/tablet adaptive layouts, and R8 release build verification.
- **Dependencies**: Stages 2–7 complete.

---

## Stage 9 — Release Preparation, APK Generation & Final Verification — **PLANNED**
- **Objectives**:
  1. Externalized release signing template (`keystore.properties`, gitignored) without committing secrets.
  2. Final R8-minified release APK & AAB build, merged-manifest privacy audit, full regression suite, physical-device end-to-end QA checklist.
  3. Update `RELEASE_REPORT.md` and wait for **explicit user release approval**.
- **Dependencies**: Stage 8 complete.
