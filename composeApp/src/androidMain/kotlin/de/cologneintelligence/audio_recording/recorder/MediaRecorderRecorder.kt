package de.cologneintelligence.audio_recording.recorder

import android.content.Context
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android implementation using [MediaRecorder].
 * - B1: Start/Stop (AAC in MPEG-4 .m4a)
 * - B2: Foreground service while recording
 * - B3: Native Pause/Resume (API 24+)
 */
class MediaRecorderRecorder(
    private val context: Context
) : Recorder {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state

    // Min SDK is 24, where MediaRecorder.pause()/resume() are available
    override val capabilities: RecorderCapabilities = RecorderCapabilities(supportsPause = true)

    private var tickerJob: Job? = null
    private var startedAt: Long = 0L
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    // B3: Keep precise elapsed across pause/resume cycles
    private var accumulatedElapsedMs: Long = 0L
    private var activeStartAtMs: Long? = null
    // B4: Acceptance logging/session tracking
    private var sessionId: String? = null
    private var batteryStartPct: Int? = null

    override suspend fun start(config: RecordingConfig): Result<Unit> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Idle) {
            error("Invalid transition: start() allowed only from Idle, was ${current::class.simpleName}")
        }
        // Prepare output file in cache dir
        val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "REC_${name}.m4a")
        outputFile = file

        // Configure MediaRecorder
        val mr = MediaRecorder()
        mediaRecorder = mr
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        // Apply config best-effort
        mr.setAudioEncodingBitRate(config.bitRateBps)
        mr.setAudioSamplingRate(config.sampleRateHz)
        mr.setOutputFile(file.absolutePath)
        mr.prepare()
        mr.start()

        startedAt = System.currentTimeMillis()
        accumulatedElapsedMs = 0L
        activeStartAtMs = startedAt
        _state.value = RecordingState.Recording(0)
        startTicker()
        // B2: Enter foreground to survive background/lock
        RecordingForegroundService.start(context)
        // B4: Initialize session logging
        sessionId = generateSessionId(name)
        batteryStartPct = BatteryInfoProvider.getBatteryPercent(context)
        RecorderSessionLogger.start(
            sessionId = sessionId!!,
            filePath = file.absolutePath,
            sampleRate = config.sampleRateHz,
            bitRate = config.bitRateBps,
            batteryStart = batteryStartPct
        )
    }.onFailure { e ->
        _state.value = RecordingState.Error("Failed to start recording: ${e.message}", e)
        releaseRecorder()
        RecorderSessionLogger.error(sessionId, "start_failed: ${e.message}")
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Recording) {
            error("Invalid transition: pause() allowed only from Recording, was ${current::class.simpleName}")
        }
        // Invoke native pause
        mediaRecorder?.pause() ?: error("Recorder not initialized")
        // Accumulate elapsed up to now
        val now = System.currentTimeMillis()
        val activeStart = activeStartAtMs ?: now
        accumulatedElapsedMs += (now - activeStart).coerceAtLeast(0)
        activeStartAtMs = null
        stopTicker()
        _state.value = RecordingState.Paused(accumulatedElapsedMs)
        // Foreground service stays running while paused
        sessionId?.let { RecorderSessionLogger.pause(it, accumulatedElapsedMs) }
        Unit
    }.onFailure { e ->
        _state.value = RecordingState.Error("Failed to pause: ${e.message}", e)
        RecorderSessionLogger.error(sessionId, "pause_failed: ${e.message}")
    }

    override suspend fun resume(): Result<Unit> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Paused) {
            error("Invalid transition: resume() allowed only from Paused, was ${current::class.simpleName}")
        }
        // Invoke native resume
        mediaRecorder?.resume() ?: error("Recorder not initialized")
        activeStartAtMs = System.currentTimeMillis()
        _state.value = RecordingState.Recording(accumulatedElapsedMs)
        startTicker()
        // Foreground continues to run from start()
        sessionId?.let { RecorderSessionLogger.resume(it, accumulatedElapsedMs) }
        Unit
    }.onFailure { e ->
        _state.value = RecordingState.Error("Failed to resume: ${e.message}", e)
        RecorderSessionLogger.error(sessionId, "resume_failed: ${e.message}")
    }

    override suspend fun stop(): Result<RecordingResult> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Recording && current !is RecordingState.Paused) {
            error("Invalid transition: stop() requires active session, was ${current::class.simpleName}")
        }
        stopTicker()
        val end = System.currentTimeMillis()
        val effectiveElapsed = when (_state.value) {
            is RecordingState.Recording -> {
                val activeStart = activeStartAtMs ?: end
                accumulatedElapsedMs + (end - activeStart).coerceAtLeast(0)
            }
            is RecordingState.Paused -> accumulatedElapsedMs
            else -> accumulatedElapsedMs
        }
        val fileRef = outputFile
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Throwable) {
                    // Some OEMs throw if stop too quickly. Still proceed to release.
                }
            }
        } finally {
            releaseRecorder()
        }
        _state.value = RecordingState.Idle

        val f = fileRef ?: error("No output file")
        // B4 logging for stop
        val batteryEnd = BatteryInfoProvider.getBatteryPercent(context)
        val batteryDelta = if (batteryStartPct != null && batteryEnd != null) batteryEnd - batteryStartPct!! else null
        sessionId?.let {
            RecorderSessionLogger.stop(
                sessionId = it,
                elapsedMs = effectiveElapsed,
                bytes = f.length(),
                batteryEnd = batteryEnd,
                batteryDelta = batteryDelta
            )
        }
        RecordingResult(
            uri = f.absolutePath,
            startedAtMillis = startedAt,
            endedAtMillis = end,
            durationMs = effectiveElapsed,
            bytes = f.length()
        )
    }.onFailure { e ->
        _state.value = RecordingState.Error("Failed to stop recording: ${e.message}", e)
        releaseRecorder()
        RecorderSessionLogger.error(sessionId, "stop_failed: ${e.message}")
    }.also {
        // Ensure foreground service stops regardless of success/failure
        try {
            RecordingForegroundService.stop(context)
        } catch (_: Throwable) {
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val getElapsed: () -> Long = {
                val base = accumulatedElapsedMs
                val start = activeStartAtMs
                if (start != null) base + (System.currentTimeMillis() - start).coerceAtLeast(0)
                else base
            }
            while (true) {
                delay(100)
                val s = _state.value
                if (s is RecordingState.Recording) {
                    val elapsed = getElapsed()
                    _state.value = RecordingState.Recording(elapsed)
                } else {
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
        } catch (_: Throwable) {
        }
        try {
            mediaRecorder?.release()
        } catch (_: Throwable) {
        }
        mediaRecorder = null
        activeStartAtMs = null
    }

    fun close() {
        stopTicker()
        releaseRecorder()
        scope.cancel()
    }

    private fun generateSessionId(timestampPart: String): String {
        val rand = (0..0xFFFF).random()
        return "${timestampPart}-${rand.toString(16).padStart(4, '0')}"
    }
}
