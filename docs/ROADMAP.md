# Jezaku Spatial Engine Roadmap

This roadmap separates **planned**, **implemented**, and **validated** work. Dates and durations are estimates, not commitments. Milestones must be closed based on evidence and acceptance criteria, not elapsed time.

## Product direction

JSE is a vendor-neutral spatial engine and SDK. It should use native platform AR runtimes where they provide mature hardware integration, while owning portable APIs, spatial data contracts, map persistence, localization workflows, and optional shared-spatial services.

The initial strategy is **hybrid, Android first**:
- Android adapter: Google ARCore.
- Future iOS adapter: Apple ARKit.
- Portable core: Rust.
- Reference UI: Flutter Spatial Lab, kept outside the portable core.
- Optional server: separate from the on-device runtime.

## Principles

1. **Engine first, product neutral:** no Jezaku account/social/business dependencies.
2. **Native where appropriate:** use platform tracking initially; do not duplicate native VIO without a measured reason.
3. **Portable contracts:** normalized observations, explicit frames, capabilities, and versioned persistence.
4. **Offline-first runtime:** local tracking and anchors must not require cloud connectivity.
5. **Evidence-based quality:** synthetic tests, replay datasets, and physical-device tests are distinct evidence levels.
6. **Incremental extraction:** preserve the existing workspace and history before restructuring crates.

## Baseline inventory

The extracted workspace originates from `jezaku/Backend/ar-core` and currently has these crate responsibilities (verify against source before changing):
- `ar-core`: foundational spatial types and frontend abstractions.
- `ar-fusion`: error-state Kalman filter / sensor fusion.
- `ar-geo`: geographic coordinate conversion.
- `ar-planes`: plane detection and raycasting.
- `ar-vo`: visual odometry and point-cloud functionality.
- `ar-service`: Tokio TCP/NDJSON service.
- `ar-replay`: recorded-data replay/evaluation.

Known limitations reported in the original project documentation and requiring fresh verification:
- VO evaluation has relied on synthetic/rendered scenes rather than sufficient real-camera evidence.
- Bundle adjustment and loop closure are not yet complete.
- Relocalization after tracking loss is not implemented.
- Map state may be discarded after tracking loss.
- Camera/IMU extrinsics are configured rather than robustly calibrated/estimated.
- Physical Android integration is not yet validated.

Do not repeat these as current facts without checking the latest code and tests.

---

## Milestone 0 — Repository extraction and baseline

**Goal:** make the engine independently buildable without changing algorithms.

### Work
- [ ] Extract `Backend/ar-core` from Jezaku into this repository while preserving Git history.
- [ ] Record source repository URL and exact source commit SHA.
- [ ] Confirm destination default branch and avoid overwriting existing history.
- [ ] Preserve existing crate names and workspace structure initially.
- [ ] Add README, license, contribution/security guidance, and this roadmap.
- [ ] Add CI for formatting, tests, and Clippy where supported.
- [ ] Verify all workspace-relative paths and documentation links.
- [ ] Document build prerequisites and known unsupported targets.

### Acceptance
- [ ] Standalone clone can build/test using documented commands.
- [ ] Existing tests pass or pre-existing failures are documented with reproducible details.
- [ ] No dependency on the Jezaku checkout is required.
- [ ] Extraction provenance is recorded.
- [ ] No product-specific code has leaked into the engine.

## Milestone 1 — JSE Foundation v0.1 (Android vertical slice)

**Goal:** create a real device AR session through a neutral JSE API.

### 1.1 Contracts and runtime
- [ ] Define coordinate-frame identifiers and transform conventions.
- [ ] Define SI units and timestamp semantics.
- [ ] Define camera intrinsics and frame metadata contracts.
- [ ] Define tracking states: initializing, tracking, limited, lost.
- [ ] Define capability discovery and provider identity/version.
- [ ] Define session lifecycle: create, configure, resume, pause, close.
- [ ] Define explicit error types for permission, unsupported device, runtime, and lifecycle failures.
- [ ] Keep provider-specific types behind adapters.

### 1.2 Android provider
- [ ] Add a Kotlin Android ARCore adapter.
- [ ] Handle ARCore availability/install and camera permission flows.
- [ ] Implement session lifecycle and configuration.
- [ ] Expose camera pose, intrinsics, timestamps, and tracking state.
- [ ] Expose plane observations and hit testing.
- [ ] Expose native anchor creation, tracking, and detachment.
- [ ] Report runtime capabilities (depth, image tracking, etc.) based on actual device/runtime support.
- [ ] Handle pause/resume, tracking loss, camera contention, and device/runtime errors.

### 1.3 Rust and app boundary
- [ ] Define a narrow C ABI/JNI boundary only where required.
- [ ] Specify ownership, thread affinity, buffer lifetime, and error propagation.
- [ ] Avoid sending full-resolution frames through Dart for ordinary tracking.
- [ ] Add a Flutter Spatial Lab reference example outside portable crates.
- [ ] Display camera state, tracking state, pose, capability report, and detected planes.

