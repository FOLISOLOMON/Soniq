# SONIQ — Release Readiness Report

**Last Updated**: 2026-11-18 (End of Autonomous Session 1)  
**Release Readiness Status**: **NOT READY (Mid-Development — Stages 1–3 Complete; Stages 4–9 Pending)**  
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

## 2. APK Outputs & Installation Verification (Commit `41c8fbc`)

| Variant | Output Path | Verified Commit | Size | Install Verification |
|---|---|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | `41c8fbc` (2026-11-18) | 17.35 MB | Installed & launched cleanly on `Medium_Phone_API_37.0` emulator (`PID 4807`, 0 SONIQ crashes) |
| Debug AndroidTest | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `41c8fbc` (2026-11-18) | 1.25 MB | 1 / 1 passed on `Medium_Phone_API_37.0` emulator |
| Release (Unsigned) | `app/build/outputs/apk/release/app-release-unsigned.apk` | `41c8fbc` (2026-11-18) | 2.72 MB | R8 minified, resource shrunk, `lintVitalRelease` clean (not installed; unsigned) |

---

## 3. Permissions & Privacy / Security Verification

- **Verified Merged Release Manifest Permissions**:
  - `READ_MEDIA_AUDIO`
  - `READ_MEDIA_VIDEO`
  - `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+ partial visual access)
  - `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`)
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `WAKE_LOCK` (`C.WAKE_MODE_LOCAL`)
  - `com.soniq.player.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (AndroidX internal)
- **Privacy & Security Checks**:
  - `INTERNET` and `ACCESS_NETWORK_STATE` stripped via `tools:node="remove"` and verified absent in merged release manifest.
  - Zero advertisement SDKs, analytics trackers, or network image fetchers (`coil-compose` + `coil-video` operate strictly offline).
  - Rebuildable MediaStore cache (`soniq_media.db`) is excluded from Android backup rules; only user-authored settings (`appearance.preferences_pb`) and presets (`soniq.db`) are included.
  - Zero hardcoded secrets or signing credentials in Git.

---

## 4. Compatibility & Performance Checks

- **Compatibility**: Targets API 26–37; adaptive navigation supports compact phones and larger screens; verified on Android 17 emulator (`Medium_Phone_API_37.0`). Physical-device verification: **NOT VERIFIED** (`112173741O045288` currently `authorizing` in ADB).
- **Performance**: R8 release APK is 2.72 MB; position ticker runs only during active playback; Room media tables are indexed on `album_id`, `artist`, `folder_path`, `title`, and `date_added`. Large-library (10k+ track) profiling remains scheduled for Stage 8.

---

## 5. Critical Release Blockers

1. Remaining roadmap stages (Stages 4–8: notification & lock-screen QA, playlists/queue/resume, equalizer/subtitles/PiP, custom accent picker, large-library profiling & accessibility audit).
2. Physical-device end-to-end QA on authorized hardware.
3. User-supplied production release signing configuration.
4. Explicit user approval for distribution/publishing.
