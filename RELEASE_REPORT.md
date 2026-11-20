# SONIQ — Release Readiness Report

**Last Updated**: 2026-11-20 (End of Session 6)  
**Release Readiness Status**: **UI/UX REDESIGN PHASE 1 ARTIFACTS GENERATED (Pending Phase 2 Polish, Physical-Device QA & Production Signing)**  
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
| Signing | Debug keystore only for debug builds; optional `keystore.properties` template (`keystore.properties.example`, real `keystore.properties` gitignored) — release artifacts currently **unsigned** |

---

## 2. APK & Bundle Outputs (Commit `47dc1a3`)

| Variant | Output Path | Verified Commit | Size | Install Verification |
|---|---|---|---|---|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | `47dc1a3` (2026-11-20) | 18.07 MB | Built & verified against 57/57 unit/Robolectric tests; `adb install` on `Infinix_X6528` blocked by XOS USB-install restriction (`DEBT-005`) |
| Debug AndroidTest | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `47dc1a3` (2026-11-20) | — | Compiled cleanly; **3 / 3 passed** at `c2b9264` on `Medium_Phone_API_37.0` |
| Release APK (Unsigned) | `app/build/outputs/apk/release/app-release-unsigned.apk` | `47dc1a3` (2026-11-20) | 2.91 MB | R8 minified, resource shrunk, `lintVitalRelease` clean (not installed; unsigned) |

---

## 3. Permissions & Privacy / Security Verification

- **Verified Merged Release Manifest Permissions**: `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `com.soniq.player.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- **Privacy & Security Checks**:
  - `INTERNET` and `ACCESS_NETWORK_STATE` stripped via `tools:node="remove"` and verified absent in merged release manifest.
  - `SoniqOfflineBitmapLoader` rejects `http`/`https` artwork URIs with `SecurityException`.
  - Zero advertisement SDKs, analytics trackers, or network image fetchers.
  - Rebuildable cache (`soniq_media.db`) and device-local playback snapshot (`playback_state.preferences_pb`) excluded from backup; user-authored `appearance.preferences_pb` and `soniq.db` (v3) preserved cleanly across non-destructive Room migrations (`MIGRATION_1_2`, `MIGRATION_2_3`).

---

## 4. Compatibility & Performance Checks

- **Compatibility**: Targets API 26–37; `currentWindowAdaptiveInfoV2()` with **0 compiler warnings**; verified on Android 17 emulator (`Medium_Phone_API_37.0`).
- **Performance**: R8 release APK is `2.92 MB`; 10k+ track aggregation runs off the Main thread (`.flowOn(Dispatchers.Default)`) and passed 10,000-track stress testing; MediaStore scanners support immediate cooperative cancellation.
- **Physical Device & Hardware Status**: **NOT VERIFIED BY AUTOMATED ADB RUN** (`Infinix_X6528` not attached in Session 4; `DEBT-005`).

---

## 5. Critical Release Blockers

1. Stage 9 release preparation (physical-device & Bluetooth/headset end-to-end QA, optional deferred Sleep Timer / PiP if requested).
2. User-supplied production release signing configuration.
3. Dedicated post-engineering UI/UX visual redesign phase (per user decision).
4. Explicit user approval for distribution/publishing.
