# JSE Architecture

## 1. Scope and non-goals

Jezaku Spatial Engine (JSE) is a reusable spatial runtime and SDK. It provides normalized contracts and portable spatial processing while delegating hardware-specific AR runtime work to platform providers when appropriate.

JSE is not:
- Google's ARCore SDK or a Google product;
- an application identity/social platform;
- a mandatory cloud service;
- a promise that Android and iOS expose identical capabilities;
- a requirement to run custom VIO alongside native platform tracking.

## 2. Logical layers

```text
+---------------------------------------------------+
| Consuming applications                            |
| Jezaku / third-party apps / robotics / tools      |
+---------------------------------------------------+
                       |
+---------------------------------------------------+
| Public SDK                                         |
| Engine / Session / Frame / Pose / Plane / Anchor   |
+---------------------------------------------------+
                       |
+---------------------------------------------------+
| Runtime orchestration and capability negotiation  |
+---------------------------------------------------+
                       |
+----------------------+----------------------------+
| Provider adapters    | Portable processing       |
| Android ARCore       | Fusion / geometry          |
| iOS ARKit            | Mapping / localization     |
| Native VO            | Serialization / indexing   |
| Replay               |                            |
+----------------------+----------------------------+
                       |
+---------------------------------------------------+
| Optional service boundary                         |
| Map registry / storage / synchronization / sharing|
+---------------------------------------------------+
```

The on-device runtime and optional server are separate deployment and lifecycle concerns.

## 3. Provider model

A provider adapts a concrete source of observations and capabilities to JSE contracts.

Initial provider families:
- **Android ARCore:** native camera, tracking, planes, hit testing, anchors, and optional runtime features.
- **iOS ARKit:** native camera, world tracking, planes, anchors, and optional runtime features.
- **Native VO:** independent visual-odometry pipeline based on available camera/sensor input.
- **Replay:** deterministic playback of recorded frames and sensor samples for tests and evaluation.

Provider selection is explicit or policy-driven and observable. The engine reports provider identity, version, selected mode, and capabilities.

### Capability negotiation

Capabilities are runtime facts, not compile-time assumptions. A capability report should include:
- capability identifier;
- available/unavailable/unknown status;
- reason or limitation where available;
- provider/runtime version;
- relevant configuration constraints.

Examples include depth, plane detection, image tracking, cloud-anchor support, environmental mesh, and geographic localization. A device may support one capability but not another.

## 4. Runtime lifecycle

The public session lifecycle should have explicit states:

```text
Created -> Configured -> Running -> Paused -> Running
                         |             |
                         v             v
                      Failed        Closed
```

Tracking state is separate from session state:

- **Initializing:** insufficient observations for stable tracking.
- **Tracking:** pose is currently tracked.
- **Limited:** pose exists but quality is constrained; reason should be exposed when available.
- **Lost:** no trustworthy current pose is available.

A session can be running while tracking is limited or lost. Do not conflate camera/session lifecycle with tracking quality.

Lifecycle requirements:
- start/resume and pause are idempotent where practical;
- close releases provider resources and detaches native handles;
- permission and installation errors are explicit;
- callbacks/events have documented threading and ordering;
- no blocking network operation occurs on the camera/tracking thread.

## 5. Coordinate frames and transforms

All public transform data must define the source and destination frame.

Required conceptual frames:
- **World/map:** local spatial map coordinate frame.
- **Device/body:** rigid frame attached to the device.
- **Camera:** optical camera frame, including documented axis convention.
- **Sensor:** IMU frame, with calibrated extrinsics to body/camera.
- **Geographic:** explicitly defined local tangent frame such as ENU, tied to a geodetic origin.

Conventions:
- distances in meters;
- angles in radians;
- right-handed coordinate frames unless a boundary explicitly documents conversion;
- timestamps from a monotonic clock for fusion;
- wall-clock timestamps only for metadata/audit;
- transforms named/documented as `T_A_B` meaning transform coordinates from frame B into frame A (or choose another convention once and apply it consistently).

