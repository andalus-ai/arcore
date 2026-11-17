# Android plane observations and hit testing

## Scope

This increment exposes the first interactive AR surface workflow through the vendor-neutral JSE Android API:

1. Configure the host view's display geometry.
2. Resume the ARCore session and call `update()` on its owner thread for each render tick.
3. Read the plane snapshot from `ProviderFrame.planes`.
4. Send view-local pixel coordinates to `hitTest(xPixels, yPixels)`.
5. Use a recent `SpatialHit` with `createAnchorFromHit(hit)`.
6. Keep the returned `AnchorHandle` session-scoped and detach it when the placed object is removed.

This is a local AR capability. It does not require a Jezaku backend, network access, account authentication, or cloud persistence.

## Public data contract

- `PlaneObservation.id` is stable for the lifetime of the provider session.
- `trackingState` maps ARCore TRACKING/PAUSED/STOPPED to JSE TRACKING/LIMITED/LOST.
- `centerPose` is a plane-to-`arcore-world` pose in meters.
- `extentX` and `extentZ` are the current bounding rectangle dimensions in meters.
- `polygon` contains local plane X/Z coordinates in meters. It is a snapshot; copy it if retained.
- `subsumedById` identifies the parent plane when ARCore merges planes.
- Plane snapshots are enumerated from the current session on every `update()`; consumers should key by ID and replace prior snapshots rather than append indefinitely.

Hit results are deliberately opaque:
- A `SpatialHit` contains a session ID, frame sequence, kind, pose, distance, and optional plane ID.
- A hit is valid only for the frame sequence that produced it.
- A hit token is consumed once by `createAnchorFromHit`.
- Tokens expire on the next `update()`, display-geometry change, pause, or session close.
- Plane hits outside the ARCore plane polygon are filtered out.

## Display geometry and coordinates

The host must call `setDisplayGeometry(rotationDegrees, widthPixels, heightPixels)` whenever the AR view's dimensions or display rotation change. Values are view pixel dimensions and clockwise rotation in degrees (0, 90, 180, 270). Hit-test coordinates must be local to that same view, not screen-global coordinates and not Flutter logical pixels unless the host has converted them to physical pixels.

Call display geometry before hit testing. Call `update()` before each hit test so the session has a current camera frame. Do not cache hits across a frame boundary.

## Thread and lifecycle rules

All session operations, including display geometry, update, hit testing, anchor creation/detach, pause, and close, must run on the same dedicated thread that created the provider session. Serialize calls; do not call these methods concurrently from UI/render workers.

- Call `pause()` when the host loses foreground ownership.
- Clear rendered plane overlays when tracking becomes LIMITED/LOST or when the session pauses.
- Resume and wait for a new `update()` before accepting taps.
- Detach anchors when the corresponding placed object is explicitly removed.
- Closing the session invalidates all plane IDs, hit tokens, and anchor handles.

## Rendering guidance

The provider supplies geometry and tracking state, not a renderer. A host may render plane polygons as translucent overlays, but should avoid treating the plane's bounding rectangle as its exact boundary. Update overlays from each frame snapshot and visually distinguish LIMITED/LOST states. For tap placement, prefer the nearest valid plane hit and call `createAnchorFromHit`; do not reconstruct a world pose from the screen coordinate yourself.

The ARCore SDK's hit-test result is nearest-first. The provider preserves that ordering after filtering plane hits outside their polygon.

## Validation status

Source and API changes are committed in the feature branch. Gradle compilation, JVM tests, emulator checks, and physical-device checks have not been run in this environment. In particular, validate portrait/landscape rotation, camera/view aspect mismatch, plane polygon stability, hit-token expiry, and anchor stability on supported hardware before shipping.
