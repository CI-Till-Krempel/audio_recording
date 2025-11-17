### Backlog and User Stories

This backlog is derived from `PRD.md` and grouped into epics. Each story includes acceptance criteria (AC), dependencies, risks, and an estimate. Detailed implementation tasks are documented in `../plans/PLANS.md`.

---

### Epic A — Core KMP Recorder API and Minimal UI

- A1 — US1: Start a recording (core)
  - AC
    - Tapping Record starts recording.
    - Elapsed time displays while recording.
    - Stopping exposes a file path/metadata.
      - Note: For the A1 spike, a fake/placeholder recording is acceptable and may not write a real audio file. Real audio capture and persistent file saving are implemented in Epics B (Android) and C (iOS).
  - Dependencies: none
  - Risks: API churn; keep thin and adaptable.
  - Estimate: S

- A2 — US2: Pause/resume recording (core behavior)
  - AC
    - Pause/Resume available when the platform supports it; otherwise simulated via segmented files with seamless playback timeline.
  - Dependencies: A1
  - Risks: Segment stitching at playback time.
  - Estimate: S

- A3 — US3: Stop and play last recording (UI + shared playback API)
  - AC
    - After Stop, Play button enables; plays last file with basic controls (Play/Pause/Stop).
  - Dependencies: A1
  - Risks: Cross‑platform player API differences.
  - Estimate: S

---

### Epic B — Android Recording Implementation

- B1 — Android: Recorder implementation with MediaRecorder
  - AC
    - Recording works on Android; file saved as AAC m4a; minimal error handling.
  - Dependencies: A1
  - Risks: API 29+ specifics; test on API 33–34.
  - Estimate: S

- B2 — Android: Foreground Service + ongoing notification (Lockscreen behavior)
  - AC
    - Recording continues while app is backgrounded/locked for ≥ 5 minutes.
    - Persistent notification visible; tapping returns to app.
  - Dependencies: B1
  - Risks: Android 14 foreground limits; use correct service type.
  - Estimate: M

- B3 — Android: Pause/Resume support
  - AC
    - Use native pause/resume if available; otherwise segment strategy.
  - Dependencies: B1, A2
  - Risks: Gaps between segments; communicate in UI if simulated.
  - Estimate: S

- B4 — Android: Acceptance run & logs
  - AC
    - Logs show start/pause/resume/stop, battery % start/end; 5‑minute locked test passes with notification.
  - Dependencies: B2
  - Risks: OEM background policies; test on Pixel/API 34 when possible.
  - Estimate: XS

---

### Epic C — iOS Recording Implementation

- C1 — iOS: Recorder implementation with AVAudioRecorder
  - AC
    - Recording works on iOS; file saved as AAC m4a; minimal error handling.
  - Dependencies: A1
  - Risks: Permissions/session setup edge cases; must test on device.
  - Estimate: M

- C2 — iOS: Lockscreen/Background behavior test and documentation
  - AC
    - Attempt recording, lock device; observe for ≥ 5 minutes and document in PRD.
    - If constrained, show messaging and recommend fallback.
  - Dependencies: C1
  - Risks: iOS policy limitations; simulator not representative.
  - Estimate: S

- C3 — iOS: Pause/Resume support or segmentation
  - AC
    - Provide Pause/Resume if supported; otherwise segmentation approach.
  - Dependencies: C1, A2
  - Risks: Seamless playback across segments; handle at playback time.
  - Estimate: XS

- C4 — iOS: Acceptance run & logs
  - AC
    - Evidence logs for start/pause/resume/stop, battery deltas; 5‑minute lock test outcome recorded.
  - Dependencies: C2
  - Risks: Limited battery APIs; allow manual note if necessary.
  - Estimate: XS

---

### Epic D — Fallback Mode (Keep Screen Awake)

- D1 — Android: Wake lock / Keep screen on
  - AC
    - Toggle or automatic fallback keeps CPU/screen awake during recording; disables on Stop.
  - Dependencies: B2 preferred; works standalone if background fails.
  - Risks: Battery drain; show banner/warning while active.
  - Estimate: S

- D2 — iOS: Disable idle timer
  - AC
    - While recording, `UIApplication.shared.isIdleTimerDisabled = true`; reset to false on Stop.
  - Dependencies: C1
  - Risks: Battery impact; show in‑app indicator.
  - Estimate: XS

---

### Epic E — Permissions, Errors, and Interruption Handling

- E1 — Microphone permission flow (both platforms)
  - AC
    - On first attempt, request permission; on denial, show rationale and disable Record until granted.
  - Dependencies: A1 UI wiring
  - Risks: Platform nuances; thread‑safe UI updates.
  - Estimate: S

- E2 — Interruption handling
  - AC
    - Incoming call, route change: gracefully pause; allow resume; no crash; file remains playable.
  - Dependencies: B1, C1
  - Risks: Edge conditions; document observed behaviors.
  - Estimate: M

---

### Epic F — Playback and File Management

- F1 — Basic playback of last recording (per platform)
  - AC
    - Play/Pause/Stop works for last file; UI reflects state.
  - Dependencies: A3
  - Risks: Audio focus on Android; keep simple.
  - Estimate: XS

- F2 — Minimal storage metadata
  - AC
    - After Stop, log duration and size; retain reference to last recording path.
  - Dependencies: B1, C1
  - Risks: Cross‑platform paths; normalize via expect/actual helpers.
  - Estimate: XS

---

### Epic G — Observability and Measurement

- G1 — Battery and session metrics
  - AC
    - Logs include start/stop timestamps, elapsed duration, and battery % delta.
  - Dependencies: Any recording run
  - Risks: iOS battery % limitations; accept manual note.
  - Estimate: XS

---

### Epic H — Documentation and Risk Register

- H1 — Feasibility findings (PRD updates)
  - AC
    - PRD updated with platform behavior evidence, API usage, and limitations.
  - Dependencies: B4, C4
  - Risks: None
  - Estimate: XS

- H2 — Risk register and recommendations
  - AC
    - R1–R5 updated with observed realities; mitigation plan captured.
  - Dependencies: H1
  - Risks: None
  - Estimate: XS

---

### Phase 1 (Spike) Iteration Plan (1–2 weeks)
- Iteration 1 (Days 1–2): A1, A3, F1, F2
- Iteration 2 (Days 3–4): B1, C1, E1, G1
- Iteration 3 (Days 5–6): B2, C2, B3/C3, D1/D2
- Iteration 4 (Days 7–8): E2, B4/C4, H1/H2

### Phase 2 (Productization) Stories (2–4 weeks)
- P2.1 — Storage abstraction and list of recordings (Est: M)
- P2.2 — Error handling and resilience (Est: M)
- P2.3 — UX polish & indicators (Est: S)
- P2.4 — Settings (format/quality) (Est: S)
- P2.5 — QA matrix and device sweep (Est: S)
- P2.6 — Privacy/security review and copy (Est: XS)
