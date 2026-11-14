# Android Provider Contract (JSE-003)

**Status:** proposed integration contract; not an implementation claim.

## 1. Goals and boundaries

The first Android provider uses Google's ARCore SDK as the device-facing tracking runtime while keeping JSE's public contracts vendor-neutral.

The adapter owns ARCore availability/install checks, camera permission coordination, session lifecycle, frame metadata, tracking state, plane observations, hit testing, and local anchors. It must not expose ARCore classes through the stable cross-platform API.

The first vertical slice does not include custom VIO running alongside ARCore, cloud anchors, cross-device relocalization, or mandatory camera-buffer transfer through Flutter.

## 2. Component boundary

    Android app / Flutter host
              |
       commands + compact events
              v
    JSE Android facade (Kotlin)
       |-- permission / availability coordinator
       |-- ARCore provider adapter
       |     |-- Session / Config
       |     |-- Frame / Camera
       |     |-- Plane / HitResult
       |     |-- Anchor
       |-- normalized DTO and capability mapper
       |-- optional narrow JNI/C ABI bridge
                         |
                         v
                  portable Rust JSE core

Kotlin owns Android lifecycle and ARCore objects. Rust owns portable algorithms and contracts where the workspace actually provides them. Do not introduce FFI merely to pass data that can remain in Kotlin. Add a bridge only for a demonstrated portable-core requirement.

## 3. Provider interface

The Kotlin adapter should implement an interface equivalent to the following pseudocode:

    interface SpatialProvider {
        val providerInfo: ProviderInfo
        suspend fun checkAvailability(): AvailabilityResult
        fun queryCapabilities(): CapabilityReport
        suspend fun createSession(config: SessionConfig): ProviderSession
    }

    interface ProviderSession {
        fun resume()
        fun pause()
        fun update(): ProviderFrame
        fun hitTest(request: HitTestRequest): List<HitTestResult>
        fun createAnchor(pose: Pose): AnchorHandle
        fun detachAnchor(anchor: AnchorHandle)
        fun close()
    }

This is illustrative, not a committed API signature. Respect ARCore threading constraints; session update operations must not be callable from arbitrary threads.

Provider identity should include stable ID android.arcore, adapter version, ARCore runtime version when discoverable, and selected configuration. Device diagnostics may include Android API level and model but should avoid unnecessary personal/device identifiers in telemetry.

## 4. Independent state machines

Availability: checking, supported-installed, supported-install-required, unsupported, permission-denied, unavailable, error.

Session: created, configured, running, paused, failed, closed.

Tracking: initializing, tracking, limited, lost.

A running session can have limited or lost tracking. Do not conflate session lifecycle with tracking quality.

Lifecycle requirements:
1. Check runtime availability and installation before session creation.
2. Request camera permission through the host application; never bypass Android permission UX.
3. Create, configure, and resume only when host lifecycle and permission allow.
4. Pause when backgrounded or camera ownership is lost.
5. Recreate the session when runtime errors make it unusable.
6. Close exactly once and release owned resources.
7. Make repeated pause/close safe; reject resume after close.
8. Map camera contention and install/update requirements to typed errors.

The host owns Activity/Fragment lifecycle integration. The provider owns ARCore session resources.

## 5. Normalized frame contract

A provider frame carries metadata; it should not require a copied pixel buffer.

| Field | Contract |
|---|---|
| sequence | Monotonic per-session frame sequence |
| timestamp | Provider timestamp and documented clock domain |
| tracking | Normalized state and limitation reason |
| camera pose | Transform with explicit source/destination frames |
| intrinsics | Focal lengths, principal point, dimensions, calibration metadata |
| display geometry | Rotation/crop metadata for screen-coordinate mapping |
| image | Optional native-owned handle/texture with documented lifetime |
| provider | Provider ID and runtime version |

Do not assume ARCore timestamps share an epoch with Android wall-clock time or another sensor source. Any conversion or synchronization must be explicit and tested.

