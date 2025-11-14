### Product Requirements Document (PRD)

#### 1. Summary
A Kotlin Multiplatform (KMP) spike to evaluate audio recording across Android and iOS with a simple interface (Record, Pause, Play), focusing on enabling recording while the device is locked. If lockscreen recording is not feasible or too risky, evaluate a fallback that keeps the screen awake via a wake lock. Deliver a working prototype, platform feasibility insights, risks, and an implementation plan for a production-ready solution.

#### 2. Objectives and Key Results (OKRs)
- O1 (Feasibility): Determine if lockscreen recording is possible on Android and iOS, document constraints and required UX/technical workarounds.
  - KR1: A prototype that can continue recording with the device locked on Android, with documentation of exact APIs used and limitations.
  - KR2: A prototype that can continue recording with the device locked on iOS (if possible), or a clear statement why it isn’t and what alternatives exist.
- O2 (Fallback): Validate an alternative approach that keeps the screen on during recording via a wake lock (Android) and equivalent iOS strategy.
  - KR3: A prototype demonstrating wake-lock based recording with acceptable battery impact notes.
- O3 (Productization Readiness): Identify platform-specific risks, permissions, UX, battery, privacy and security implications.
  - KR4: Risk register and mitigation plan for production rollout.

#### 3. Scope
- In Scope:
  - Minimal UI: Record, Pause/Resume, Stop, Play last recording.
  - Audio capture on Android and iOS using platform-appropriate APIs, wrapped by shared KMP interfaces where sensible.
  - Background/lockscreen behavior investigation and prototypes.
  - Wake lock alternative and user-visible indicators (e.g., notification on Android) where applicable.
  - Storage of recordings locally and playback.
  - Documentation: feasibility, risks, API differences, battery impact, UX considerations, QA scenarios.
- Out of Scope (for the spike):
  - Cloud sync, sharing, advanced editing, transcription.
  - Complex UI/visualizations (waveforms), multi-track mixing.
  - Long-term storage management/policies beyond a simple file list and size note.

#### 4. Users and Use Cases
- Primary user: Field worker or interviewer needing to record long sessions.
- Use cases:
  - Quick voice memo with device possibly locked in pocket.
  - Long meeting/interview recording without accidental touch inputs.
  - Playback immediately after recording.

#### 5. Functional Requirements
- FR1: Show an interface with buttons: Record, Pause/Resume, Stop, Play (last recording).
- FR2: Request and handle microphone permission (Android + iOS).
- FR3: Start an audio recording, save to a local file, and expose path/metadata.
- FR4: Pause/resume during recording if the platform supports it; if not, simulate pause via segmented files and concatenate or playback stitching.
- FR5: Stop recording and finalize the file.
- FR6: Play the last completed recording in-app (basic controls: play/pause/stop).
- FR7: Lockscreen recording evaluation:
  - FR7a (Android): Attempt recording while device is locked using a foreground service + media projection if needed (not required for mic) and `MediaRecorder`/`AudioRecord`. Provide persistent notification.
  - FR7b (iOS): Investigate background modes (`audio`, `airplay`, `voip` are deprecated for this use), and AVAudioSession categories allowing recording while screen is off. Determine if iOS allows continued recording with screen off; document capability and any user-facing indicators.
- FR8: Fallback: Keep screen awake during recording
  - FR8a (Android): Implement wake lock (`PARTIAL_WAKE_LOCK` or `SCREEN_DIM_WAKE_LOCK` + `acquire()` via `PowerManager`), and/or `FLAG_KEEP_SCREEN_ON`.
  - FR8b (iOS): Use `UIApplication.isIdleTimerDisabled = true` while recording to prevent auto-lock.
- FR9: Handle interruptions (incoming call, audio focus changes) gracefully, resume where possible.
- FR10: Provide user-visible indication when recording is ongoing (Android notification, in-app banner; iOS in-app indicator).
- FR11: Basic file management: show size/duration post-recording in logs; at least persist last recording.

