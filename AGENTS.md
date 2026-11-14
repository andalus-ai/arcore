# AGENTS.md — Jezaku Spatial Engine

These instructions apply to the entire repository unless a deeper `AGENTS.md` overrides them.

## Mission and boundaries

- This repository builds the **Jezaku Spatial Engine (JSE)**: a neutral, reusable spatial-computing engine.
- The repository name is `arcore`; this project is not Google's ARCore and is not affiliated with Google.
- Keep the engine independent of Jezaku product concepts. Do not add Jezaku accounts, profiles, social graphs, feeds, messaging, payments, or product-specific business rules to portable engine crates.
- Jezaku is a consumer/reference application. It must integrate through public, versioned APIs.
- Prefer native platform capabilities through adapters (Google ARCore on Android, Apple ARKit on iOS) while keeping portable types and processing independent of vendor SDK types.
- Do not claim feature parity with ARCore/ARKit or production readiness without tests and device evidence.

## Before changing code

1. Inspect the root `Cargo.toml`, crate manifests, source code, tests, and existing documentation. Do not assume proposed structures in planning documents already exist.
2. Identify the current owner of each behavior and avoid duplicate implementations.
3. Check coordinate conventions, units, timestamp semantics, threading, ownership, and lifecycle before changing spatial APIs.
4. Keep changes incremental. Preserve existing crate names and behavior during repository extraction unless a migration is explicitly required.
5. If a requested change depends on platform SDK behavior, consult the relevant official platform documentation and record version/device constraints.

## Architecture rules

- Portable Rust crates must not depend on Android, ARCore, iOS, ARKit, Flutter, or a specific app backend.
- Platform-specific code belongs in provider/adapter packages.
- Provider capabilities must be explicit and queryable. Never silently emulate unsupported features or report a capability as available without runtime confirmation.
- Native provider tracking remains authoritative when selected. Do not run a competing VIO pipeline by default without a defined fusion/fallback design.
- The local engine must work without network access. Cloud services are optional.
- Keep `ar-service` (remote transport/server functionality) separate from the on-device session/runtime lifecycle.
- Avoid circular crate dependencies. Keep foundational types and interfaces small.
- Do not expose vendor-specific SDK objects in the stable public API. Use adapters and normalized, documented data contracts.
- Avoid unnecessary copies of camera images, GPU textures, and high-frequency frame buffers across JNI, FFI, and Dart boundaries.
- Never block a real-time camera/tracking thread on network I/O, disk I/O, or unbounded work.

## Spatial correctness

- Use SI units: meters, seconds, radians, and meters per second unless an API explicitly documents otherwise.
- Every pose/transform must identify its source and destination coordinate frames or have an unambiguous documented convention.
- Document handedness, axis directions, camera optical frame, device/body frame, map/world frame, and geographic frame.
- Use monotonic timestamps for sensor synchronization. Keep wall-clock timestamps separate for persistence/audit metadata.
- Handle timestamp discontinuities, out-of-order sensor samples, missing data, and tracking-state transitions explicitly.
- Validate finite numeric values and reject invalid transforms rather than allowing NaN/Inf to propagate.
- Distinguish tracking confidence/quality from a calibrated probability. Document the meaning and range of every score.
- Anchor persistence must include the map/frame identity and version. Never persist a bare XYZ coordinate as if it were globally stable.
- Cross-device map reuse requires localization and coordinate alignment; a serialized anchor record alone is not cross-device persistence.

## Rust quality

- Follow existing workspace conventions and minimum supported Rust version, if declared.
- Prefer clear types and explicit errors over panics in library code.
- Avoid `unwrap()`/`expect()` on runtime, sensor, file, network, and user-provided data paths.
- Keep public APIs documented. Add examples for nontrivial public APIs.
- Add unit tests for math, transforms, serialization, state machines, and edge cases.
- Add integration tests for provider lifecycle and data-contract behavior where feasible.
- Keep dependencies justified and review licenses, platform support, and maintenance health before adding them.
- Do not introduce unsafe Rust without a documented safety rationale and narrow encapsulation.
- Do not add blanket `allow` attributes to silence lints without explaining why.

## Testing and validation

Run the relevant checks, and report exactly what was and was not run:

```bash
cargo fmt --all -- --check
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
```

If a command is not supported by the current workspace or environment, explain why; do not claim it passed.

For Android/provider changes, add or update tests for:
- permission and installation/availability failures;
- session create, resume, pause, and close lifecycle;
- unsupported capabilities;
- timestamp and coordinate conversion;
- tracking states (initializing, tracking, limited, lost);
- anchor creation and cleanup;
- device/runtime errors.

Synthetic/replay tests are valuable but do not replace physical-device validation. Record device model, Android version, ARCore availability/version, build variant, test conditions, and observed metrics for device tests.

## API and data compatibility

- Treat public types, serialized formats, and FFI symbols as compatibility surfaces.
- Version persisted map formats independently from crate versions.
- Prefer additive API changes. Document breaking changes and migration steps.
- Use explicit schema/version fields in persisted map and anchor formats.
- Do not serialize opaque ARCore/ARKit internal map blobs as the canonical cross-platform JSE map format.
- Never promise that ARCore and ARKit maps are directly interchangeable.

## Security and privacy

- Process camera frames and sensor data on-device by default.
- Do not upload camera imagery, spatial maps, or location data without explicit product-level user consent and a documented purpose.
- Minimize retention and collection of sensitive spatial data.
- Treat map downloads, serialized maps, and shared-anchor payloads as untrusted input; validate sizes, versions, numeric values, and resource limits.
- Do not log raw camera frames, precise location trails, credentials, tokens, or private map contents.
- Keep authentication/authorization for optional cloud services outside the local tracking API.

## Documentation expectations

Update relevant documentation when changing:
- public APIs or coordinate conventions;
- provider capability behavior;
- lifecycle/state transitions;
- map/anchor serialization;
- platform requirements;
- build/test procedures;
- performance claims or measured results.

Clearly label proposals, implemented behavior, and validated behavior separately.

## Pull requests

A good PR should include:
1. Problem and rationale.
2. Architecture/API impact.
3. Files/modules changed.
4. Tests run and results.
5. Platform/device validation (or explicit statement that it was not performed).
6. Documentation and migration notes.
7. Known limitations and follow-up work.

Keep PRs focused. Do not combine repository extraction, broad crate renaming, algorithm rewrites, and app integration into one change.