The exact convention must be checked against existing code before being made a stable API. Centralize conversions at provider boundaries; do not scatter sign flips and axis swaps through consumers.

## 6. Data contracts

### Camera frame metadata

A frame contract should cover:
- sequence/frame identifier;
- monotonic capture timestamp and clock domain;
- image dimensions and pixel format;
- camera intrinsics and calibration identifier;
- orientation/rotation metadata;
- crop/stride information where relevant;
- ownership/lifetime of any image buffer;
- provider/session identity.

Do not copy full camera buffers unless a consumer explicitly requests them. Document whether a buffer is borrowed, reference-counted, or transferred.

### Pose and tracking

A tracking result should include:
- pose and frame identity;
- timestamp and clock domain;
- tracking state and optional limitation reason;
- quality/confidence with documented semantics;
- provider source and tracking mode.

A confidence field must not be described as a probability unless calibrated as one.

### Anchors

An anchor is a pose attached to a particular map/world frame, not just a position. It needs:
- stable JSE anchor ID;
- map ID and map version/frame;
- transform;
- anchor type/source;
- lifecycle/tracking state;
- provider-native handle only as opaque, process/session-scoped implementation detail;
- creation and update metadata.

Never persist a provider-native pointer or assume a native anchor handle survives process restart.

## 7. Mapping and persistence

JSE should define a provider-independent, versioned map representation over time. It may contain:
- map ID and format version;
- coordinate-frame definition and origin;
- calibration provenance;
- keyframes and feature/landmark observations;
- planes and optional mesh/semantic data;
- quality metrics and capture metadata;
- references to anchors.

Provider-specific map blobs may be stored as optional opaque extensions, but they are not the canonical portable map and must be tagged with provider/runtime compatibility metadata.

Map loading must validate:
- schema/version;
- payload size and decompression limits;
- numeric finiteness and bounds;
- frame and calibration references;
- integrity/hash where applicable;
- resource limits before allocation.

Relocalization must estimate and validate a transform into the target map frame. Do not restore anchors solely because their serialized coordinates exist.

## 8. Native and Flutter boundaries

Preferred flow:

```text
Camera + sensors -> native provider -> JSE runtime/processing
                                      |
                                      +-> renderer/native UI
                                      |
                                      +-> compact state/events -> Flutter
```

- Kotlin owns Android ARCore session and camera lifecycle.
- Swift owns iOS ARKit lifecycle.
- Rust owns portable algorithms and data processing where appropriate.
- FFI should be narrow, versioned, and explicit about ownership/threading.
- Flutter receives compact events and commands; avoid per-frame full-image transfer through Dart.
- GPU textures should remain in a compatible native graphics path where possible.

Do not decide JNI versus C ABI solely by preference. Prototype the smallest safe boundary, benchmark it, and document ownership, threading, and error semantics.

## 9. Optional service architecture

The existing `ar-service` TCP/NDJSON server is a transport/service component, not the definition of the on-device AR runtime.

Keep optional services responsible for:
- map registration and metadata;
- map blob storage/retrieval;
- shared map/anchor synchronization;
- access control and authorization;
- version negotiation;
- collaboration signaling.

The local engine must continue to function when the service is unreachable. Product authentication and authorization should be injected at the service boundary, not embedded into portable tracking crates.

## 10. Security and privacy

Camera, spatial map, and location data can reveal sensitive places and behavior.

- Default to local processing.
- Upload only with explicit user consent and a documented purpose.
- Minimize retention and access.
- Validate all remote and persisted binary data.
- Apply limits to image/map sizes, feature counts, decompression, and CPU/memory use.
- Avoid logging images, precise trails, raw map payloads, tokens, or credentials.
- Make map deletion and revocation semantics explicit for shared services.

## 11. Architectural evolution

Do not immediately split every module into a separate crate. First stabilize boundaries in the existing workspace. Extract crates when they have:
- a clear owner and responsibility;
- a small dependency surface;
- meaningful independent tests;
- a stable public contract;
- a reason to version/reuse independently.

Preserve the initial workspace during repository extraction. Refactor incrementally after the baseline builds.
