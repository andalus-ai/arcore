# Android Provider Implementation Plan (JSE-004)

**Status:** implementation plan; this document does not claim that Android code or device validation exists.

## 1. Objective

Deliver the first usable Android implementation of the JSE provider contract using the native ARCore SDK. The initial deliverable is a Kotlin library/provider that can be embedded by a host Android application and exercised by automated tests before it is connected to Flutter or the Rust workspace.

The provider must remain product-neutral. It must not depend on Jezaku account, identity, social, feed, or backend services.

## 2. Dependency and repository sequencing

The destination repository currently contains the JSE documentation and CI workflow; the history-preserving Rust workspace import remains a separate prerequisite for Rust integration.

Implementation order:

1. Land the Android module as an independently buildable Android library.
2. Keep all ARCore types inside the Android provider module.
3. Define Kotlin-facing normalized contracts and fake implementations for host integration tests.
4. Validate the module on a physical ARCore-capable Android device.
5. Integrate Rust only after the extracted workspace and a concrete cross-language use case are available.
6. Add a Flutter sample after the native provider lifecycle is stable.

Do not block the Kotlin provider on Rust extraction. Do not create placeholder Rust crates or JNI interfaces just to make the directory structure look complete.

## 3. Target module layout

Use this as the initial module boundary; adapt to the repository's existing build conventions when the module is added.

```text
providers/android-arcore/
├── build.gradle.kts
├── consumer-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── kotlin/ai/andalus/jse/android/
    │       ├── AndroidArCoreProvider.kt
    │       ├── ArCoreAvailabilityChecker.kt
    │       ├── ArCoreSessionController.kt
    │       ├── ArCoreCapabilityMapper.kt
    │       ├── ArCoreFrameMapper.kt
    │       ├── ArCorePlaneMapper.kt
    │       ├── ArCoreAnchorRegistry.kt
    │       ├── ArCoreErrorMapper.kt
    │       ├── CoordinateTransform.kt
    │       └── model/
    │           ├── ProviderInfo.kt
    │           ├── AvailabilityResult.kt
    │           ├── CapabilityReport.kt
    │           ├── SessionConfig.kt
    │           ├── ProviderFrame.kt
    │           ├── TrackingState.kt
    │           ├── PlaneObservation.kt
    │           ├── HitTestRequest.kt
    │           ├── HitTestResult.kt
    │           └── AnchorHandle.kt
    └── test/
        └── ... unit tests with fakes
```

The package name is a proposal. Confirm the organization-wide namespace before publishing the first artifact. Keep Android-specific implementation classes internal where possible; expose only the provider facade and stable normalized contracts.

## 4. Build and dependency policy

- Use an Android library module, not an application module.
- Align compile SDK, min SDK, Kotlin, AGP, and Gradle versions with the repository's chosen Android toolchain when one exists.
- Pin the ARCore dependency to an explicit tested version; avoid dynamic versions and version ranges.
- Keep ARCore dependency declarations local to this provider module.
- Document required Android permissions and ARCore manifest metadata in the module manifest.
- Do not request camera permission from a library. The host app owns runtime permission UX and passes permission state into provider/session creation.
- Do not add Flutter, JNI, NDK, or Rust dependencies in the first Kotlin-only increment.
- Add dependency/license review before publishing or distributing binaries.

## 5. Public Kotlin API boundary

Start with small interfaces and immutable DTOs. The API should express lifecycle and observations without leaking ARCore classes.

Proposed shape (illustrative; finalize names and signatures in code review):

```kotlin
interface SpatialProvider {
    val info: ProviderInfo
    suspend fun checkAvailability(): AvailabilityResult
    fun capabilities(): CapabilityReport
    fun createSession(config: SessionConfig): ProviderSession
}

interface ProviderSession : AutoCloseable {
    val state: SessionState
    fun resume()
    fun pause()
    fun update(): ProviderFrame
    fun hitTest(request: HitTestRequest): List<HitTestResult>
    fun createAnchor(pose: SpatialPose): AnchorHandle
    fun detachAnchor(anchor: AnchorHandle)
    override fun close()
}
```

API requirements:

- Use typed results/errors for expected platform conditions; do not use exceptions as ordinary availability control flow.
- Make session methods enforce documented thread affinity.
- Give every session a unique opaque session ID.
- Make close idempotent and invalidate all session-scoped handles.
- Make invalid operations after close fail deterministically.
- Keep DTOs independent of ARCore SDK classes and Android UI classes.
- Avoid exposing raw camera buffers in the first API; expose metadata and, only if required, a separately documented native-owned image/texture handle.

## 6. Lifecycle and threading implementation

Implement a serialized session controller. It must own the ARCore Session and all mutations to it.

Required behavior:

1. Check ARCore availability and installation state before session creation.
2. Receive host permission state; return a typed permission-required/denied result when absent.
3. Create and configure the ARCore Session on the designated provider thread.
4. Resume only when host lifecycle and permission allow.
5. Pause promptly when the host backgrounds or camera ownership is lost.
6. Ensure update, resume, pause, reconfigure, and close cannot race.
7. Convert ARCore exceptions into stable JSE error categories.
8. Close the native session and release anchor handles exactly once.
9. Do not call blocking provider work on the Android main thread.
10. Do not emit callbacks while holding internal locks.

