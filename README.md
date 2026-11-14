# Jezaku Spatial Engine (JSE)

**A neutral, reusable spatial-computing engine for mobile and other spatial applications.**

This repository is the home of the Jezaku Spatial Engine. It is designed to provide portable spatial APIs and reusable spatial-processing components while integrating with platform-native capabilities where they are strongest.

> **Naming note:** This project uses the repository name `arcore`, but it is not Google's ARCore and is not affiliated with Google. In documentation and APIs, the preferred project name is **Jezaku Spatial Engine (JSE)**.

## Project goals

- Provide a vendor-neutral API for camera frames, sensors, tracking, planes, anchors, maps, and localization.
- Reuse mature platform runtimes such as Google ARCore on Android and Apple ARKit on iOS through adapters.
- Keep portable spatial data models and processing in Rust where appropriate.
- Support independent providers, including native visual-odometry and recorded-data/replay providers.
- Make local on-device operation possible without requiring a cloud service.
- Add optional services for map storage, distribution, and shared spatial experiences.
- Keep the engine independent of any single product, identity system, social graph, or application backend.

## Architecture at a glance

```text
Applications (Jezaku, third-party apps, tools)
                 |
          Public JSE SDK/API
                 |
       Session + capability model
                 |
     Provider interfaces / adapters
       |          |           |
    Android      iOS       Replay / Native VO
    ARCore       ARKit
       |          |           |
       +------ Spatial core --+
                  |
       Fusion / geometry / maps /
       anchors / localization
                  |
       Optional spatial services
```

Platform adapters own platform-specific lifecycle and runtime objects. JSE normalizes supported observations and capabilities; it must not pretend that every provider exposes identical features or internal maps.

## Current starting point

The initial implementation is being extracted from `jezaku/Backend/ar-core`. The existing Rust workspace contains these crates:

- `ar-core`
- `ar-fusion`
- `ar-geo`
- `ar-planes`
- `ar-vo`
- `ar-service`
- `ar-replay`

Treat these as the starting inventory, not as proof that every capability is production-ready. Consult [the roadmap](docs/ROADMAP.md) and [architecture notes](docs/ARCHITECTURE.md) for known gaps and sequencing.

## First milestone

**JSE Foundation v0.1** focuses on a standalone, buildable Rust workspace and a physical-device Android vertical slice:

1. Establish the repository and workspace independently of Jezaku.
2. Document coordinate frames, units, timestamps, capabilities, and lifecycle contracts.
3. Define provider interfaces without leaking ARCore/ARKit-specific types into the public API.
4. Integrate an Android ARCore provider.
5. Expose a small, stable native boundary to a Flutter Spatial Lab example.
6. Validate camera lifecycle, tracking, plane detection, hit testing, and anchors on real devices.

Do not claim v0.1 complete based only on synthetic tests or compilation. See [ROADMAP.md](docs/ROADMAP.md) for acceptance criteria.

## Repository structure (target)

The structure will evolve incrementally. Do not create empty crates just to match this diagram.

```text
.
├── Cargo.toml
├── Cargo.lock
├── crates/                 # portable Rust workspace crates
├── providers/
│   ├── android-arcore/     # Android / ARCore adapter
│   ├── ios-arkit/          # iOS / ARKit adapter (later)
│   ├── native-vo/           # independent Rust tracking provider
│   └── replay/              # recorded-data provider
├── bindings/
│   └── flutter/             # optional Flutter integration/example
├── examples/
│   └── spatial-lab/
├── benchmarks/
└── docs/
```

Preserve the existing crate layout during the initial history-preserving extraction. Move and rename modules only when a concrete API boundary and migration are agreed.

## Development

Start by inspecting the actual workspace manifest and crate READMEs:

```bash
cargo fmt --all -- --check
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
```

Run only commands supported by the current workspace; add missing targets and CI incrementally. Android builds also require the Android SDK/NDK and the supported Rust Android target(s).

See [DEVELOPMENT.md](docs/DEVELOPMENT.md) for contribution and validation guidance and [AGENTS.md](AGENTS.md) for coding-agent instructions.

## Product integration

Jezaku is the first reference application, not a runtime dependency. Product-specific identity, social, account, feed, and business logic belongs in the consuming application or its backend.

For initial local development, a consuming repository may use a pinned Git dependency or a temporary Cargo path override. Prefer immutable tags/commits for reproducible builds. Do not introduce a permanent Git submodule unless there is a demonstrated workflow need.

## Status

Early-stage engineering project. APIs, provider coverage, and persistence formats are subject to change until explicitly stabilized and versioned.
