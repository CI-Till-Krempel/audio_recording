### AGENTS: Lean Development Process

This document describes a lightweight, outcome‑oriented process for developing the Audio Recording KMP spike and its path to productization. It is optimized for speed, clarity, and evidence‑based decisions.

#### 1. Roles (caps may be combined in a small team)
- Product Lead: Owns PRD, prioritization, acceptance criteria, and outcomes.
- Tech Lead: Owns architecture decisions, platform feasibility, and quality bar.
- Platform Engineers (Android/iOS): Implement and validate platform specifics.
- KMP Engineer: Shapes shared APIs and Compose Multiplatform UI.
- QA (Lightweight): Defines sanity matrix; validates acceptance.

#### 2. Operating Principles
- Evidence over speculation: Ship spike code to real devices early and measure.
- Small batches: Time‑box spikes (days), merge behind flags, iterate.
- Clear definitions: Each story has acceptance criteria and a crisp demo.
- Default to simplicity: Prefer the simplest API/tool that proves feasibility.
- Transparency: Capture key findings in PRD and DECISIONS.md (if needed) promptly.

#### 3. Workflow
1) Plan (60–90 minutes per iteration)
- Review PRD goals and pick the next highest‑value stories.
- Split into 1–2 day tasks with acceptance criteria.
- Identify risks and the quickest validation path.

2) Build
- Implement the minimal vertical slice to validate the target.
- Instrument with logs for observability during tests.
- Feature‑flag or keep changes small and reversible.

3) Verify
- Run on at least one Android device (API 33–34) and one iOS device.
- Validate acceptance criteria and record outcome (pass/fail, notes).

4) Document
- Update PRD.md findings, risks, and recommendations.
- Add short notes in a CHANGELOG or DECISIONS.md for non‑obvious tradeoffs.

5) Demo & Decide
- Short demo (≤10 min): show behavior, logs, and battery notes.
- Decide: proceed, adjust scope, or pivot to fallback.

#### 4. Branching, Reviews, CI
- Branching: `feat/<short-topic>` for dev; rebase on main frequently.
- Reviews: 1 reviewer minimum; approve with screenshots/logs for behavior.
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

#### 7. Estimation & Cadence
- Use T‑shirt sizes (XS ≤ 0.5d, S ≤ 1d, M ≤ 2d, L ≤ 4d). Avoid XL.
- Cadence: 2–3 day micro‑iterations; weekly checkpoint with demo.

#### 8. Quality & Risk Management
- Manual sanity matrix: Android 26/30/34, iOS latest ‑ 1 where possible.
- Log key events (start/pause/resume/stop, errors, battery %).
- Early risk spikes: background/lock behavior, permissions, battery.

#### 9. Communication
- Daily async update: yesterday/today/blockers.
- Keep artifacts close to code: PRD.md, DECISIONS.md, and README links.

#### 10. Tooling
- Kotlin MPP + Compose Multiplatform UI, `MediaRecorder/AudioRecord` (Android), `AVAudioRecorder` (iOS).
- Lightweight scripts for battery sampling and log capture during tests.
