# SONIQ — Private Release Signing, Vercel Hosting & Update Publishing Guide

This guide covers:
1. Creating and protecting your permanent Android release keystore.
2. Safe migration from existing debug installs on your phone.
3. Hosting SONIQ's private HTTPS update manifest and signed APK on **Vercel + Vercel Blob**.
4. Repeatable release publishing workflow.

---

## 1. Permanent Release Signing Setup (Required Prerequisite)

Android only allows in-place OTA updates when the new APK has the **exact same `applicationId` (`com.soniq.player`) and signing certificate** as the installed APK.

### Generate a Permanent Release Keystore (Once)

Run this locally outside the Git repository (or keep it out of commits — `*.jks`, `*.keystore`, and `keystore.properties` are gitignored):

```powershell
keytool -genkeypair -v `
  -keystore soniq-release.jks `
  -alias soniq-release `
  -keyalg RSA -keysize 4096 `
  -validity 10000
```

**Back up `soniq-release.jks` and its passwords securely** (e.g., encrypted password manager / offline backup). Losing the keystore prevents future in-place updates.

### Configure Gradle Signing Locally or via Environment Variables

Option A — Copy `keystore.properties.example` to `keystore.properties` at the repo root:
```properties
storeFile=C:/Users/Foli Solomon/SONIQ-Signing/soniq-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=soniq-release
keyPassword=YOUR_KEY_PASSWORD
```

Option B — Set environment variables:
- `SONIQ_KEYSTORE_FILE`
- `SONIQ_KEYSTORE_PASSWORD`
- `SONIQ_KEY_ALIAS`
- `SONIQ_KEY_PASSWORD`

### Safe Migration From Existing Debug Phone Installs
- Debug builds use `applicationIdSuffix = ".debug"` (`com.soniq.player.debug`) and the debug keystore.
- Signed release builds use `com.soniq.player` and your permanent release keystore.
- Because the package ID and signing keys differ, **never uninstall your existing debug app without first backing up or deciding you no longer need its local playlists/history**, and note that future release updates only update `com.soniq.player` -> `com.soniq.player`.

---

## 2. Hosting Architecture: Vercel + Vercel Blob

- **Manifest**: Static `manifest.json` served over HTTPS by Vercel with `Cache-Control: no-store, max-age=0` (see `update-server/vercel.json`).
- **APK Binary**: Signed release APK uploaded to **Vercel Blob** (HTTPS CDN).
- **App Configuration**: Pass your Vercel manifest URL at build time:
  ```powershell
  .\gradlew.bat assembleRelease `
    -Psoniq.versionCode=2 `
    -Psoniq.versionName=0.2.0 `
    -Psoniq.updateManifestUrl="https://your-soniq-updates.vercel.app/manifest.json"
  ```
  *(Or set `SONIQ_UPDATE_MANIFEST_URL` in your environment.)*

---

## 3. Repeatable Release Publishing Workflow

1. **Increment version**: Choose the next `soniq.versionCode` (e.g. `2`) and `soniq.versionName` (e.g. `0.2.0`).
2. **Build signed release APK**:
   `.\gradlew.bat assembleRelease -Psoniq.versionCode=2 -Psoniq.versionName=0.2.0 -Psoniq.updateManifestUrl="https://.../manifest.json"`
3. **Verify signing & package ID**:
   `& "$env:ANDROID_HOME\build-tools\37.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk`
4. **Calculate SHA-256 & exact byte size**:
   `Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256`
   `(Get-Item app\build\outputs\apk\release\app-release.apk).Length`
5. **Upload APK to Vercel Blob** (via Vercel Dashboard or `vercel blob put`) and copy its HTTPS URL.
6. **Update `manifest.json`** using `update-server/manifest.sample.json` as template (`schemaVersion: 1`, `versionCode`, `versionName`, `releaseNotes`, `apkUrl`, `apkSizeBytes`, lowercase `sha256`, `minSupportedVersionCode`, `mandatory`, `publishedAt`).
7. **Deploy `manifest.json` to Vercel** (`vercel --prod` inside your private update-server directory).
8. **Verify hosted artifacts**: Confirm `https://<your-domain>/manifest.json` returns valid JSON and the `apkUrl` returns HTTP 200 with matching SHA-256.
9. **Test in-app**: Open SONIQ → **Settings → About SONIQ → Check for updates**, download, verify, and confirm Android installation.
