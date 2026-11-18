# SONIQ — Development Roadmap

**Last Updated**: 2026-11-18 (End of Autonomous Session 1)  
**Active Stage**: Stages 1, 2, and 3 **COMPLETED** → Next: **Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Bluetooth**

---

## Stage 1 — Foundation and Project Hardening — **COMPLETED** (`2b8937f`, `46be15f`)
- **1.1 Project Initialization & Architecture**: Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, KSP 2.3.14, Compose BOM `2026.09.00`, Media3 1.11.1, Room 2.8.5, DataStore 1.3.0, Hilt 2.60.1, Coil 3.6.3, adaptive navigation shell, tokenized design system, theme presets, offline privacy enforcement.
- **1.1b Hardening Pass**: Android 14+ `READ_MEDIA_VISUAL_USER_SELECTED`, `Media3PlaybackController` disconnect/reconnect lifecycle, eager `MainViewModel` splash gate, zero hardcoded visual constants, explicit Kotlin JVM 17 target, 0 Lint warnings.

---

## Stage 2 — Local Media Discovery and Library Management — **COMPLETED** (`53cbae1`)
- **Delivered**:
  1. Dedicated non-backed-up Room cache database `SoniqMediaDatabase` (`soniq_media.db`, exported schema v1) with indexed `AudioTrackEntity` and `VideoItemEntity` plus atomic chunked `syncWith` DAOs (resolving `DEBT-001`).
  2. Pure domain models (`AudioTrack`, `VideoItem`, `AlbumSummary`, `ArtistSummary`, `MediaFolderSummary`, sort enums, `LibrarySyncState`), `MediaFormatters`, and injectable `@IoDispatcher`.
  3. Production `MediaStoreAudioScanner` and `MediaStoreVideoScanner` (API 26–37 `RELATIVE_PATH`/`DATA` handling, album art URIs, `<unknown>` normalization, zero-duration filtering) + `DefaultMusicLibraryRepository` & `DefaultVideoLibraryRepository` with `Mutex`-guarded sync and debounced `MediaStoreChangeObserver` auto-resync.
  4. Full `MusicScreen` (Songs/Albums/Artists/Folders tabs, drill-down filters, sorting, offline search, refresh, error retry) and `VideoScreen` (Videos/Folders tabs, offline `coil-video` thumbnails, duration/resolution/size metadata, Android 14+ partial-access banner).
- **Verified**: 33/33 unit & Robolectric tests, 0 Lint warnings, clean debug build.

---

## Stage 3 — Audio and Video Playback Engine — **COMPLETED** (`41c8fbc`)
- **Delivered**:
  1. `MediaItemFactory` converting domain `AudioTrack` / `VideoItem` to rich Media3 `MediaItem` with full `MediaMetadata` and explicit media kind extras.
  2. Dynamic `AudioAttributes` switching (`C.AUDIO_CONTENT_TYPE_MUSIC` vs `C.AUDIO_CONTENT_TYPE_MOVIE`) inside `SoniqPlaybackService` (resolving `DEBT-002`).
  3. Upgraded `Media3PlaybackController` with cold-start command queuing, active-only 250ms position ticker, queue/transport/seek/repeat/shuffle/speed controls, and `player: StateFlow<Player?>` surface handle.
  4. `PlaybackViewModel`, docked audio `MiniPlayerBar`, full-screen `NowPlayingScreen` rendering all three persisted `PlayerLayout` variants (`CLASSIC`, `ARTWORK_FOCUS`, `COMPACT`), and dedicated full-screen `VideoPlayerScreen` using Media3 Compose `PlayerSurface` (`media3-ui-compose:1.11.1`).
  5. Full navigation wiring (`SoniqRoute.AudioPlayer`, `SoniqRoute.VideoPlayer`, auto-hiding bottom nav chrome on full-screen player screens, tapping any song or video starts playback immediately).
- **Verified**: 39/39 unit & Robolectric tests, 0 Lint warnings, R8 release build PASS (2.72 MB unsigned), 1/1 emulator instrumented test + launch check PASS on `Medium_Phone_API_37.0`.

---

## Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Bluetooth — **NEXT**
- **Objectives**:
  1. Configure `DefaultMediaNotificationProvider` / custom `MediaSession.Callback` in `SoniqPlaybackService` for clean notification channel branding, compact-view transport buttons, and artwork rendering.
  2. Add graceful optional `POST_NOTIFICATIONS` permission flow on Android 13+ (without blocking playback if declined).
  3. Verify Bluetooth/headset media button commands, audio focus ducking/pause, noisy audio pause, and continuous screen-off background audio playback.
  4. Add optional user setting for background video audio continuation vs auto-pause on exit.
- **Dependencies**: Stage 3 complete.
- **Verification**: Service & MediaSession callback tests, notification/permission tests, emulator & physical-device background playback verification.

---

## Stage 5 — Playlists, Favorites, Search, History & Queue Management — **PLANNED**
- **Objectives**:
  1. Extend persistent user database `soniq.db` (tested Room migration `v1 → v2` verified with `MigrationTestHelper`) with `PlaylistEntity`, `PlaylistTrackCrossRef`, `FavoriteMediaEntity`, `PlaybackHistoryEntity`, and `PlaybackResumeEntity`.
  2. Implement custom playlist CRUD, track ordering, Favorites view, and automatic video/long-audio resume position tracking.
  3. Implement editable Up Next queue sheet (reorder, remove, play next, add to queue).
- **Dependencies**: Stages 2 & 3 complete.

---

## Stage 6 — Advanced Audio and Video Features — **PLANNED**
- **Objectives**:
  1. Audio: Sleep timer, 5-band Equalizer & Bass Boost backed by Android `AudioEffect` attached to ExoPlayer's `audioSessionId`, gapless/crossfade options.
  2. Video: Aspect ratio / resize modes (Fit, Fill, Zoom), swipe gestures for brightness/volume/seek, local subtitle track selection (`.srt`/`.vtt`/embedded), audio track selection, Picture-in-Picture (PiP) support.
- **Dependencies**: Stages 3 & 4 complete.

---

## Stage 7 — Complete Theme Customization and Personalization — **PLANNED**
- **Objectives**:
  1. Interactive custom ARGB/HSL accent color picker dialog in Settings (beyond the 8 curated swatches).
  2. Custom background image scrim/blur controls and stale/revoked URI detection & recovery banner.
  3. Curated built-in starter theme presets plus preset rename/overwrite support.

---

## Stage 8 — Performance, Reliability, Privacy, Accessibility & Compatibility — **PLANNED**
- **Objectives**:
  1. Large-library profiling (10k+ tracks), Compose stability/recomposition audit, StrictMode & memory leak checks.
  2. Full TalkBack / contentDescription / touch-target (≥ 48dp) / large-font accessibility pass.
  3. Multi-API compatibility verification across API 26–37, foldable/tablet adaptive layouts, and R8 release audit.

---

## Stage 9 — Release Preparation, APK Generation & Final Verification — **PLANNED**
- **Objectives**:
  1. Externalized release signing template (`keystore.properties`, gitignored) without committing secrets.
  2. Final R8-minified release APK & AAB build, merged-manifest privacy audit, full regression suite, physical-device end-to-end QA checklist.
  3. Update `RELEASE_REPORT.md` and wait for **explicit user release approval**.
