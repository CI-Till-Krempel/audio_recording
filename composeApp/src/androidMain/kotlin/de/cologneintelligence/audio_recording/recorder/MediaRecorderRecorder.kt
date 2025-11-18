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
 * Epic B1: Minimal start/stop supporting AAC in MPEG-4 (.m4a). Pause/Resume not supported yet.
 */
class MediaRecorderRecorder(
    private val context: Context
) : Recorder {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = _state

    override val capabilities: RecorderCapabilities = RecorderCapabilities(supportsPause = false)

    private var tickerJob: Job? = null
    private var startedAt: Long = 0L
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

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
        _state.value = RecordingState.Recording(0)
        startTicker()
        // B2: Enter foreground to survive background/lock
        RecordingForegroundService.start(context)
    }.onFailure { e ->
        _state.value = RecordingState.Error("Failed to start recording: ${e.message}", e)
        releaseRecorder()
    }

    override suspend fun pause(): Result<Unit> = runCatching {
        error("Pause not supported in B1; capabilities.supportsPause = false")
    }

    override suspend fun resume(): Result<Unit> = runCatching {
        error("Resume not supported in B1; capabilities.supportsPause = false")
    }

    override suspend fun stop(): Result<RecordingResult> = runCatching {
        val current = _state.value
        if (current !is RecordingState.Recording && current !is RecordingState.Paused) {
            error("Invalid transition: stop() requires active session, was ${current::class.simpleName}")
        }
        stopTicker()
        val end = System.currentTimeMillis()
        val duration = (end - startedAt).coerceAtLeast(0)
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

        val f = outputFile ?: error("No output file")
        RecordingResult(
            uri = f.absolutePath,
            startedAtMillis = startedAt,
            endedAtMillis = end,
            durationMs = duration,
            bytes = f.length()
        )
    }.onFailure { e ->
        _state.value = RecordingState.Error("Failed to stop recording: ${e.message}", e)
        releaseRecorder()
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
            val startAt = startedAt
            while (true) {
                delay(100)
                val s = _state.value
                if (s is RecordingState.Recording) {
                    val elapsed = (System.currentTimeMillis() - startAt).coerceAtLeast(0)
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
    }

    fun close() {
        stopTicker()
        releaseRecorder()
        scope.cancel()
    }
}