#### 6. Non-Functional Requirements
- NFR1: Reliability: No data corruption on stop; guard against crashes during recording via safe finalization and temp file strategy.
- NFR2: Battery: Measure and report battery impact for lockscreen vs. wake-lock approaches in 10–30 minute sessions.
- NFR3: Privacy/Security: Microphone permission rationale; clearly indicate recording status. Store files in app-private storage by default.
- NFR4: Portability: Shared KMP interface with thin platform-specific implementations.
- NFR5: Maintainability: Clear separation between UI layer, recorder service, and storage.
- NFR6: Observability: Minimal logging for key events; optional simple metrics (durations, errors).

#### 7. Platform Considerations and Findings to Produce
- Android:
  - Use `AudioRecord` or `MediaRecorder` for mic input. For lockscreen continuation, run as a foreground service with persistent notification; device can be locked; recording should continue. Wake lock may be necessary to avoid CPU sleep depending on implementation; typically a foreground service with partial wake lock is sufficient for long-running audio recording.
  - Permissions: `RECORD_AUDIO`, optional `FOREGROUND_SERVICE`, and exact foreground service type (mediaProjection not needed for mic-only). Target SDK foreground restrictions documented.
  - Interruptions: Audio focus API, phone calls, Bluetooth routing.
- iOS:
  - Use `AVAudioSession` with category `.playAndRecord` or `.record`, activate session. Recording can continue when screen is off; true "lock screen UI controls" (custom controls on lock screen) are not available for recording—Now Playing controls are for playback, not recording. Background modes for recording: Apple's guidance allows background audio for playback; recording in background is possible for certain use-cases but tightly controlled; must test if the app continues while device is locked with screen off. Deliver clear findings with Xcode logs and sample code.
  - Permissions: `NSMicrophoneUsageDescription` in `Info.plist`.
  - Interruptions: Route changes, phone calls; handle via notifications.

#### 8. UX Requirements
- Minimal UI:
  - Idle: Record enabled, Play disabled until a recording exists.
  - Recording: Show elapsed time, Record transforms to Stop, show Pause if supported.
  - Paused: Show Resume and Stop.
  - Playing: Show Pause/Stop for playback.
- Indicators:
  - Android: Foreground service notification when recording; tapping returns to app.
  - iOS: In-app banner during recording; lock screen likely won’t show recording controls.
- Errors: Clear toasts/snackbars or banners for permission denied, storage error.

#### 9. Data and Storage
- File format: Start with AAC (m4a) or WAV depending on API availability. Define per-platform default:
  - Android: AAC LC in MPEG-4 container (m4a) via `MediaRecorder`.
  - iOS: AAC (m4a) via `AVAudioRecorder`.
- File naming: Timestamp-based, e.g., `rec_YYYYMMDD_HHMMSS.m4a`.
- Location: App-private storage; provide retrieval of last file path.

#### 10. Analytics/Instrumentation (optional for spike)
- Log session starts/stops, durations, and error counts to console; no external analytics in spike.

#### 11. Risks & Mitigations
- R1: iOS restrictions may prevent lockscreen/background recording without special entitlement or behavior.
  - Mitigation: Provide fallback (idle timer disabled) and document user guidance.
- R2: Foreground service limitations on Android 14+.
  - Mitigation: Use correct foreground service type (`mediaProjection` not required), test on API 34; partial wake lock when necessary.
- R3: Battery drain with wake locks.
  - Mitigation: Prominent UI indicator and warning; measure and document drain.
- R4: Permissions denied.
  - Mitigation: Clear rationale and in-app guidance.
- R5: Audio focus and call interruptions cause data loss.
  - Mitigation: Handle interruptions; autosave temp buffers frequently.

#### 12. Deliverables
- D1: KMP project branch with minimal UI and recording/playback on Android and iOS.
- D2: Lockscreen/background recording feasibility report per platform.
- D3: Wake lock fallback implementation demo and findings.
- D4: Documentation updates (README pointers) and this PRD.
- D5: Risk register with platform notes and recommendations.

#### 13. Acceptance Criteria
- AC1: On Android, app can record, be sent to background/locked, and continue recording for at least 5 minutes; notification visible.
- AC2: On iOS, attempt recording, lock the device: document whether recording continues for at least 5 minutes; if not possible, explicitly document observed behavior and required settings/workarounds.
- AC3: Fallback approach works on both platforms to keep the app recording without user interaction for at least 5 minutes (wake lock / idleTimerDisabled).
- AC4: Play back the last recording successfully on both platforms.
- AC5: PRD and risk doc are updated with findings.

