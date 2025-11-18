package de.cologneintelligence.audio_recording.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.time.TimeSource

/**
 * Simple in-memory player for A3/F1.
 * Simulates progression of playback time, no real audio.
 */
class FakePlayer : Player {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val state: StateFlow<PlaybackState> = _state

    private var ticker: Job? = null
    private var lastTick = TimeSource.Monotonic.markNow()
    private var loadedUri: String? = null
    private var loadedDuration: Long = 0L

    override suspend fun load(uri: String, durationMs: Long): Result<Unit> = runCatching {
        stopTicker()
        loadedUri = uri
        loadedDuration = maxOf(0, durationMs)
        _state.value = PlaybackState.Paused(positionMs = 0, durationMs = loadedDuration)
    }

    override suspend fun play(): Result<Unit> = runCatching {
        val s = _state.value
        when (s) {
            is PlaybackState.Paused -> {
                lastTick = TimeSource.Monotonic.markNow()
                _state.value = PlaybackState.Playing(s.positionMs, s.durationMs)
                startTicker()
            }
            is PlaybackState.Completed -> {
                // restart from 0 if media loaded
                if (loadedUri != null) {
                    lastTick = TimeSource.Monotonic.markNow()
                    _state.value = PlaybackState.Playing(0, loadedDuration)
                    startTicker()
                } else error("No media loaded")
            }
            else -> error("Invalid transition: play() from ${s::class.simpleName}")
        }
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        val s = _state.value
        if (s is PlaybackState.Playing) {
            val delta = lastTick.elapsedNow().inWholeMilliseconds
            val pos = min(s.durationMs, s.positionMs + delta)
            stopTicker()
            _state.value = PlaybackState.Paused(pos, s.durationMs)
        } else error("Invalid transition: pause() from ${s::class.simpleName}")
    }

    override suspend fun stop(): Result<Unit> = runCatching {
        val s = _state.value
        when (s) {
            is PlaybackState.Playing, is PlaybackState.Paused -> {
                stopTicker()
                _state.value = PlaybackState.Paused(0, loadedDuration)
            }
            else -> error("Invalid transition: stop() from ${s::class.simpleName}")
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(100)
                val s = _state.value
                if (s is PlaybackState.Playing) {
                    val base = s.positionMs
                    val delta = lastTick.elapsedNow().inWholeMilliseconds
                    val next = min(s.durationMs, base + delta)
                    lastTick = TimeSource.Monotonic.markNow()
                    if (next >= s.durationMs) {
                        _state.value = PlaybackState.Completed
                    } else {
                        _state.value = PlaybackState.Playing(next, s.durationMs)
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    fun close() {
        stopTicker()
        scope.cancel()
    }
}
