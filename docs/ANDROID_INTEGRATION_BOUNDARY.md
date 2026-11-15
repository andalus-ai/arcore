# Android Integration Boundary: Jezaku and JSE

## Status

Design note for the JSE extraction and Android provider work. This document describes repository ownership and integration boundaries; it does not claim that the JSE Android provider has been implemented.

## Why this boundary matters

Jezaku already contains a Flutter-facing Android AR plugin. Its responsibilities include method-channel calls, Flutter texture/platform-view rendering, app lifecycle integration, and mapping native failures into Dart-facing errors. The standalone Jezaku Spatial Engine (JSE) should not absorb those Flutter product concerns.

The current Jezaku implementation is visible in:

- `UI/Packages/ar_engine/android/`
- `UI/Packages/ar_engine/lib/`
- `UI/Packages/ar_engine/test/`

The source repository's 2026-11-14 change `c7dd8055070dac90d42db9ed1d88296056c60bb9` improves synchronous resume error propagation and camera-permission handling. Treat this as application integration behavior, not as a JSE core API.

## Ownership

| Concern | Owner | Notes |
|---|---|---|
| ARCore SDK session creation, update loop, tracking state | JSE Android provider | Platform-specific adapter implementing JSE contracts |
| ARCore availability/install checks | JSE Android provider | Report structured capability and availability results |
| Camera permission request UX | Jezaku app/plugin | The engine reports permission requirements and errors; it must not own app permission UI |
| Flutter method channel and Dart exceptions | Jezaku plugin | Product-facing API, compatibility, and error translation |
| Flutter texture / platform-view lifecycle | Jezaku plugin | Keep rendering transport out of portable Rust crates |
| Frame metadata and normalized pose/state events | JSE provider boundary | Stable, documented, versioned contracts |
| Coordinate conversion and portable spatial math | JSE Rust crates | Explicit frame conventions and units |
| Plane/anchor semantics | JSE contracts + provider | Provider maps platform objects into engine-neutral identifiers and values |
| App analytics, navigation, UI state | Jezaku | Never introduce these dependencies into JSE |

## Required provider boundary

The Android provider should expose an engine-facing API that is independent of Flutter classes. Conceptually:

```kotlin
interface SpatialProvider {
    fun checkAvailability(): AvailabilityResult
    fun createSession(config: SessionConfig): ProviderResult<SessionHandle>
    fun resume(handle: SessionHandle): ProviderResult<Unit>
    fun pause(handle: SessionHandle): ProviderResult<Unit>
    fun close(handle: SessionHandle)
    fun poll(handle: SessionHandle): ProviderResult<SpatialFrame>
}
```

This is illustrative pseudocode, not a committed public API. Final signatures must align with the extracted Rust/JSE contracts and the Android provider specification.

The Flutter plugin should call the provider through a narrow adapter and translate results into its existing channel contract. It should not pass Flutter objects into the JSE provider.

## Error handling

- JSE provider errors must be typed and stable: unavailable, installation-required, permission-required/denied, session-not-initialized, tracking-limited, resource-exhausted, and provider-internal.
- The provider must not silently convert a failed synchronous resume into success.
- The app/plugin owns permission prompts and user-facing recovery.
- The app/plugin may map a JSE permission error to its established Dart exception type.
- Do not expose raw platform exception strings as the only machine-readable error contract.
- Pause or close native resources consistently after failed lifecycle transitions.

## Data and threading

- Keep camera image buffers and GPU textures native unless a deliberate data path requires copying.
- Send compact pose, tracking, plane, anchor, timestamp, and diagnostic events across the boundary.
- Use monotonic timestamps and document their clock domain.
- Document coordinate handedness, axis directions, units, and transform direction at every boundary.
- Do not block Flutter's platform thread with image processing or expensive Rust calls.
- Define thread affinity for every provider method and callback.

## Extraction scope

The history-preserving extraction from `jezaku/Backend/ar-core/` imports the Rust workspace only. It must not move the Flutter plugin or app-specific Kotlin files from `UI/Packages/ar_engine/` into JSE.

The Android provider implementation belongs in JSE as a reusable Android adapter. Jezaku remains the first integration client and reference application.

## Acceptance criteria

- [ ] JSE Android provider has no Flutter imports or method-channel dependency.
- [ ] Jezaku plugin remains responsible for Flutter lifecycle and rendering transport.
- [ ] Provider errors map deterministically to existing app-facing error behavior.
- [ ] Resume failure is propagated; no false-success result is returned.
- [ ] Permission UX remains in the app/plugin.
- [ ] Frame timestamps and coordinate conventions are explicit and tested.
- [ ] Fake-provider tests cover lifecycle and error mapping without a physical device.
- [ ] Physical-device validation is recorded separately from unit/instrumented test results.