#### 14. User Stories (Initial Backlog)
- US1: As a user, I can start a recording so that I can capture audio.
  - Acceptance: Tapping Record starts recording; elapsed time shows; file saved on Stop.
- US2: As a user, I can pause and resume recording so that I can avoid unwanted segments.
  - Acceptance: Pause/Resume available if platform supports; otherwise simulated pause via segments.
- US3: As a user, I can stop recording and play back the last recording so that I can review it.
  - Acceptance: After Stop, Play is enabled and plays the last file.
- US4: As a user, I can continue recording while the device is locked or app is backgrounded on Android.
  - Acceptance: Recording continues >=5 minutes; notification persists.
- US5: As a user, I can attempt to record with device locked on iOS, with documented behavior and clear messaging.
  - Acceptance: Documented success or limitation; app handles either case.
- US6: As a user, I can use an alternative mode that keeps the screen awake to ensure uninterrupted recording.
  - Acceptance: A toggle or automatic fallback activates wake lock / idle timer disabled during recording; deactivates on Stop.
- US7: As a user, I receive clear feedback when the microphone permission is denied.
  - Acceptance: On denial, app shows rationale and disables Record.

#### 15. Implementation Plan (Spike then Productization)
- Phase 1: Spike (1–2 weeks)
  - P1.1 KMP API: Define `Recorder` interface in `commonMain` with start/pause/resume/stop, state flow, and event callbacks.
  - P1.2 Android impl: Use `MediaRecorder` (or `AudioRecord` for more control) in a foreground service; persistent notification; partial wake lock where needed.
  - P1.3 iOS impl: Use `AVAudioRecorder` with proper `AVAudioSession` setup; test lockscreen behavior; log findings.
  - P1.4 UI: Minimal Compose Multiplatform UI with buttons and state.
  - P1.5 Playback: Simple playback for last file (`MediaPlayer`/`AVAudioPlayer`).
  - P1.6 Fallbacks: Android wake lock + `FLAG_KEEP_SCREEN_ON`; iOS `isIdleTimerDisabled`.
  - P1.7 Measurement: Record battery level at start/end; log deltas for 10–30 minute tests.
  - P1.8 Documentation: Update PRD with findings; risk/mitigation table.
- Phase 2: Productization (2–4 weeks)
  - P2.1 Storage layer abstraction and list of recordings; metadata (duration, size).
  - P2.2 Error handling, retries, resilience.
  - P2.3 UX polish, indicators, settings (format/quality).
  - P2.4 QA matrix across Android API 26–34 and recent iOS versions.
  - P2.5 Security/privacy review; permission copy; localization.

#### 16. Technical Notes & APIs
- Android:
  - Foreground service with `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` not required for mic-only; use standard media type. Use `NotificationCompat` for channel and ongoing notification. Consider `WakeLock` with `PARTIAL_WAKE_LOCK`.
  - Recording API: `MediaRecorder` simple path; `AudioRecord` for PCM + encoding.
  - Playback: `MediaPlayer` or ExoPlayer (overkill for spike).
- iOS:
  - `AVAudioSession` category `.record` or `.playAndRecord`, `setActive(true)`. Use `AVAudioRecorder` for convenience.
  - Background/lock testing: Verify behavior on real device; simulators are not representative.
  - Playback: `AVAudioPlayer`.

#### 17. Open Questions
- Q1: iOS background recording policy variations across iOS versions and device models.
- Q2: Required wording around background activity to pass App Store review if recording continues while locked.
- Q3: Preferred default audio format/bitrate for target users.
- Q4: Do we need external microphone support and routing preferences?

#### 18. Success Metrics
- Spike success: Clear, evidence-based statement on lockscreen feasibility per platform with working demos and logs.
- Usability: Start/stop/pause/play flows validated by 3–5 internal testers.
- Stability: No crashes during 30-minute record sessions on test devices.
