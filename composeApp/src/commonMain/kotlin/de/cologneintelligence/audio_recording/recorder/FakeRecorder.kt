package de.cologneintelligence.audio_recording.recorder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.TimeSource

/**
 * Simple in-memory recorder used for initial UI wiring (A1). No real audio is recorded.
 * It simulates elapsed time and produces a fake file path on stop.
 */
class FakeRecorder : Recorder {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state

    override val capabilities: RecorderCapabilities = RecorderCapabilities(supportsPause = true)

    private var tickerJob: Job? = null
    private var lastTickMark = TimeSource.Monotonic.markNow()

    override suspend fun start(config: RecordingConfig): Result<Unit> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Idle) {
            error("Invalid transition: start() allowed only from Idle, was ${current::class.simpleName}")
        }
        lastTickMark = TimeSource.Monotonic.markNow()
        _state.value = RecordingState.Recording(elapsedMs = 0)
        startTicker()
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Recording) {
            error("Invalid transition: pause() allowed only from Recording, was ${current::class.simpleName}")
        }
        val elapsed = elapsedSinceStart()
        _state.value = RecordingState.Paused(elapsed)
        stopTicker()
    }

    override suspend fun resume(): Result<Unit> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Paused) {
            error("Invalid transition: resume() allowed only from Paused, was ${current::class.simpleName}")
        }
        // resume ticking from current elapsed; no change to startedAt
        lastTickMark = TimeSource.Monotonic.markNow()
        _state.value = RecordingState.Recording(current.elapsedMs)
        startTicker()
    }

    override suspend fun stop(): Result<RecordingResult> = runCatching {
        val current = _state.value
        val duration = when (current) {
            is RecordingState.Recording -> current.elapsedMs
            is RecordingState.Paused -> current.elapsedMs
            is RecordingState.Idle, is RecordingState.Error -> error(
                "Invalid transition: stop() requires active session, was ${current::class.simpleName}"
            )
        }
        stopTicker()
        _state.value = RecordingState.Idle
        // produce a fake file path
        RecordingResult(
            uri = "fake://rec_${duration}.m4a",
            startedAtMillis = 0L,
            endedAtMillis = duration,
            durationMs = duration,
            bytes = max(1L, duration / 10) // fake size ~100B per second
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(100)
                val current = _state.value
                if (current is RecordingState.Recording) {
                    val base = current.elapsedMs
                    val delta = lastTickMark.elapsedNow().inWholeMilliseconds
                    _state.value = RecordingState.Recording(base + delta)
                    lastTickMark = TimeSource.Monotonic.markNow()
                } else {
                    // paused or other, stop ticking
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun close() {
        stopTicker()
        scope.cancel()
    }

    private fun elapsedSinceStart(): Long {
        val current = _state.value
        return when (current) {
            is RecordingState.Recording -> current.elapsedMs
            is RecordingState.Paused -> current.elapsedMs
            else -> 0L
        }
    }
}