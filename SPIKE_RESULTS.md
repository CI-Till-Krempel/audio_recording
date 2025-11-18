### Spike Results — Epic C (iOS Recording via Swift Wrapper)

This document records only the outcomes of the spike. It intentionally excludes plans and future implementation steps.

Date: 2025‑11‑18

#### Spike goal (for context)
- Validate feasibility of using a thin Swift wrapper around AVFoundation, invoked from Kotlin Multiplatform via Objective‑C interop.

#### Decision (outcome)
- Adopt the Swift wrapper approach for iOS recording, exposing a minimal ObjC‑visible API to KMP.
- Rationale evidenced in spike: Swift provides the most direct, stable access to AVFoundation; a small bridge reduces interop friction and type/constant mismatch issues.

#### Evidence and concrete changes produced by the spike
- `iosApp/iosApp/ARAudioRecorder.swift` exists (wrapper entry point scaffolded in the app target).
- `iosApp/iosApp/Info.plist` includes `NSMicrophoneUsageDescription` with a user‑facing rationale.
- `composeApp/src/iosMain/.../recorder/IosRecorder.kt` present as a bridge placeholder to keep shared code compiling.
- `composeApp/src/iosMain/.../recorder/RecordingFiles.ios.kt` updated to handle iOS recording file paths.

#### Current functional state (end of spike)
- iOS factory continues to return `FakeRecorder`; the new wrapper is not yet invoked at runtime.
- Project builds for iOS with the above scaffolding; no on‑device audio file output was validated during this spike.

#### Observations and constraints discovered
- Objective‑C visibility is required for Swift types used by Kotlin/Native; keeping the public surface minimal appears practical.
- Microphone permission string in `Info.plist` is mandatory; missing it blocks recording attempts.
- File storage strategy aligns with using the app Caches directory for temporary recordings.

#### Risks noted (from evidence, not plans)
- Interop configuration can be brittle across Gradle/Xcode settings; minimizing the exposed API surface reduces complexity.
- Background behavior may vary with `AVAudioSession` category; needs empirical validation in later work (not part of this spike).
