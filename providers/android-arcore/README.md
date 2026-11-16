# JSE Android ARCore Provider

Standalone Android library for the Jezaku Spatial Engine (JSE). This module is independent of the Jezaku application, Flutter, and the Rust workspace.

## Current implementation status

Implemented in this increment:
- Vendor-neutral Kotlin provider/session contracts and normalized DTOs.
- Typed provider error categories.
- ARCore runtime availability mapping.
- User-mediated ARCore install/update request through a foreground Activity.
- ARCore session construction and horizontal/vertical plane configuration.
- Session resume/pause/update/close lifecycle.
- Camera tracking state, timestamp, and camera-pose extraction.
- Session-scoped native anchor creation and detach.
- Same-thread enforcement for session operations.
- JVM tests for lifecycle and availability mapping.

Not implemented yet:
- Plane observation enumeration and hit testing.
- Capability confirmation from actual device/configuration state (currently reported as UNKNOWN).
- Full coordinate-convention golden tests and quaternion normalization policy.
- Host Activity lifecycle integration and permission request UX.
- Instrumented tests or physical-device validation.
- Flutter or Rust integration.

## Toolchain

- Android Gradle Plugin: 9.1.0
- Gradle: 9.3.1 (minimum compatible wrapper distribution)
- Kotlin Gradle Plugin: 2.4.10
- JDK: 17
- Compile SDK: 36
- Minimum SDK: 24
- ARCore SDK: 1.57.0

Versions are pinned; do not replace them with dynamic selectors. Review the official compatibility matrix before upgrading the Android/Kotlin/Gradle toolchain.

## Build and test

From this directory, with Android SDK and JDK 17 installed:

```bash
gradle test
gradle assemble
```

A Gradle wrapper is not committed yet. Add a wrapper after the project toolchain is exercised in a controlled environment and its wrapper JAR/distribution checksum can be verified.

## Host integration sequence

1. The host requests Android CAMERA permission using its own Activity/permission UX.
2. Construct `ArCoreSpatialProvider(context, adapterVersion)`.
3. Call `checkAvailability(cameraPermissionGranted = true)`.
4. If the result is `SUPPORTED_INSTALL_REQUIRED`, call `requestRuntimeInstall(activity, userRequestedInstall)` from a foreground Activity and handle the Activity result/resume flow before checking again.
5. Create, resume, update, pause, and close the provider session on one dedicated thread. Do not call session methods from the UI thread or concurrently from multiple threads.
6. Call `close()` when the host no longer owns the AR session.

The provider does not request runtime permission itself. The host remains responsible for permission grant, denial, revocation, and rationale UX.

## API and data boundary

Only normalized JSE types belong in the public Kotlin provider/session API. ARCore SDK types remain implementation details. Camera pose translations are meters; the timestamp is the ARCore provider monotonic timestamp, not Unix time. The current pose frame labels are `camera` to `arcore-world`; verify conversion against the portable Rust conventions before cross-language integration.

Capabilities intentionally remain `UNKNOWN` until device/configuration-backed detection is implemented. Do not treat this first slice as feature-complete: plane configuration is present, but plane observations and hit testing are not yet exposed.

The local provider does not require network access. Camera frames and spatial observations remain on-device unless a host application explicitly implements a separate consented data flow.
