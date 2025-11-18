package de.cologneintelligence.audio_recording.recorder

import araudiokit.ARAudioRecorder
import araudiokit.ARRecordingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import platform.Foundation.NSDate

/**
 * iOS recorder backed by the Swift wrapper (ARAudioRecorder).
 * Note: The iOS factory will be switched to use this implementation
 * only after on-device verification of C1.
 */
class IosRecorder : Recorder {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state

    override val capabilities: RecorderCapabilities = RecorderCapabilities(supportsPause = true)

    private var ticker: Job? = null
    private var startedAtMillis: Long = 0L
    private var swift: ARAudioRecorder? = null

    override suspend fun start(config: RecordingConfig): Result<Unit> = runCatching {
        check(_state.value is RecordingState.Idle) { "start only allowed from Idle" }
        val rec = ARAudioRecorder()
        swift = rec
        // Start Swift recorder
        rec.start(sampleRate = config.sampleRateHz.toInt(), bitRate = config.bitRateBps.toInt())
        startedAtMillis = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
        _state.value = RecordingState.Recording(0)
        // Start a lightweight ticker that queries elapsed from Swift
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(200)
                val elapsed: Long = try { swift?.currentElapsedMs() ?: 0L } catch (_: Throwable) { 0L }
                _state.value = RecordingState.Recording(elapsed)
            }
        }
    }.onFailure {
        _state.value = RecordingState.Error("Failed to start: ${it.message}", it)
        ticker?.cancel()
        swift = null
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        val current = _state.value
        require(current is RecordingState.Recording) { "pause only from Recording" }
        swift?.pause() ?: error("Recorder not initialized")
        ticker?.cancel()
        val elapsed = try { swift?.currentElapsedMs() ?: 0L } catch (_: Throwable) { 0L }
        _state.value = RecordingState.Paused(elapsed)
    }.onFailure {
        _state.value = RecordingState.Error("Failed to pause: ${it.message}", it)
    }

    override suspend fun resume(): Result<Unit> = runCatching {
        val current = _state.value
        require(current is RecordingState.Paused) { "resume only from Paused" }
        val rec = swift ?: error("Recorder not initialized")
        rec.resume()
        _state.value = RecordingState.Recording(current.elapsedMs)
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(200)
                val elapsed: Long = try { swift?.currentElapsedMs() ?: 0L } catch (_: Throwable) { 0L }
                _state.value = RecordingState.Recording(elapsed)
            }
        }
    }.onFailure {
        _state.value = RecordingState.Error("Failed to resume: ${it.message}", it)
    }

    override suspend fun stop(): Result<RecordingResult> = runCatching {
        val s = _state.value
        if (s !is RecordingState.Recording && s !is RecordingState.Paused) error("stop requires active session")
        ticker?.cancel()
        val rec = swift ?: error("Recorder not initialized")
        val result: ARRecordingResult = rec.stop()
        swift = null
        val endedAt = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
        _state.value = RecordingState.Idle
        val urlStr = result.url?.path ?: result.url?.absoluteString ?: ""
        RecordingResult(
            uri = urlStr,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAt,
            durationMs = result.durationMs,
            bytes = result.bytes,
        )
    }.onFailure {
        _state.value = RecordingState.Error("Failed to stop: ${it.message}", it)
    }
}
