### AGENTS: Lean Development Process (Agent‑Led, Single Maintainer)

This document defines a lightweight, outcome‑oriented process for the Audio Recording KMP spike executed by software agents with oversight from a single maintainer. It is optimized for speed, clarity, and evidence‑based decisions.

#### 2. Operating Principles
- Evidence over speculation: Ship spike code to real devices early and measure.
- Small batches: Time‑box spikes (days), merge behind flags, iterate.
- Clear definitions: Each story has acceptance criteria and a crisp demo.
- Default to simplicity: Prefer the simplest API/tool that proves feasibility.
- Transparency: Capture key findings in PRD and DECISIONS.md (if needed) promptly.

#### 3. Workflow
1) Plan
- Agents read PRD goals and select the next highest‑value story.
- Split into small tasks with explicit acceptance criteria.
- Identify risks and the quickest validation path.

2) Build
- Implement the minimal vertical slice to validate the target.
- Instrument with logs for observability during tests.
- Feature‑flag or keep changes small and reversible.

Commit policy:
- Commit only when a story is complete according to its Definition of Done (DoD).
- Each story completion commit must include a clear message summarizing:
  - What was implemented (scope),
  - Evidence of acceptance (e.g., “on-device test passed; playable file saved”),
  - Any notable trade‑offs or follow‑ups.
  Example: "C1 — iOS AVAudioRecorder implemented via Swift wrapper; start/stop verified on device; files saved to Caches/recordings; factory switched to IosRecorder."

3) Verify
- Run on at least one Android device (API 33–34) and one iOS device.
- Validate acceptance criteria and record outcome (pass/fail, notes).

4) Document
- Update PRD.md findings, risks, and recommendations.
- Add short notes in a CHANGELOG or DECISIONS.md for non‑obvious tradeoffs.

Agents automatically attach logs, screenshots, and sample artefacts (e.g., audio files) to the story for maintainer review.

#### 4. Branching & CI
- Branching: `feat/<short-topic>` for dev; rebase on main frequently.
- CI: Lint/build checks must pass; fast unit test suite when added.

#### 5. Definition of Ready (DoR)
A story is Ready when:
- Clear goal and acceptance criteria.
- Dependencies/permissions identified.
- Device(s) available for test.

#### 6. Definition of Done (DoD)
A story is Done when:
- Acceptance criteria pass on device(s) with evidence (screens/logs).
- No crash in a 5–10 minute manual session.
- PRD updated (if findings); code merged; optional feature flag off if safe.

#### 8. Quality & Risk Management
- Manual sanity matrix: Android 26/30/34, iOS latest ‑ 1 where possible.
- Log key events (start/pause/resume/stop, errors, battery %).
- Early risk spikes: background/lock behavior, permissions, battery.

#### 9. Artifacts
- Keep artifacts close to code: PRD.md, DECISIONS.md, and README links.

#### 10. Tooling
- Kotlin MPP + Compose Multiplatform UI, `MediaRecorder/AudioRecord` (Android), `AVAudioRecorder` (iOS).
- Lightweight scripts for battery sampling and log capture during tests.
