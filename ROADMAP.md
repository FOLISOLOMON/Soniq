# SONIQ — Development Roadmap

**Last Updated**: 2026-11-20 (Private Update System)  
**Active Stage**: Stages 1–9 **COMPLETED** + Liquid Glass Redesign **COMPLETED** + **Private User-Initiated Update System IMPLEMENTED (`ffcbc12`)** → Next: **User Keystore Generation, Vercel + Vercel Blob Deployment, Physical-Device Verification (`Infinix_X6528`) & Release Approval**  

---

## Stage 1 — Foundation and Project Hardening — **COMPLETED** (`2b8937f`, `46be15f`)
## Stage 2 — Local Media Discovery and Library Management — **COMPLETED** (`53cbae1`)
## Stage 3 — Audio and Video Playback Engine — **COMPLETED** (`41c8fbc`)
## Stage 4 — Background Playback, MediaSession, Notification, Lock-Screen & Bluetooth — **COMPLETED** (`057fb56`, `c5fabc0`, `13983a8`)
## Stage 5 — Playlists, Favorites, Search, History & Queue Management — **COMPLETED** (`f265823`)
## Stage 6 — Advanced Audio and Video Features — **COMPLETED (Core Scope)** (`13983a8`, `ae90175`)
- Implemented: Hardware `Equalizer` & `BassBoost` with capability detection & unsupported-state UI, Gapless + configurable Crossfade, Fit/Fill/Zoom aspect modes, embedded & external `.srt`/`.vtt` subtitles + cue rendering, audio track selection, speed controls, immersive orientation toggle, brightness/volume/seek gestures, reliable resume.
- **Deferred (Optional)**: Audio **Sleep Timer** and Android **Picture-in-Picture (PiP)** mode.

---

## Stage 7 — Complete Theme Customization and Personalization — **COMPLETED** (`3f1fd87`, `c2b9264`)
- **Delivered**:
  1. Custom HSL accent color picker dialog with `#RRGGBB` readout & live WCAG contrast preview (`DEBT-003`).
  2. Broken SAF background-image URI detection & recovery banner (`DEBT-004`).
  3. Background image scrim opacity (`35%–95%`) & Android 12+ blur radius (`0–24 dp`) controls (`soniq.db` `v2 → v3` via tested `MIGRATION_2_3`).
  4. Theme preset Rename & Duplicate + 4 curated non-destructive starter presets (`SoniqStarterPresets`).

---

## Stage 8 — Performance, Reliability, Privacy, Accessibility & Compatibility — **COMPLETED** (`c2b9264`)
- **Delivered**:
  1. **Large-Library Performance**: Moved `DefaultMusicLibraryRepository` & `DefaultVideoLibraryRepository` sync/mapping/grouping off the Main thread (`.flowOn(Dispatchers.Default)`, `withContext(Dispatchers.IO)`), added periodic `ensureActive()` scan cancellation checks in `MediaStoreAudioScanner` / `MediaStoreVideoScanner`, and verified a **10,000-track** sync & aggregation stress test.
  2. **Accessibility**: Enforced `>= 48dp` accent swatch touch targets (`SoniqSizes.accentSwatch = 48.dp`) and added `heading()` semantics to `SoniqSectionHeader` and modal sheet titles (`AudioEffectsSheet`, `Up Next`, `Audio & Subtitles`).
  3. **Compatibility**: Migrated `SoniqApp` to `currentWindowAdaptiveInfoV2()` (`DEBT-008`), achieving **0 Kotlin compiler warnings** and **0 Lint warnings**.

---

## Stage 9 — Release Preparation, APK/AAB Generation & Deferred Features — **COMPLETED (Engineering)** (`c1c710d`)
- **Delivered**:
  1. Implemented service-coordinated **Sleep Timer** (`SoniqSleepTimerManager`, live countdown, `Off` / `15m` / `30m` / `45m` / `60m` chips in `AudioEffectsSheet`).
  2. Implemented Android **Picture-in-Picture (PiP)** support (`android:supportsPictureInPicture="true"`, `PictureInPictureParams` with `setSourceRectHint` and API 31+ `setAutoEnterEnabled`, PiP button & control hiding in `VideoPlayerScreen`, `onUserLeaveHint` auto-PiP in `MainActivity`).
  3. Fixed narrow-screen video top-bar button overflow (`PlayerScreensUiTest` regression caught & fixed by moving secondary video actions to a scrollable second row).
  4. Added externalized release signing template (`keystore.properties.example`, gitignored `keystore.properties` wiring in `app/build.gradle.kts`).
  5. Built R8-minified, resource-shrunk `app-release-unsigned.apk` (`2.92 MB`) and `app-release.aab` (`7.10 MB`), verified `56/56` unit/Robolectric tests & `0` code/manifest Lint warnings.
- **Pending External Actions Before Release Approval**:
  1. Physical-device (`Infinix_X6528`) & Bluetooth/headset end-to-end QA + emulator instrumented re-run once host hypervisor acceleration or XOS USB-install approval is active.
  2. User-supplied production `keystore.properties` and **explicit user release approval**.

---

## UI/UX Visual Redesign Phase 2 — **COMPLETED** (`522a407`, `473241e`)
- **Delivered**:
  1. **Scroll & Contrast Hardening (`522a407`)**: Fixed unreadable dark section headings on Home (`LocalContentColor` in `SoniqBackground` + explicit `onBackground`), and cached gradient `Brush`es via `drawWithCache` / `remember` for smooth scrolling.
  2. **Shared Glass Primitives**: Added `SoniqGlassModalSheet` & `SoniqGlassDialog`, upgraded `SoniqSegmentedChoice`, `SoniqEmptyState`, and `SoniqSectionHeader`.
  3. **Home Quick Access Deep Linking**: Wired `pendingMusicTab` in `SoniqApp` and `requestedTab` in `MusicRoute` so every Home category bubble opens its exact `MusicLibraryTab`.
  4. **Screens & Overlays**: Redesigned `VideoPlayerScreen` (auto-hiding minimal glass top/bottom panels, edge scrims, `SoniqGlassModalSheet` tracks sheet), `AudioEffectsSheet`, `SettingsScreen` (plus Privacy & Offline Guarantee card), and `MusicScreen` / `NowPlayingScreen` dialogs & sheets.
- **Private User-Initiated Update System — COMPLETED (`ffcbc12`)**: Implemented `core/update/` (`UpdateModels`, `HttpsUpdateRemoteDataSource`, `AndroidApkVerifier` SHA-256 + package ID + signing-cert verification, `AndroidApkInstaller` via `FileProvider`), `UpdateViewModel`, Settings → **About SONIQ** UI, `BuildConfig.UPDATE_MANIFEST_URL` + env-var/properties signing in `app/build.gradle.kts`, and Vercel + Vercel Blob hosting templates (`update-server/`, `docs/UPDATE_HOSTING_AND_RELEASES.md`).
- **Next (User Actions & First OTA Gate)**: With explicit user approval, upload signed `app-release.apk` (v0.3.0, `versionCode=3`) to Vercel Blob, publish `update-server/manifest.json` to `soniq-update-server` on Vercel, and test end-to-end OTA delivery from installed v0.2.0 (`versionCode=2`) via **Settings → About SONIQ → Check for updates**.
