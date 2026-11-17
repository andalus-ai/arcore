# JSE Android ARCore Provider

Standalone Android library for the Jezaku Spatial Engine (JSE). This module is independent of the Jezaku application, Flutter, and the Rust workspace.

## Current implementation status

Implemented:
- Vendor-neutral Kotlin provider/session contracts and normalized DTOs.
- Typed provider error categories.
- ARCore runtime availability mapping and user-mediated install/update request.
- ARCore session construction and horizontal/vertical plane configuration.
- Session resume/pause/update/close lifecycle with same-thread enforcement.
- Camera tracking state, provider monotonic timestamp, and camera-pose extraction.
- Session-scoped native anchor creation and detach.
- Plane observation snapshots with stable session IDs, extents, local polygon vertices, tracking state, and subsumption links.
- Display-geometry updates for 0/90/180/270-degree rotation.
- View-pixel hit testing with plane-polygon filtering.
- Opaque, one-frame hit tokens and anchor creation directly from hit results.

Not implemented yet:
- Capability confirmation from actual device/runtime configuration (currently reported as UNKNOWN).
- Full coordinate-convention golden tests and quaternion normalization policy.
- Host Activity lifecycle and CAMERA permission UX integration.
- Renderer, Flutter/JNI bridge, Rust integration, or cloud persistence.
- Instrumented tests or physical-device validation.

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
5. Create and use the provider session on one dedicated host-owned thread.
6. After the AR view has a size, call `setDisplayGeometry(rotationDegrees, widthPixels, heightPixels)`; repeat after rotation or size changes.
7. On each render tick, call `update()`. Render the returned plane snapshot from `ProviderFrame.planes`.
8. Convert tap coordinates to physical pixels local to the AR view and call `hitTest(xPixels, yPixels)`.
9. Choose a valid hit and call `createAnchorFromHit(hit)`. The hit token expires at the next update, geometry change, pause, or close.
10. Detach the returned anchor when its placed object is removed. Pause/close the session when the host no longer owns it.

The provider does not request runtime permission itself. The host remains responsible for permission grant, denial, revocation, rationale UX, and Activity lifecycle.

## API and data boundary

Only normalized JSE types belong in the public Kotlin API. ARCore SDK types remain implementation details. Distances and translations are meters. The frame timestamp is provider-monotonic, not Unix time. Pose frame labels currently use `camera`, `plane`, or `anchor` as source and `arcore-world` as target; verify conversion against portable Rust conventions before cross-language integration.

Plane polygons are local plane X/Z coordinates in meters. Hit-test coordinates must match the physical-pixel dimensions passed to display geometry. Plane observations are snapshots and should be replaced by ID, not accumulated. All session calls are thread-affine and must be serialized on the session-creation thread.

See [Android plane observations and hit testing](../../docs/ANDROID_PLANE_HIT_TESTING.md) for data semantics, hit-token lifetime, rendering guidance, and device validation cases.

The local provider does not require network access. Camera frames and spatial observations remain on-device unless a host application explicitly implements a separate consented data flow.
