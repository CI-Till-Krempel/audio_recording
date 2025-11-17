### Implementation Plans

This document provides concrete, actionable task breakdowns to implement the backlog in `../stories/STORIES.md`. It is organized by Epic and references Story IDs.

Conventions
- Platform notation: [A]=Android, [i]=iOS, [S]=Shared/commonMain, [UI]=Compose Multiplatform UI.
- Estimates: T‑shirt sizes from `AGENTS.md`.
- Evidence: Prefer screenshots/logs and short notes appended to PRD under Sections 7, 11, and 12.

---

### Epic A — Core KMP Recorder API and Minimal UI

A1 — US1: Start a recording (core)
- Tasks
  - [S] Define `Recorder` API
    - `start(config: RecordingConfig): Result<Unit>`
    - `pause(): Result<Unit>`
    - `resume(): Result<Unit>`
    - `stop(): Result<RecordingResult>`
    - `state: StateFlow<RecordingState>`
    - `capabilities: RecorderCapabilities`
  - [S] Define models
    - `RecordingConfig(sampleRate, bitRate, format, container, outputDirProvider)`
    - `RecordingState` = Idle | Recording(elapsedMs) | Paused(elapsedMs) | Error(message)
    - `RecordingResult(uri, startedAt, endedAt, durationMs, bytes)`
    - `RecorderCapabilities(supportsPause: Boolean)`
  - [UI] Minimal screen
    - Buttons: Record, Pause/Resume, Stop, Play (disabled until a file exists)
    - Elapsed time showing from `state`
  - [S] Timekeeping
    - Use monotonic clock for elapsed; update via coroutine timer when in Recording/Paused
- Definition of Done
  - API compiles for all targets; UI renders with stub platform implementations

A2 — US2: Pause/resume recording (core)
- Tasks
  - [S] Finalize `RecorderCapabilities`
  - [S] Define segmentation contract for platforms without native pause
    - `SegmentedRecorder` keeps a list of file URIs per session
  - [UI] Show Pause/Resume only when `capabilities.supportsPause || simulated`
- DoD
  - UI correctly reflects supported vs simulated pause

A3 — US3: Stop and play last recording (shared playback API)
- Tasks
  - [S] Define `Player` API: `play(uri)`, `pause()`, `stop()`, `state: StateFlow<PlayerState>`
  - [UI] Wire Play button to last recording path from `Recorder`
- DoD
  - Last file can be played locally on both platforms

---

### Epic B — Android Recording Implementation

B1 — Android: Recorder implementation with MediaRecorder
- Tasks
  - [A] Permissions
    - Add `RECORD_AUDIO` to `AndroidManifest.xml`
  - [A] File strategy
    - Generate timestamped `rec_YYYYMMDD_HHMMSS.m4a` in app files dir
    - Write to temp file; rename on stop
  - [A] Implement `AndroidRecorder`
    - Setup `MediaRecorder`: MIC, AAC LC, MP4 container, sample/bit rates from config
    - Prepare, start, stop, release; update `state`
    - Compute metadata with `MediaMetadataRetriever` after stop
  - [A] Error handling
    - Map platform exceptions to `RecordingState.Error`
- DoD
  - Manual start/stop on device saves playable m4a

B2 — Android: Foreground Service + ongoing notification
- Tasks
  - [A] Manifest and service type
    - Add foreground service permission and type `android:foregroundServiceType="mediaRecording"` (or `mediaPlayback` if more appropriate for APIs)
  - [A] Service
    - `RecordingService` with binder to `AndroidRecorder`
    - Elevate to foreground on `start()`; show ongoing notification
  - [A] Notification channel
    - Create channel `recording` with low importance; persistent notification with content intent back to app
  - [A] Wake lock (optional for stability)
    - Acquire `PARTIAL_WAKE_LOCK` while recording; release on stop
  - [UI] Bind/unbind service; reflect service state in UI
- DoD
  - 5‑minute background/lock test passes; notification persists

B3 — Android: Pause/Resume support
- Tasks
  - [A] If API supports `MediaRecorder.pause()/resume()` (24+), call them and update `state`
  - [A] If not supported or unstable, implement segmentation
- DoD
  - Pause/resume works or is simulated with clear UI feedback

B4 — Android: Acceptance run & logs
- Tasks
  - [A] Add log statements at start/pause/resume/stop and error sites
  - [A] Battery sampling via `BatteryManager` at start/stop
  - [A] Run a 5‑minute locked test; capture Logcat, battery %, notification screenshot
  - [Docs] Update PRD Section 7 and 12 with findings
- DoD
  - Evidence attached to PRD; AC satisfied

---

### Epic C — iOS Recording Implementation

C1 — iOS: Recorder implementation with AVAudioRecorder
- Tasks
  - [i] Permissions
    - Ensure `NSMicrophoneUsageDescription` in `Info.plist`
  - [i] Audio session
    - Configure `AVAudioSession` category `.playAndRecord` or `.record`
    - Set preferred sample rate; `setActive(true)`
  - [i] Implement `IosRecorder`
    - Initialize `AVAudioRecorder` with AAC settings
    - Start/stop; update `state`; compute duration/size
  - [i] Interruptions
    - Observe `AVAudioSession` interruptions and route changes; update `state`