Document image/texture lifetime and synchronization. Native texture handles are scoped to their graphics context and lifetime; they must not be serialized or treated as durable resources.

## 6. Coordinate conversion

Verify existing JSE Rust conventions and ARCore API conventions before implementation.

Requirements:
- meters for translation and radians for angles;
- explicit camera, device/body, and world/map frame identifiers;
- one documented transform multiplication/order convention;
- centralized conversion for handedness, axes, quaternion ordering, and display orientation;
- finite-value validation at the adapter boundary;
- tests for identity, translation, rotation, inverse, and composition.

Never patch axis/sign differences independently in UI widgets. Add golden conversion tests using known poses and rays.

## 7. Capability mapping

Query capabilities from the active device, runtime, and configuration. Do not infer support from Android version or device marketing name alone.

Initial identifiers:
- camera_tracking
- plane_detection_horizontal
- plane_detection_vertical
- hit_test
- anchors_local
- depth
- instant_placement
- augmented_images
- environmental_hdr
- geospatial
- cloud_anchor

Each report entry includes available, unavailable, requires_configuration, or unknown; a reason; and relevant constraints. The first vertical slice need not implement every capability. Unimplemented capabilities must remain unavailable.

## 8. Planes, hit tests, and anchors

Planes normalize ID, pose, extent, orientation/classification where available, tracking state, and update timestamp. Plane IDs are session-scoped unless stronger persistence is proven.

A hit-test request specifies screen coordinates or a ray in a named frame. Results include hit pose, distance, target kind, tracking state, and source frame. Screen-to-world conversion must use current display geometry.

A JSE anchor ID is distinct from the provider-native handle. Native handles are opaque and session-scoped; never persist them as durable identity. Anchor records include map/session identity, pose, tracking state, creation source, and timestamps. Detaching releases the native resource and emits a lifecycle event.

The first milestone validates anchors only within a live session. Persistence and relocalization belong to later milestones.

## 9. Threading and event delivery

- Follow ARCore's documented threading constraints.
- Keep frame acquisition and time-sensitive work off the UI thread.
- Serialize session mutations to prevent resume/pause/update races.
- Deliver compact state changes and pose/plane/anchor events to Flutter.
- Coalesce high-frequency pose updates if consumers cannot keep up; never create an unbounded event queue.
- Never block frame acquisition on network, disk, or Dart callbacks.
- Include session ID and sequence/timestamp metadata so consumers can discard stale events.

## 10. Error model

Distinguish at minimum: unsupported/unavailable runtime, install/update required, permission denied/restricted, camera unavailable/in use, invalid configuration, paused/closed session, limited/lost tracking, resource exhaustion, and provider/runtime failure.

Errors include stable JSE code, safe message, recoverability, and optional provider diagnostic code.

## 11. Test strategy

JVM/unit tests cover availability mapping, capability mapping, lifecycle state machine, tracking mapping, coordinate conversion, error mapping, and anchor lifecycle using fakes.

Instrumented/device tests cover permission grant/deny/revoke, install/update paths where testable, lifecycle cycles, camera contention, tracking initialization/recovery/loss, planes, hit testing across rotations, anchor creation/detach, and resource behavior.

A fake provider or emulator is not physical-device validation. Record evidence separately.

## 12. Implementation sequence

1. Add Android module/provider skeleton and dependency/version policy.
2. Implement availability and permission error mapping.
3. Implement lifecycle with a fakeable session factory.
4. Add normalized pose, timestamp, intrinsics, and tracking state.
5. Add plane observations and hit testing.
6. Add local anchors and detach semantics.
7. Add capability report and device diagnostics.
8. Add Flutter reference host after native provider tests pass.
9. Add Rust FFI only where the portable core consumes normalized data; benchmark and document the boundary.

## 13. Exit criteria

JSE-003 is complete only when the provider is implemented, lifecycle/mapping tests pass, at least one supported physical Android device is tested, pose/tracking/planes/hit tests/anchors are demonstrated, capabilities match runtime reality, and exact device/build/test evidence is recorded. No cloud dependency is allowed for the local session.
