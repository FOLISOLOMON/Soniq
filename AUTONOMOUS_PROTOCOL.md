# SONIQ — Autonomous Engineering Protocol

**Status**: Active Permanent Governance Document  
**Repository Root**: `D:\Projects\Soniq`  
**Last Updated**: 2026-11-20 (v0.3.1 OTA Verification Hardening)

---

## 1. Mission & Autonomy Boundaries

Autonomous engineering sessions exist to evolve **SONIQ** into a polished, reliable, production-quality, offline-first Android music and video player.

### Permitted Autonomous Actions
- Implementing planned features from `ROADMAP.md` in priority order.
- Refactoring architecture, schemas, UI components, and replacing unsuitable implementations when technically justified.
- Fixing bugs, resolving lint warnings, and optimizing performance and memory usage.
- Writing and executing unit, Robolectric, and instrumented tests.
- Building debug and release APKs for local verification and testing on authorized devices or emulators.
- Creating clean, meaningful Git checkpoint commits on the local repository.
- Updating all eight project-control Markdown files to reflect ground truth.

### Prohibited Actions Without Explicit User Approval
- Publishing, uploading, or distributing builds to Google Play, GitHub Releases, or any external channel.
- Replacing or generating production signing credentials or committing secrets (`*.keystore`, `*.jks`, `keystore.properties`, `secrets.properties`, `local.properties`).
- Introducing advertisements, ad SDKs, analytics/telemetry trackers, or unnecessary network dependencies (only user-initiated HTTPS update checks in Settings → About SONIQ may use `INTERNET` / `ACCESS_NETWORK_STATE`).
- Running irreversible remote Git operations (force pushes, history rewrites on shared branches, deleting user work).
- Claiming a build, test, or device check passed unless it was actually executed and verified in the current state.

---

## 2. Execution Budget & Session Lifecycle

- **Maximum execution budget**: **2 hours** per autonomous session.
- Sessions must stop safely before exhausting the two-hour budget, leaving the repository clean, compiling, passing tests, documented, and checkpointed in Git.
- Never start a subsequent autonomous session automatically after the two-hour budget concludes; report clearly and wait for the user's next instruction.

### Mandatory Execution Loop

Every meaningful unit of work MUST follow this exact cycle:

```text
PLAN → IMPLEMENT → BUILD → TEST → DEBUG → RETEST → REGRESSION CHECK → DOCUMENT → CHECKPOINT → REPORT
```

1. **Inspect**: Read all eight root Markdown control files and inspect affected source files before editing.
2. **Plan**: Choose the highest-priority unfinished objective in `ROADMAP.md` (or highest-severity open defect in `BUGS.md`).
3. **Implement**: Write production-quality Kotlin using centralized design tokens, null safety, structured concurrency, and lifecycle-aware state management. No fake or stub implementations.
4. **Build**: Run `./gradlew assembleDebug` (and `assembleRelease` / `assembleDebugAndroidTest` at stage milestones).
5. **Test**: Run `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `connectedDebugAndroidTest` when an authorized device or emulator is available.
6. **Debug & Retest**: Diagnose any compilation, test, or lint failure at the root cause; never remove functionality or weaken assertions just to make tests pass.
7. **Regression Check**: Confirm previously passing suites remain green and zero new lint warnings or permissions were introduced.
8. **Document**: Update `PROJECT_STATUS.md`, `ROADMAP.md`, `ARCHITECTURE.md`, `BUGS.md`, `TEST_REPORT.md`, `SESSION_LOG.md`, and `RELEASE_REPORT.md` consistently.
9. **Checkpoint**: Verify `.gitignore` and `git status`, then create a meaningful commit on `main`.
10. **Report**: Deliver a factual, verified summary of completed work, test results, device status, Git checkpoints, and remaining objectives.

---

## 3. Data, Privacy & Secret Protection

- Offline-first for all media playback, indexing, playlists, and themes; Coil configured without network fetchers; `INTERNET` and `ACCESS_NETWORK_STATE` are used exclusively for explicit user-initiated update checks and APK downloads in Settings → About SONIQ.
- Never commit `local.properties`, keystores, signing configs, or user media fixtures.
- Never use destructive Room migrations (`fallbackToDestructiveMigration`) for user-authored data (`soniq.db`). Export schemas and test migrations explicitly.
- Separate rebuildable local media index caches from cloud-backed-up user data.

---

## 4. Reporting Honesty Rules

- Distinguish explicitly between **Implemented**, **Partially Implemented**, **Planned**, and **NOT VERIFIED / UNKNOWN**.
- Never conflate APK compilation with runtime functional testing.
- Never claim physical-device verification unless commands actually executed on an authorized physical device.
- Never silently delete resolved bugs from `BUGS.md`; mark them `RESOLVED` with commit/session references.
