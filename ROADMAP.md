# SONIQ — Development Roadmap

**Last Updated**: 2026-11-18 (End of Autonomous Session 4)  
**Active Stage**: Stages 1–8 **COMPLETED** → Next: **Stage 9 — Release Preparation, Physical-Device QA & Optional Deferred Features (Sleep Timer / PiP)**  
*(Note: Visual UI/UX redesign is explicitly frozen until all planned engineering stages are complete.)*

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

## Stage 9 — Release Preparation, Physical-Device QA & Optional Deferred Features — **NEXT**
- **Objectives**:
  1. Optional deferred engineering items if requested: Audio Sleep Timer & Video Picture-in-Picture (PiP).
  2. Physical-device (`Infinix_X6528`) & Bluetooth/headset end-to-end QA checklist execution.
  3. Externalized release signing template (`keystore.properties`, gitignored) without committing secrets.
  4. Final R8-minified release APK & AAB build, merged-manifest privacy audit, full regression suite, and waiting for **explicit user release approval**.
- **Post-Stage 9**: Dedicated **UI/UX Visual Redesign Phase**.
