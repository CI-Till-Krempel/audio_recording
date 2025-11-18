package de.cologneintelligence.audio_recording.recorder

import kotlinx.coroutines.flow.StateFlow

/**
 * Core recording configuration and API (Epic A).
 */

/**
 * Basic configuration for a recording session. Implementations may ignore
 * options that are not supported on a given platform during the spike phase.
 */
data class RecordingConfig(
    val sampleRateHz: Int = 44100,
    val bitRateBps: Int = 128_000,
    val format: AudioFormat = AudioFormat.AAC,
    val container: AudioContainer = AudioContainer.MP4,
)

enum class AudioFormat { AAC }

enum class AudioContainer { MP4 }

/**
 * State of the recorder.
 *
 * A2 semantics:
 * - Idle: no active session
 * - Recording(elapsedMs): actively recording; [elapsedMs] increases over time
 * - Paused(elapsedMs): session paused; [elapsedMs] is frozen and does not increase
 * - Error: terminal error description; behavior after error is implementation-defined in spike, but
 *   invalid-operation calls should prefer returning failures without changing the state
 */
sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val elapsedMs: Long) : RecordingState
    data class Paused(val elapsedMs: Long) : RecordingState
    data class Error(val message: String, val cause: Throwable? = null) : RecordingState
}

/**
 * Result details of a finished recording.
 */
data class RecordingResult(
    val uri: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMs: Long,
    val bytes: Long,
)

/**
 * Capabilities of a concrete recorder implementation.
 *
 * - [supportsPause]: if false, Pause/Resume should be disabled in the UI. A2 allows
 *   simulated pause via segmentation later, but the spike keeps it simple.
 */
data class RecorderCapabilities(
    val supportsPause: Boolean,
)

/**
 * Recorder API.
 *
 * A2 state machine and rules:
 * - Allowed transitions: Idle→Recording, Recording→Paused, Paused→Recording, (Recording|Paused)→Idle via [stop].
 * - All other calls return Result.failure with a descriptive message; state remains unchanged.
 * - Elapsed timing increases only in Recording; is frozen in Paused; new [start] resets elapsed to 0.
 */
interface Recorder {
    /** Hot state stream reflecting current [RecordingState]. */
    val state: StateFlow<RecordingState>

    /** Static capability flags for the implementation. */
    val capabilities: RecorderCapabilities

    /**
     * Starts a new recording from Idle. Returns failure if called from any other state.
     */
    suspend fun start(config: RecordingConfig): Result<Unit>

    /** Pauses an active recording. Returns failure if not currently Recording. */
    suspend fun pause(): Result<Unit>

    /** Resumes a paused recording. Returns failure if not currently Paused. */
    suspend fun resume(): Result<Unit>

    /**
     * Stops the current session and returns a [RecordingResult].
     * Returns failure if there is no active session (i.e., state is Idle or Error).
     */
    suspend fun stop(): Result<RecordingResult>
}
