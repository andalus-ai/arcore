# JSE Android ARCore Provider

Standalone Android library for the Jezaku Spatial Engine (JSE). This module is intentionally independent of the Jezaku application, Flutter, and the Rust workspace.

## Current status

This is the provider foundation, not a complete ARCore runtime implementation.

Implemented:
- Vendor-neutral Kotlin provider/session contracts and DTOs.
- Stable provider error categories.
- Deterministic internal session lifecycle state machine.
- JVM tests for basic lifecycle transitions.
- Pinned Android, Kotlin, Gradle-plugin, and ARCore dependency versions.

Not implemented yet:
- ARCore availability/install checker and runtime session adapter.
- Camera permission host integration.
- Frame, pose, plane, hit-test, and anchor mapping.
- Capability detection from a real device/runtime.
- Instrumented tests or physical-device validation.

## Toolchain

- Android Gradle Plugin: 9.1.0
- Gradle: 9.3.1 (minimum compatible wrapper distribution)
- Kotlin Gradle Plugin: 2.4.10
- JDK: 17
- Compile SDK: 36
- Minimum SDK: 24
- ARCore SDK: 1.57.0

Versions are pinned; do not replace them with dynamic selectors. Review the official compatibility matrix before upgrading the Android/Kotlin/Gradle toolchain.

## Build and test

From this directory, with Android SDK and JDK 17 installed:

```bash
gradle test
gradle assemble
```

A Gradle wrapper is not committed yet. Add a wrapper after the project toolchain is exercised in a controlled environment and its wrapper JAR/distribution checksum can be verified.

## API boundary

Only normalized JSE types belong in the public Kotlin API. ARCore SDK classes must remain implementation details. The host application owns camera permission request UX and must pass the resulting permission state to the provider. Session operations will be serialized on a provider-owned thread when the concrete adapter is introduced.

The local provider must not require network access. Camera frames and spatial observations remain on-device unless a host application explicitly implements a separate consented data flow.
