# Epic B — Story B4: Android Acceptance Run & Logs

This document describes the acceptance test process, prerequisites, and expected outcomes for Epic B, Story B4 (Android acceptance run with structured logs and a 5‑minute lock‑screen test).

## Objective
Demonstrate that recording continues reliably in the foreground, background, and while the device is locked for ≥ 5 minutes. Capture structured logs including session lifecycle events and battery percentage at the start and end of a session.

## Test Requirements
- Device: Android phone or emulator (physical device recommended), Android 29–34; Pixel/API 34 preferred.
- Permissions:
  - Microphone: RECORD_AUDIO
  - Android 13+ (API 33+): POST_NOTIFICATIONS (recommended for visible notification)
- Build: Debug build (structured logs are emitted in debug; they use Logcat tag `RecorderB4`).
- Time budget: ~10–15 minutes (including ≥5 minutes locked).

## What is Logged
Structured JSON lines to Logcat with tag `RecorderB4`:
- Events: `service_start`, `service_stop`, `start`, `pause`, `resume`, `stop`, and `error`.
- Fields:
  - `ts`: wall‑clock timestamp (ms since epoch)
  - `sessionId`: string identifier for the session (on start/resume/pause/stop)
  - `filePath` (start)
  - `sampleRate`, `bitRate` (start)
  - `batteryStart` (start) — nullable
  - `elapsedMs` (pause/resume/stop)
  - `bytes` (stop)
  - `batteryEnd`, `batteryDelta` (stop) — nullable

### Example log lines
```
{"event":"service_start","ts":1731927800000}
{"event":"start","ts":1731927840000,"sessionId":"20251118_1104-ab12","filePath":"/data/user/0/.../REC_20251118_110400.m4a","sampleRate":44100,"bitRate":128000,"batteryStart":87}
{"event":"pause","ts":1731927855000,"sessionId":"20251118_1104-ab12","elapsedMs":15000}
{"event":"resume","ts":1731927860000,"sessionId":"20251118_1104-ab12","elapsedMs":15000}
{"event":"stop","ts":1731928160000,"sessionId":"20251118_1104-ab12","elapsedMs":300000,"bytes":5234567,"batteryEnd":85,"batteryDelta":-2}
{"event":"service_stop","ts":1731928160100}
```

## Procedure
1. Install and launch the app on the test device.
2. Ensure permissions are granted:
   - Microphone (RECORD_AUDIO)
   - Android 13+: Notifications (POST_NOTIFICATIONS) so the ongoing notification is visible.
3. Open Logcat and filter by tag `RecorderB4`.
4. Tap Record:
   - Expect `service_start` and a `start` log with `sessionId`, `filePath`, `sampleRate`, `bitRate`, `batteryStart`.
5. After ~10–15 seconds, tap Pause:
   - Expect `pause` with `elapsedMs` ≈ time since start.
6. Tap Resume:
   - Expect `resume` with `elapsedMs` continuing from pause.
7. Press Home and lock the device. Leave locked for ≥ 5 minutes.
   - Ensure the ongoing notification remains visible (on devices where notification permission is granted).
8. Unlock the device, return to the app, and tap Stop:
   - Expect `stop` with `elapsedMs` ≈ total active time, `bytes` > 0, `batteryEnd`, and `batteryDelta`.
   - Expect `service_stop` soon after.

## Expected Outcomes
- Notification stays visible throughout the background/lock period.
- The produced `.m4a` file appears in `<app cache>/recordings` and is listed in the app’s Recordings section.
- Logged `elapsedMs` is plausible (close to real time considering pauses).
- Battery readings may be absent on some devices; in that case, fields are omitted (acceptable).

## Evidence to Capture
- Copy the Logcat excerpt for the session (`RecorderB4` + the specific `sessionId`).
- Note device model, Android version, and wall‑clock start/stop times.
- Optionally, verify the file plays back and note approximate duration.

## Notes & Limitations
- If notifications are disabled, the foreground service runs but the notification may be hidden; record this in notes.
- Some OEMs may behave differently with background restrictions; the foreground service uses `foregroundServiceType="microphone"` to comply with Android 14+ limits.
- Battery % may be coarse or unavailable; absence is acceptable for acceptance as long as runtime evidence is captured.
