# Android provider capability reporting

## Contract

`SpatialProvider.capabilities()` is a snapshot for the most recently configured session on that provider instance.

- Before a session is successfully created and configured, exposed capabilities report `UNKNOWN`.
- After successful ARCore session configuration, camera tracking, hit testing, and local anchors report `AVAILABLE`.
- Horizontal and vertical plane finding report `AVAILABLE` only when enabled in the successfully configured `SessionConfig`; disabled modes report `UNAVAILABLE` with an explanation.
- If session construction or configuration fails, the previous capability snapshot is retained. A failed attempt must not advertise new capabilities.
- When multiple sessions are created sequentially on one provider, the report describes the most recently configured session. Hosts needing per-session capability state should retain the session's own `SessionConfig`.

## What this does not claim

This is configuration-backed capability reporting, not a hardware benchmark or a guarantee that a surface will be detected in every environment. Actual observations still depend on camera permission, tracking quality, lighting, scene texture, device support, and ARCore runtime behavior.

The provider currently does not expose depth sensing, instant placement configuration, augmented images, cloud anchors, or geospatial capabilities. They must not be inferred from ARCore SDK availability alone.

## Host guidance

1. Call `checkAvailability()` and handle camera permission/runtime installation.
2. Create a session with the desired horizontal/vertical plane modes.
3. Read `capabilities()` after session creation succeeds.
4. Gate plane UI using the matching capability status.
5. Continue to inspect each `ProviderFrame.trackingState` and each plane's tracking state; an AVAILABLE capability does not mean a plane is currently tracked.

Capability values are vendor-neutral strings and statuses. Do not branch on ARCore SDK classes outside the provider implementation.

## Tests and validation

`CapabilityDetector` is a pure Kotlin component with JVM unit coverage for unknown, default, partial-plane, and no-plane configurations. These tests are committed but have not been executed in this environment. Gradle build, emulator, and physical-device validation remain pending.
