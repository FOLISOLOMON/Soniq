# SONIQ — Release Readiness Report

**Last Updated**: 2026-11-18 (End of Autonomous Session 3)  
**Release Readiness Status**: **NOT READY (Stages 1–6 Complete + Stage 7 Foundation Complete; Stages 7–9 Remaining)**  
**User Release Approval Status**: **UNAPPROVED** *(Release remains unapproved until explicitly approved by the user.)*

---

## 1. Application Identity & Build Configuration

| Property | Value |
|---|---|
| Product Name | `SONIQ` |
| Namespace | `com.soniq.player` |
| Application ID (Release) | `com.soniq.player` (configurable via `-Psoniq.applicationId=...`) |
| Application ID (Debug) | `com.soniq.player.debug` |
| Version Code / Name | `1` / `0.1.0` (`0.1.0-debug` for debug) |
| SDK Levels | `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37` |
| Release Optimization | `isMinifyEnabled = true`, `isShrinkResources = true`, `app/proguard-rules.pro` |
| Signing | Debug keystore only for debug builds; release APK currently **unsigned** (no production keystore configured or committed) |

---

## 2. APK Outputs & Installation Verification (Commit `3f1fd87`)

| Variant | Output Path | Verified Commit | Size | Install Verification |
|---|---|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | `3f1fd87` (2026-11-18) | 18.06 MB | Installed & launched cleanly on `Medium_Phone_API_37.0` emulator (`PID 4321`, 0 SONIQ crashes) |
| Debug AndroidTest | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `3f1fd87` (2026-11-18) | 2.36 MB | **3 / 3 passed** on `Medium_Phone_API_37.0` emulator |
| Release (Unsigned) | `app/build/outputs/apk/release/app-release-unsigned.apk` | `3f1fd87` (2026-11-18) | 2.90 MB | R8 minified, resource shrunk, `lintVitalRelease` clean (not installed; unsigned) |

---

## 3. Permissions & Privacy / Security Verification

- **Verified Merged Release Manifest Permissions**:
  - `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`)
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `POST_NOTIFICATIONS` (optional on Android 13+)
  - `WAKE_LOCK` (`C.WAKE_MODE_LOCAL`)
  - `com.soniq.player.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
- **Privacy & Security Checks**:
  - `INTERNET` and `ACCESS_NETWORK_STATE` stripped via `tools:node="remove"` and verified absent in merged release manifest.
  - `SoniqOfflineBitmapLoader` rejects `http`/`https` artwork URIs with `SecurityException`.
  - Zero advertisement SDKs, analytics trackers, or network image fetchers.
  - Rebuildable cache (`soniq_media.db`) and device-local playback snapshot (`playback_state.preferences_pb`) excluded from backup; user-authored `appearance.preferences_pb` and `soniq.db` (v2) preserved cleanly across non-destructive Room migrations (`MIGRATION_1_2`).

---

## 4. Compatibility & Performance Checks

- **Compatibility**: Targets API 26–37; adaptive navigation; verified on Android 17 emulator (`Medium_Phone_API_37.0`).
- **Physical Device & Hardware Status**: **NOT VERIFIED** — `Infinix_X6528` (`112173741O045288`) disconnected in Session 3 (`DEBT-005`). Real Bluetooth/headset media buttons, lock-screen hardware controls, and physical audio/video decoding remain pending physical-device QA.
- **Performance**: R8 release APK is `2.90 MB`; position ticker and crossfade monitor run only during active playback; `SoniqOfflineBitmapLoader` caps artwork at `512px`. Large-library (10k+ track) profiling scheduled for Stage 8.

---

## 5. Critical Release Blockers

1. Remaining roadmap stages (Stage 7 starter presets/scrim, Stage 8 large-library profiling & accessibility audit).
2. Physical-device end-to-end QA on authorized hardware (including real Bluetooth/headset media buttons, lock-screen controls, and hardware EQ/video playback).
3. User-supplied production release signing configuration.
4. Explicit user approval for distribution/publishing.