Define and test transitions for created, configured, running, paused, failed, and closed. Tracking state is a separate dimension and must not be represented as session lifecycle.

## 7. Frame, pose, and coordinate normalization

The frame mapper should produce compact metadata:

- monotonic per-session sequence number;
- ARCore frame timestamp with its clock-domain semantics retained;
- normalized tracking state and optional limitation reason;
- camera pose with explicit source and destination frame identifiers;
- camera intrinsics and image dimensions;
- display geometry/version used for screen-coordinate mapping;
- provider ID and runtime version when available.

Coordinate conversion must be centralized. Before implementation, compare the extracted Rust coordinate conventions with ARCore conventions. Do not assume axis, handedness, quaternion ordering, or transform multiplication order.

Add deterministic tests for identity, translation, 90-degree rotations, inverse transforms, composition, and invalid/non-finite values. Include screen-coordinate hit-test cases for portrait, landscape, and display-geometry changes.

## 8. Capabilities, planes, hit tests, and anchors

### Capabilities
Implement capability mapping from the active runtime, device, and selected configuration. Each capability must report available, unavailable, requires-configuration, or unknown with an optional reason. Do not advertise a feature merely because the SDK contains an API for it.

Initial vertical-slice capabilities:
- camera_tracking
- plane_detection_horizontal
- plane_detection_vertical
- hit_test
- anchors_local

Other capabilities (depth, instant placement, augmented images, environmental HDR, geospatial, cloud anchors) remain unavailable or unknown until explicitly implemented and tested.

### Planes and hit tests
Normalize plane IDs as session-scoped IDs. Include pose, extent, orientation/classification where available, tracking state, and update timestamp. Hit-test requests must identify screen or ray coordinate space. Reject invalid coordinates and stale display geometry.

### Anchors
Maintain a registry mapping JSE anchor IDs to opaque native ARCore Anchor objects. IDs are unique within a session. Detach releases the native anchor. Session close invalidates every handle. Do not persist native handles or claim cross-session persistence in this milestone.

## 9. Error taxonomy

Map provider failures into stable categories, including:

- runtime unsupported or unavailable;
- ARCore install/update required;
- camera permission required or denied;
- camera unavailable/in use;
- invalid configuration;
- operation while paused or closed;
- tracking limited or lost (state, not necessarily fatal error);
- resource exhaustion;
- provider/runtime failure.

Each error should include a stable code, safe user-facing message, recoverability, and optional sanitized provider diagnostic. Avoid logging raw frames, precise location, or unnecessary device identifiers.

## 10. Test plan and gates

### Unit tests (JVM)
- availability/install-state mapping;
- capability mapping for device/configuration combinations;
- lifecycle transition table and illegal transitions;
- tracking-state mapping;
- coordinate conversion and finite-value validation;
- error mapping;
- anchor ID uniqueness, detach, and session invalidation;
- frame sequence and timestamp preservation.

Use a small ARCore-facing abstraction or adapter seam so most mapping and lifecycle tests run without a device. Do not mock every SDK object indiscriminately; keep SDK interaction isolated.

### Instrumented/device tests
- permission granted, denied, and revoked;
- runtime installed/update-required/unsupported paths where reproducible;
- create/resume/update/pause/close;
- repeated pause/resume and recreate cycles;
- camera contention and recovery;
- tracking initialization and recovery;
- horizontal/vertical planes and hit tests across display rotations;
- anchor create, update, detach, and close invalidation;
- continuous-session stability and resource observations.

### Completion gates
- Android library builds from a clean checkout using documented commands.
- JVM tests pass.
- Instrumented tests compile and run on the configured test target.
- At least one supported physical device completes the core lifecycle, tracking, plane, hit-test, and anchor scenarios.
- Device metadata and sanitized evidence are recorded using ANDROID_DEVICE_VALIDATION.md.
- Unsupported capabilities are reported honestly.
- No Flutter/Rust integration is claimed until separately implemented and tested.

## 11. Integration sequence after the Kotlin provider

1. Import the Rust workspace with preserved Git history and run its baseline checks.
2. Compare Rust and Kotlin spatial DTOs; identify genuinely shared contracts.
3. Choose one boundary: JNI/C ABI only if Rust must consume or produce data on the critical path; otherwise keep the provider native and integrate at a higher level.
4. If FFI is justified, document ownership, thread affinity, ABI versioning, error mapping, and buffer lifetime before writing bindings.
5. Add a Flutter Spatial Lab host that displays provider availability, session state, tracking state, pose, capabilities, planes, and anchor events.
6. Validate end-to-end behavior on physical hardware.

## 12. Risks and explicit non-goals

- ARCore support depends on device/runtime availability; the library must surface unsupported states.
- Lifecycle and camera ownership are more failure-prone than the initial DTO mapping; test these first.
- Coordinate convention mistakes can produce plausible but incorrect poses; require golden tests.
- High-frequency event delivery can overwhelm Flutter; coalesce observations and bound queues.
- The first milestone does not deliver custom VIO, map persistence, relocalization, cloud anchors, shared maps, or iOS support.
- An emulator, fake provider, or green CI run is not physical-device validation.
