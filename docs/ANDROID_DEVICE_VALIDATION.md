# Android Device Validation Plan

**Status:** test plan; no device execution is implied.

## 1. Purpose

Provide repeatable evidence that the JSE Android provider works on real hardware. Keep platform availability, automated tests, synthetic/replay tests, and physical-device results distinct.

## 2. Test matrix and run metadata

Start with at least one ARCore-supported physical Android device. Before closing the milestone, expand to a mid-range device, a higher-performance device, and an unsupported or capability-limited path where available, across the Android API levels the app supports.

Record for every run:
- exact manufacturer and device model;
- Android version and API level;
- Google Play Services for AR version;
- app commit, build variant, and package version;
- JSE/provider commit and version;
- camera configuration and enabled options;
- environment (indoor/outdoor, lighting, texture, surface);
- test run ID, pass/fail/blocked result, and issue link.

Do not infer support from model name alone; record the runtime availability result.

## 3. Test cases

### A. Availability and permissions
- [ ] Runtime supported and installed.
- [ ] Runtime supported but install/update required.
- [ ] Device/runtime unsupported.
- [ ] Camera permission granted.
- [ ] Camera permission denied.
- [ ] Permission revoked while app is inactive.
- [ ] Camera unavailable or claimed by another app.
- [ ] Recovery after install/permission issue is resolved.

Expected: typed, actionable states; no crash or indefinite spinner.

### B. Session lifecycle
- [ ] Create and configure a session.
- [ ] Resume and receive frames.
- [ ] Pause when app enters background.
- [ ] Resume after returning to foreground.
- [ ] Repeat pause/resume at least 20 times.
- [ ] Close and recreate session at least 10 times.
- [ ] Attempt invalid operations after close.
- [ ] Test camera contention if reproducible.

Expected: no leaked camera ownership, stale callbacks, duplicate sessions, or unbounded resource growth. Record lifecycle timing and recovery delay.

### C. Tracking and pose
- [ ] Start in a low-texture scene and observe initializing/limited state.
- [ ] Move slowly through a textured indoor scene.
- [ ] Rotate in place and translate while observing pose updates.
- [ ] Observe temporary tracking limitation and recovery.
- [ ] Test low light and motion blur separately.
- [ ] Verify timestamp monotonicity within a session.
- [ ] Verify pose values remain finite and frame conventions are correct.

Expected: state transitions are visible and pose is not presented as trustworthy while tracking is lost. Do not claim metric accuracy without ground truth.

### D. Planes and hit testing
- [ ] Detect a horizontal floor/table.
- [ ] Detect a vertical wall if supported by runtime/configuration.
- [ ] Verify plane pose and extent update as scene understanding improves.
- [ ] Hit-test screen center and off-center points.
- [ ] Repeat after portrait/landscape rotation.
- [ ] Reject stale display geometry and invalid coordinates.

Expected: hits map to the visible surface and coordinate conversions remain consistent across rotations.

### E. Anchors
- [ ] Create an anchor from a valid hit result.
- [ ] Move around it and observe pose/tracking state.
- [ ] Create multiple anchors and verify unique JSE IDs.
- [ ] Detach one anchor and verify cleanup/event.
- [ ] Pause/resume and observe documented native behavior.
- [ ] Close session and verify native handles are invalidated.

Expected: anchor identity is session-scoped for this milestone. Do not describe it as persistent across app restart or devices.

### F. Capabilities
- [ ] Capture capability report for the test device.
- [ ] Compare each report with runtime/configuration behavior.
- [ ] Verify unavailable capabilities include a reason where known.
- [ ] Change configuration and confirm capability state refreshes if needed.

Expected: no capability is reported available solely because a code path exists.

### G. Stability and resource use
- [ ] Run a continuous session for at least 15 minutes.
- [ ] Repeat session start/stop cycles.
- [ ] Observe memory, CPU, thermal state, and battery trend.
- [ ] Confirm frame/event queues do not grow without bound.
- [ ] Confirm raw frames or precise trails do not appear in logs.

Expected: no crash, persistent camera lock, runaway memory, or unbounded backlog. Treat measurements as baseline, not release-grade performance claims.

## 4. Evidence capture

For each case, save run metadata, pass/fail/blocked result, sanitized logs/provider diagnostics, issue links, timings/metrics, and environmental conditions.

Screen recordings require appropriate consent and privacy review. Do not commit private camera recordings, identifiable faces, home interiors, or precise location traces to the public repository. Store sensitive evidence in an approved private location and commit only sanitized summaries.

## 5. Release gate

The Android vertical slice is not complete until:
- [ ] Native provider lifecycle tests pass.
- [ ] Device tests pass on at least one supported physical device.
- [ ] Pose/tracking, plane detection, hit testing, and local anchors are demonstrated.
- [ ] Capability report is checked against runtime behavior.
- [ ] At least one pause/resume/recreate stress run is completed.
- [ ] Known limitations and failed cases are recorded.
- [ ] Build/test commands and exact source commit are recorded.

A green CI run does not replace physical-device validation. An emulator or fake provider does not satisfy the physical-device gate.
