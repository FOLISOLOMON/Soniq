# SONIQ — Release Readiness Report

**Last Updated**: 2026-11-18  
**Release Readiness Status**: **NOT READY (Early Development — Stage 1 Complete, Core Playback & Library Pending)**  
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

## 2. APK Outputs & Installation Verification

| Variant | Output Path | Last Verified Build | Size | Install Verification |
|---|---|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | `46be15f` (2026-11-18) | ~17.0 MB | Installed & launched cleanly on `Medium_Phone_API_37.0` emulator (`PID 7792`, 0 crashes) |
| Debug AndroidTest | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `46be15f` (2026-11-18) | ~1.25 MB | 1 / 1 passed on `Medium_Phone_API_37.0` emulator |
| Release (Unsigned) | `app/build/outputs/apk/release/app-release-unsigned.apk` | `2b8937f` (2026-11-18) | 2.45 MB | Not installed (unsigned; re-build required after Stage 2 changes) |

---

## 3. Permissions & Privacy / Security Verification

- **Declared Runtime & Service Permissions**:
  - `READ_MEDIA_AUDIO`
  - `READ_MEDIA_VIDEO`
  - `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+ partial visual access)
  - `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`)
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `WAKE_LOCK` (`C.WAKE_MODE_LOCAL`)
- **Privacy Checks**:
  - `INTERNET` and `ACCESS_NETWORK_STATE` stripped via `tools:node="remove"`; verified absent from merged release manifest.
  - Zero advertisement SDKs, analytics trackers, or network image fetchers.
  - Zero hardcoded secrets or keystores in Git.

---

## 4. Compatibility & Performance Checks

- **Compatibility**: Targets API 26–37; adaptive navigation supports compact phones and larger screens; verified on Android 17 emulator (`Medium_Phone_API_37.0`). Physical-device verification: **NOT VERIFIED** (`112173741O045288` currently `authorizing` in ADB).
- **Performance**: Cold start splash synchronization verified; large-library and playback profiling: **NOT VERIFIED** (scheduled for Stage 8).

---

## 5. Critical Release Blockers

1. Core product features not yet implemented (Stages 2–7: local media indexing, audio/video playback, notifications, playlists, advanced controls).
2. Physical-device QA not yet performed.
3. Production signing configuration not yet provided by the user.
4. Explicit user release approval not yet granted.