### Acceptance
- [ ] Standalone Rust workspace builds and tests.
- [ ] Android provider initializes on at least one supported physical device.
- [ ] Camera/session lifecycle survives repeated start/pause/resume/close cycles.
- [ ] Pose and tracking-state updates are observable through JSE.
- [ ] Plane detection and hit testing work on a physical device.
- [ ] At least one anchor can be created, tracked while moving, and cleanly detached.
- [ ] Unsupported capabilities are reported accurately.
- [ ] Device model, OS version, ARCore version, test conditions, and results are recorded.
- [ ] No claim of iOS support until an iOS adapter is implemented and tested.

## Milestone 2 — Spatial capabilities v0.2

**Goal:** reliable local spatial primitives and persistence.

- [ ] Introduce anchor IDs and explicit map/frame references.
- [ ] Define anchor lifecycle and loss semantics.
- [ ] Persist local anchor metadata and provider handles where supported.
- [ ] Define a versioned JSE spatial-map schema.
- [ ] Add serialization/deserialization with size and numeric validation.
- [ ] Add keyframe and landmark data structures.
- [ ] Add map quality metadata and calibration provenance.
- [ ] Define provider-specific persistence capability boundaries.
- [ ] Test app restart and restoration on the same device/provider where supported.

### Acceptance
- [ ] Map and anchor data round-trip across supported schema versions.
- [ ] Invalid, oversized, or unsupported map data is rejected safely.
- [ ] Anchors are never restored against an unknown or incompatible map frame.
- [ ] Provider limitations are surfaced rather than hidden.

## Milestone 3 — Relocalization and mapping v0.3

**Goal:** recover a device's pose relative to a previously saved JSE map.

- [ ] Build keyframe selection and local map management.
- [ ] Add place-recognition/descriptor indexing.
- [ ] Add feature matching and robust pose estimation.
- [ ] Add relocalization confidence and failure states.
- [ ] Evaluate loop closure and bundle adjustment.
- [ ] Preserve maps through tracking interruptions.
- [ ] Add replayable real-world datasets and regression suites.
- [ ] Measure drift, relocalization success, latency, and memory use.

### Acceptance
- [ ] A saved map can be loaded and matched under documented test conditions.
- [ ] Relocalization reports quality and refuses unsafe low-confidence alignment.
- [ ] Performance and accuracy are measured against recorded datasets.
- [ ] Results include device, environment, motion, lighting, and failure cases.

## Milestone 4 — Shared spatial services

**Goal:** optional multi-device spatial experiences without coupling the core to one backend.

- [ ] Define map upload/download and synchronization protocols.
- [ ] Define map ownership, permissions, and revocation at the service boundary.
- [ ] Define shared coordinate-frame alignment.
- [ ] Define anchor synchronization and conflict handling.
- [ ] Add version compatibility and integrity checks.
- [ ] Add privacy controls, retention, and explicit consent requirements.
- [ ] Keep cloud service deployment independent of the local runtime.

### Acceptance
- [ ] Two supported devices can localize into a documented common frame under validated conditions.
- [ ] Shared anchors include map identity/version and alignment quality.
- [ ] Offline behavior and conflict/retry semantics are documented.
- [ ] No camera or location data is uploaded without explicit consent.

## Milestone 5 — iOS provider and cross-platform SDK

**Goal:** add an iOS implementation behind the same public contracts.

- [ ] Implement ARKit adapter and lifecycle.
- [ ] Map ARKit observations into documented JSE types.
- [ ] Report capability differences explicitly.
- [ ] Validate coordinate conversion and timestamp semantics.
- [ ] Implement Swift/native boundary and Flutter integration.
- [ ] Add cross-platform conformance tests.
- [ ] Document features that cannot be made behaviorally identical.

### Acceptance
- [ ] Android and iOS pass shared API conformance tests.
- [ ] Platform-specific limitations are discoverable.
- [ ] No unsupported feature is silently emulated.
- [ ] A reference application runs on physical devices on both platforms.

## Milestone 6 — SDK hardening and v1.0

- [ ] Stabilize public API and semantic versioning policy.
- [ ] Stabilize map and anchor serialization formats.
- [ ] Publish developer documentation and examples.
- [ ] Add compatibility matrix and support policy.
- [ ] Add crash/error telemetry hooks that are opt-in and privacy-preserving.
- [ ] Benchmark CPU, memory, thermal behavior, battery, and frame latency.
- [ ] Complete security review of parsers, FFI, and network protocols.
- [ ] Publish crates/packages only after licensing, API, and release checks.

### v1.0 acceptance
- Documented support matrix and tested reference devices.
- Stable public API and migration policy.
- Versioned map format and compatibility policy.
- Reproducible CI and release process.
- Physical-device test evidence.
- Security and privacy documentation.
- Independent local operation without a cloud dependency.

---

## Priority labels

- **P0:** blocks a usable, testable foundation.
- **P1:** required for reliable persistent spatial functionality.
- **P2:** cross-device, advanced, or scale-out functionality.
- **P3:** research/experimentation with no release commitment.

## Measurement policy

Track at least:
- session startup and recovery time;
- frame timestamp jitter and dropped frames;
- tracking-state duration and loss frequency;
- pose error/drift on datasets with ground truth;
- plane/hit-test stability;
- anchor stability and restoration success;
- relocalization success rate and latency;
- CPU, memory, thermal, and battery behavior.

Never publish a numeric performance or accuracy claim without a reproducible test setup and recorded results.
