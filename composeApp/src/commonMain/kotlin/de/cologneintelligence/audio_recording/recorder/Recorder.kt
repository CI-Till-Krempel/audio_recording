package de.cologneintelligence.audio_recording.recorder

import kotlinx.coroutines.flow.StateFlow

// Core models for recording (A1)

data class RecordingConfig(
    val sampleRateHz: Int = 44100,
    val bitRateBps: Int = 128_000,
    val format: AudioFormat = AudioFormat.AAC,
    val container: AudioContainer = AudioContainer.MP4,
)

enum class AudioFormat { AAC }

enum class AudioContainer { MP4 }

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val elapsedMs: Long) : RecordingState
    data class Paused(val elapsedMs: Long) : RecordingState
    data class Error(val message: String, val cause: Throwable? = null) : RecordingState
}

data class RecordingResult(
    val uri: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMs: Long,
    val bytes: Long,
)

data class RecorderCapabilities(
    val supportsPause: Boolean,
)

interface Recorder {
    val state: StateFlow<RecordingState>
    val capabilities: RecorderCapabilities

    suspend fun start(config: RecordingConfig): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun stop(): Result<RecordingResult>
}