- DoD
  - Manual start/stop on device saves playable m4a

C2 — iOS: Lockscreen/Background behavior test and docs
- Tasks
  - [i] Run 5‑minute lock test under `.record` and `.playAndRecord`
  - [i] Capture device logs and observations
  - [S] Expose `RecorderCapabilities.canRecordWhileLocked: Boolean?` (null = unknown)
  - [Docs] Update PRD Sections 7 and 12 with results
- DoD
  - Clear, evidence‑based finding documented

C3 — iOS: Pause/Resume or segmentation
- Tasks
  - [i] If `AVAudioRecorder.pause()` is reliable, wire it; else segment files
- DoD
  - Feature parity with Android approach

C4 — iOS: Acceptance run & logs
- Tasks
  - [i] Log start/pause/resume/stop and basic battery deltas (manual note permitted)
  - [Docs] Append outcomes to PRD
- DoD
  - Evidence recorded; AC satisfied

---

### Epic D — Fallback Mode (Keep Screen Awake)

D1 — Android: Wake lock / Keep screen on
- Tasks
  - [A] Activity flag `FLAG_KEEP_SCREEN_ON` while recording (cheap path)
  - [A] Optionally `PowerManager.PARTIAL_WAKE_LOCK` in service for CPU wakefulness
  - [UI] Toggle: "Keep screen awake while recording"; default off; persist in shared prefs
  - [UI] In‑app banner warning about battery usage
- DoD
  - Toggle works; resources released on Stop or on lifecycle end

D2 — iOS: Disable idle timer
- Tasks
  - [i] Set `UIApplication.shared.isIdleTimerDisabled = true` while recording; reset on stop
  - [UI] Toggle mirroring Android for consistency; in‑app indicator shown
- DoD
  - Device does not auto‑lock during recording when toggle is on

---

### Epic E — Permissions, Errors, and Interruption Handling

E1 — Microphone permission flow
- Tasks
  - [A] Request via `ActivityResultContracts.RequestPermission`
  - [i] Request via `AVAudioSession.requestRecordPermission`
  - [S] Shared `PermissionState` and UI handling of rationale/denied states
- DoD
  - Denied state disables Record and shows rationale message

E2 — Interruption handling
- Tasks
  - [A] Audio focus listener and phone state awareness; auto‑pause on loss; attempt resume
  - [i] Handle `AVAudioSession` interruption begin/end; update state and allow resume
- DoD
  - No crashes; recording remains playable even across interruptions

---

### Epic F — Playback and File Management

F1 — Basic playback of last recording
- Tasks
  - [A] Minimal `MediaPlayer` wrapper with callbacks
  - [i] Minimal `AVAudioPlayer` wrapper with callbacks
  - [UI] Play/Pause/Stop wiring; reflect player state
- DoD
  - Last file plays on both platforms

F2 — Minimal storage metadata
- Tasks
  - [S] `LastRecordingRepository` storing uri, timestamp, duration, size
  - [S] Expect/actual helpers for paths and sizes
- DoD
  - Metadata logged after Stop; last file remembered across app restarts (optional)

---

### Epic G — Observability and Measurement

G1 — Battery and session metrics
- Tasks
  - [S] Logging utility with `[REC]` prefix and timestamps
  - [A] Sample battery via `BatteryManager`
  - [i] If needed, record manual battery % in test notes
- DoD
  - Start/stop durations and battery deltas visible in logs

---

### Epic H — Documentation and Risk Register

H1 — Feasibility findings
- Tasks
  - [Docs] Add findings to PRD Section 7 (Platform Considerations) and 12 (Deliverables)
- DoD
  - PRD updated with screenshots/log excerpts and final statements

H2 — Risk register and recommendations
- Tasks
  - [Docs] Update risks R1–R5 with observed behavior; add mitigations and recommendations
- DoD
  - Risks reflect reality; guidance included for defaults and review readiness

---

### Iteration Breakdown (Phase 1)
- Iter 1 (Days 1–2): A1, A3, F1, F2
- Iter 2 (Days 3–4): B1, C1, E1, G1
- Iter 3 (Days 5–6): B2, C2, B3/C3, D1/D2
- Iter 4 (Days 7–8): E2, B4/C4, H1/H2, internal demo

---

### Cross‑Epic Dependencies
- A1 → B1, C1 → B2/C2 → B4/C4 → H1/H2
- A2 → B3, C3
- A3 → F1
- D1/D2 parallel to B2/C2; fallback path if iOS background recording limited
- E1 precedes meaningful platform tests

---

### Notes
- Keep the spike code simple and explicit; avoid premature abstractions.
- Defer segment concatenation; playback stitching is acceptable for the spike.
- Always test lock/background behavior on real devices; simulators are not representative for audio session behavior.
