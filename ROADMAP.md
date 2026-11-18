# SONIQ — Development Roadmap

**Last Updated**: 2026-11-18 (End of Autonomous Session 2)  
**Active Stage**: Stages 1, 2, 3, 4, and 5 **COMPLETED** → Next: **Stage 6 — Advanced Audio and Video Features**

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
  5. Full navigation wiring (`SoniqRoute.AudioPlayer`, `SoniqRoute.VideoPlayer`, auto-hiding bottom nav chrome on full-screen player screens).
- **Verified**: 39/39 unit & Robolectric tests, 0 Lint warnings, R8 release build PASS (2.72 MB unsigned), 1/1 emulator instrumented test + launch check PASS on `Medium_Phone_API_37.0`.

---

## Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Bluetooth — **COMPLETED** (`057fb56`, `c5fabc0`)
- **Delivered**:
  1. Custom `DefaultMediaNotificationProvider` (`soniq_playback_channel`, `ic_music_note`), strictly offline `SoniqOfflineBitmapLoader` (rejecting `http`/`https`, downsampling local/embedded art to `512px`), and custom `MediaSession.Callback` (`COMMAND_TOGGLE_SHUFFLE`, `COMMAND_CYCLE_REPEAT` in `SLOT_OVERFLOW`, preserving compact Prev/Play-Pause/Next slots; resolving `DEBT-007`).
  2. Declared `POST_NOTIFICATIONS` + exported `androidx.media3.session.MediaButtonReceiver` (`MEDIA_BUTTON`) in `AndroidManifest.xml`, configured audio focus (`handleAudioFocus = true`), headphone disconnection pause (`handleAudioBecomingNoisy = true`), and `WAKE_MODE_LOCAL`.
  3. Implemented `@PlaybackDataStore` (`playback_state.preferences_pb`, excluded from backup) + `PlaybackPreferencesDataSource` + `PlaybackRestorationManager` for `MediaSession.Callback.onPlaybackResumption` and cold-start paused session restoration in `Media3PlaybackController`.
  4. Hardened `SoniqPlaybackService.onTaskRemoved` (keeps active audio alive, stops idle/paused sessions) and leak-free `onDestroy`.
  5. Added non-blocking one-time Android 13+ `POST_NOTIFICATIONS` prompt on playback start plus Settings "Playback" card (background video audio continuation toggle + notification permission Allow button).
- **Verified**: 45/45 unit & Robolectric tests at `057fb56` (49/49 after Stage 5), 0 Lint warnings, `SoniqPlaybackServiceInstrumentedTest` PASS on `Medium_Phone_API_37.0` emulator. *(Physical device & physical Bluetooth hardware testing remain pending external XOS USB-install approval.)*

---

## Stage 5 — Playlists, Favorites, Search, History & Queue Management — **COMPLETED** (`f265823`)
- **Delivered**:
  1. Extended persistent user database `soniq.db` from `v1` to `v2` via explicit non-destructive `SoniqDatabaseMigrations.MIGRATION_1_2` (exported schema `app/schemas/com.soniq.player.core.database.SoniqDatabase/2.json`), adding `playlists`, `playlist_tracks` (FK cascade), `favorite_media`, `playback_history`, and `playback_resume` while preserving existing user `theme_presets`.
  2. Implemented `UserLibraryRepository` / `DefaultUserLibraryRepository` (playlist CRUD & validation, ordered track membership, favorites toggle, auto-pruned playback history, resume position persistence with `<5s` / `>=95%` auto-clear).
  3. Extended `PlaybackController` / `Media3PlaybackController` / `PlaybackViewModel` with `QueueItem`, `playNext`, `addToQueue`, `skipToQueueIndex`, `removeQueueItem`, `moveQueueItem`, `playVideo(video, startPositionMs)`, automatic playback history logging, and automatic resume save/restore.
  4. Added `PLAYLISTS` and `FAVORITES` tabs, playlist create/rename/delete/drill-down, add-to-playlist dialog, and track overflow actions in `MusicScreen`; Favorite toggle & Up Next `ModalBottomSheet` queue editor in `NowPlayingScreen`; and saved resume progress bars on `VideoScreen` thumbnails.
- **Verified**: 49/49 unit & Robolectric tests (including `SoniqDatabaseMigrationUnitTest` & `DefaultUserLibraryRepositoryTest`), 0 Lint warnings, R8 release build PASS (2.85 MB unsigned), and 3/3 emulator instrumented tests PASS on `Medium_Phone_API_37.0` (including official Room `MigrationTestHelper` in `SoniqDatabaseMigrationTest`).

---

## Stage 6 — Advanced Audio and Video Features — **NEXT**
- **Objectives**:
  1. Audio: Sleep timer, 5-band Equalizer & Bass Boost backed by Android `AudioEffect` attached to ExoPlayer's `audioSessionId`, gapless/crossfade options.
  2. Video: Aspect ratio / resize modes (Fit, Fill, Zoom), swipe gestures for brightness/volume/seek, local subtitle track selection (`.srt`/`.vtt`/embedded), audio track selection, Picture-in-Picture (PiP) support.
- **Dependencies**: Stages 3, 4 & 5 complete.

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
