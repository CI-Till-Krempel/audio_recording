package de.cologneintelligence.audio_recording.recorder

/**
 * Platform factory for obtaining a [Recorder] implementation.
 * - Android: MediaRecorder-backed implementation (Epic B1)
 * - Other platforms: falls back to [FakeRecorder] during spike
 */
expect fun createRecorder(): Recorder
