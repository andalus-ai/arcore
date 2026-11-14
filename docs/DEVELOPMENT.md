# Development Guide

## Prerequisites

The portable Rust workspace requires a supported Rust toolchain and Cargo. Platform integrations additionally require their native SDK toolchains.

- Rust toolchain matching the workspace's declared MSRV/toolchain policy.
- Git.
- Android Studio/Android SDK and NDK for Android provider work.
- Xcode and supported iOS toolchain for future ARKit work.
- Physical AR-capable devices for runtime validation.

Check the actual crate manifests and CI configuration before assuming target/platform support.

## First checkout

```bash
git clone https://github.com/andalus-ai/arcore.git
cd arcore
```

## Rust checks

Run from the workspace root:

```bash
cargo fmt --all -- --check
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
```

If a command fails because a target, crate, or tool is not yet configured, record that as a repository gap. Do not mask it by weakening checks without a documented reason.

For a quick iteration on one crate:

```bash
cargo test -p ar-core
cargo clippy -p ar-core --all-targets -- -D warnings
```

Use the actual package name from its Cargo manifest; crate directory and package names may differ.

## Android provider validation

Before running an Android integration:
1. Confirm the device supports the required ARCore features.
2. Confirm Google Play Services for AR is installed/available where required.
3. Grant camera permission.
4. Record device model, Android version, ARCore version, app build, and test environment.
5. Verify lifecycle transitions and resource cleanup.

Minimum manual test:
- launch session;
- move device slowly through a textured environment;
- observe tracking initialization and transition to tracking;
- inspect pose updates and timestamps;
- detect a horizontal surface;
- hit-test and create an anchor;
- walk around the anchor and inspect stability;
- pause/resume the app;
- close and reopen the session;
- verify unsupported capabilities are reported honestly.

A same-session native anchor test is not proof of cross-session or cross-device persistence. Test and label those separately.

## Dataset and replay tests

Replay tests should be deterministic where possible. Record:
- dataset source/license;
- camera calibration;
- sensor timestamp clock domain;
- ground-truth availability;
- preprocessing steps;
- expected metrics and tolerances.

Synthetic data validates controlled behavior but cannot establish real-world robustness. Keep synthetic, public benchmark, and device-captured datasets clearly distinguished.

## Performance measurements

Measure rather than guess:
- session startup latency;
- frame delivery rate and timestamp jitter;
- tracking interruptions;
- pose drift/error against ground truth where available;
- plane and anchor stability;
- relocalization latency/success;
- CPU, memory, thermal state, and battery impact.

Record hardware, OS, provider/runtime versions, environment, motion profile, and build configuration with each result.

## Coding workflow

- Keep PRs focused and small.
- Add tests with behavior changes.
- Update API and architecture docs with contract changes.
- Avoid unrelated formatting/refactoring in algorithm changes.
- Do not commit secrets, raw private spatial datasets, or camera recordings without appropriate consent and licensing.
- Follow [AGENTS.md](../AGENTS.md).

## Reporting results

A useful test report states:
- exact command/device and software versions;
- pass/fail outcome;
- test data and conditions;
- observed metrics;
- known gaps and reproduction steps.

Never report an unrun test as passing or a proposed capability as implemented.
