package de.cologneintelligence.audio_recording.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * Minimal shared playback API for A3/F1.
 */

/**
 * State of the player.
 *
 * Semantics:
 * - Idle: no media loaded
 * - Paused(positionMs): media loaded and paused at [positionMs]
 * - Playing(positionMs): actively playing; [positionMs] increases over time until [durationMs]
 * - Completed: playback reached the end; next Play should restart from 0
 * - Error: terminal error for the current session
 */
sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Paused(val positionMs: Long, val durationMs: Long) : PlaybackState
    data class Playing(val positionMs: Long, val durationMs: Long) : PlaybackState
    data object Completed : PlaybackState
    data class Error(val message: String, val cause: Throwable? = null) : PlaybackState
}

/**
 * Player API. Keeps transitions intentionally simple for spike:
 * - load(uri, duration): Idle|Completed|Error → Paused(0)
 * - play(): Paused → Playing; Completed → Playing from 0
 * - pause(): Playing → Paused
 * - stop(): Playing|Paused → Paused(0)
 * Invalid operations return Result.failure and keep state unchanged.
 */
interface Player {
    val state: StateFlow<PlaybackState>

    suspend fun load(uri: String, durationMs: Long): Result<Unit>

    suspend fun play(): Result<Unit>

    suspend fun pause(): Result<Unit>

    suspend fun stop(): Result<Unit>
}
