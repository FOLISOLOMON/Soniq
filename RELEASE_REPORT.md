# SONIQ — Release Readiness Report

**Last Updated**: 2026-11-18 (End of Autonomous Session 2)  
**Release Readiness Status**: **NOT READY (Mid-Development — Stages 1–5 Complete; Stages 6–9 Pending)**  
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

## 2. APK Outputs & Installation Verification (Commits `f265823` / `c5fabc0`)

| Variant | Output Path | Verified Commit | Size | Install Verification |
|---|---|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | `c5fabc0` (2026-11-18) | 18.04 MB | Installed & launched cleanly on `Medium_Phone_API_37.0` emulator (`PID 4611`, 0 SONIQ crashes) |
| Debug AndroidTest | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `c5fabc0` (2026-11-18) | 2.36 MB | **3 / 3 passed** on `Medium_Phone_API_37.0` emulator (`SoniqInstrumentedSmokeTest`, `SoniqPlaybackServiceInstrumentedTest`, `SoniqDatabaseMigrationTest`) |
| Release (Unsigned) | `app/build/outputs/apk/release/app-release-unsigned.apk` | `f265823` (2026-11-18; main app code unchanged in `c5fabc0`) | 2.85 MB | R8 minified, resource shrunk, `lintVitalRelease` clean (not installed; unsigned) |

---

## 3. Permissions & Privacy / Security Verification

- **Verified Merged Release Manifest Permissions**:
  - `READ_MEDIA_AUDIO`
  - `READ_MEDIA_VIDEO`
  - `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+ partial visual access)
  - `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`)
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `POST_NOTIFICATIONS` (optional on Android 13+ for foreground playback notification)
  - `WAKE_LOCK` (`C.WAKE_MODE_LOCAL`)
  - `com.soniq.player.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (AndroidX internal)
- **Privacy & Security Checks**:
  - `INTERNET` and `ACCESS_NETWORK_STATE` stripped via `tools:node="remove"` and verified absent in merged release manifest.
  - `SoniqOfflineBitmapLoader` explicitly rejects `http`/`https` artwork URIs with `SecurityException`, enforcing offline-only artwork decoding for notifications and lock-screen controls.
  - Zero advertisement SDKs, analytics trackers, or network image fetchers (`coil-compose` + `coil-video` operate strictly offline).
  - Rebuildable MediaStore cache (`soniq_media.db`) and device-local playback queue snapshot (`playback_state.preferences_pb`) are excluded from Android backup rules; user-authored `appearance.preferences_pb` and `soniq.db` (v2: theme presets, playlists, favorites, history, resume) are preserved cleanly across non-destructive Room migrations (`MIGRATION_1_2`).
  - Zero hardcoded secrets or signing credentials in Git.

---

## 4. Compatibility & Performance Checks

- **Compatibility**: Targets API 26–37; adaptive navigation supports compact phones and larger screens; verified on Android 17 emulator (`Medium_Phone_API_37.0`).
- **Physical Device & Hardware Status**: **NOT VERIFIED** — `Infinix_X6528` (`112173741O045288`, Android 13) timed out during `adb install` waiting for Infinix XOS interactive USB-install approval (`DEBT-005`). Real Bluetooth/headset button hardware and screen-off physical audio playback remain pending physical-device QA.
- **Performance**: R8 release APK is 2.85 MB; position ticker runs only during active playback; artwork decoded via `SoniqOfflineBitmapLoader` is downsampled to max `512px`; playback history is bounded and pruned automatically; Room tables are indexed. Large-library (10k+ track) profiling remains scheduled for Stage 8.

---

## 5. Critical Release Blockers

1. Remaining roadmap stages (Stages 6–8: equalizer/sleep timer/subtitles/PiP, custom accent picker & broken-URI recovery, large-library profiling & accessibility audit).
2. Physical-device end-to-end QA on authorized hardware (including real Bluetooth/headset media buttons, lock-screen controls, and real audio/video decoding).
3. User-supplied production release signing configuration.
4. Explicit user approval for distribution/publishing.
