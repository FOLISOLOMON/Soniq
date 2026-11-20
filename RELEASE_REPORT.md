# SONIQ — Release Readiness Report

**Last Updated**: 2026-11-20 (First Production OTA Published v0.3.0)  
**Release Readiness Status**: **v0.3.0 OTA PUBLISHED TO VERCEL BLOB & VERCEL PRODUCTION (`https://soniq-update-server.vercel.app/manifest.json`)**  
**User Release Approval Status**: **APPROVED FOR v0.3.0 OTA PUBLICATION (2026-11-20)**

---

## 1. Application Identity & Build Configuration

| Property | Value |
|---|---|
| Product Name | `SONIQ` |
| Namespace | `com.soniq.player` |
| Application ID (Release) | `com.soniq.player` (configurable via `-Psoniq.applicationId=...`) |
| Application ID (Debug) | `com.soniq.player.debug` |
| Version Code / Name | `3` / `0.3.0` (`0.3.0-debug` for debug) |
| SDK Levels | `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37` |
| Release Optimization | `isMinifyEnabled = true`, `isShrinkResources = true`, `app/proguard-rules.pro` |
| Signing | Debug keystore for debug builds; permanent RSA-4096 release keystore (`soniq-release`, SHA-256 `e49a06da7188a9ab5ec432883a0a0d15d0659d7fcd9fd82c762d13c82efe2d95`) configured via gitignored `keystore.properties` |

---

## 2. APK & Bundle Outputs (First OTA Release v0.3.0, `versionCode = 3`)

| Variant | Output Path | Version | Size & SHA-256 | Verification |
|---|---|---|---|---|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | `0.3.0-debug` (`3`) | 17.91 MB (`18,776,667` B) | Built & verified against **67 / 67** unit/Robolectric tests |
| Debug AndroidTest | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | — | — | **3 / 3 passed** on `emulator-5554` (`Small_Phone(AVD)`, Android 17) |
| Release APK (Signed) | `app/build/outputs/apk/release/app-release.apk` | `0.3.0` (`3`) | 2.98 MB (`3,126,238` B) / SHA-256 `f2d1e7aa99dd1de768cc780c8dd19be8efce250ed248cb93c87a388ee4dec089` | R8 minified, v2 RSA-4096 signed (`e49a06da...`), embedded `https://soniq-update-server.vercel.app/manifest.json`, uploaded to `https://rehezvrea6xtcs58.public.blob.vercel-storage.com/releases/soniq-v0.3.0.apk` |

---

## 3. Permissions & Privacy / Security Verification

- **Verified Merged Release Manifest Permissions**: `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `INTERNET`, `ACCESS_NETWORK_STATE`, `REQUEST_INSTALL_PACKAGES`, `com.soniq.player.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- **Privacy & Security Checks**:
  - `INTERNET` and `ACCESS_NETWORK_STATE` are used exclusively for explicit user-initiated HTTPS update checks and APK downloads in **Settings → About SONIQ** (zero background checks, zero media uploads).
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
