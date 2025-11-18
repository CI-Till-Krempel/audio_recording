package de.cologneintelligence.audio_recording.recorder

import android.util.Log

/**
 * B4: Structured session logging for acceptance evidence. Outputs one JSON line per event.
 * Filter in Logcat by tag = TAG.
 */
internal object RecorderSessionLogger {
    private const val TAG = "RecorderB4"

    private inline fun log(build: StringBuilder.() -> Unit) {
        val sb = StringBuilder()
        sb.append('{')
        sb.build()
        sb.append('}')
        Log.i(TAG, sb.toString())
    }

    fun serviceStart() = log {
        kv("event", "service_start"); comma(); kv("ts", now())
    }

    fun serviceStop() = log {
        kv("event", "service_stop"); comma(); kv("ts", now())
    }

    fun start(sessionId: String, filePath: String, sampleRate: Int, bitRate: Int, batteryStart: Int?) = log {
        kv("event", "start"); comma();
        kv("ts", now()); comma();
        kv("sessionId", sessionId); comma();
        kv("filePath", filePath); comma();
        kv("sampleRate", sampleRate); comma();
        kv("bitRate", bitRate); comma();
        if (batteryStart != null) kv("batteryStart", batteryStart)
    }

    fun pause(sessionId: String, elapsedMs: Long) = log {
        kv("event", "pause"); comma(); kv("ts", now()); comma(); kv("sessionId", sessionId); comma(); kv("elapsedMs", elapsedMs)
    }

    fun resume(sessionId: String, elapsedMs: Long) = log {
        kv("event", "resume"); comma(); kv("ts", now()); comma(); kv("sessionId", sessionId); comma(); kv("elapsedMs", elapsedMs)
    }

    fun stop(sessionId: String, elapsedMs: Long, bytes: Long, batteryEnd: Int?, batteryDelta: Int?) = log {
        kv("event", "stop"); comma(); kv("ts", now()); comma(); kv("sessionId", sessionId); comma(); kv("elapsedMs", elapsedMs); comma(); kv("bytes", bytes)
        if (batteryEnd != null) { comma(); kv("batteryEnd", batteryEnd) }
        if (batteryDelta != null) { comma(); kv("batteryDelta", batteryDelta) }
    }

    fun error(sessionId: String?, message: String) = log {
        kv("event", "error"); comma(); kv("ts", now());
        if (sessionId != null) { comma(); kv("sessionId", sessionId) }
        comma(); kv("message", message)
    }

    // Helpers
    private fun StringBuilder.kv(key: String, value: String) {
        append('"').append(escape(key)).append('"').append(':').append('"').append(escape(value)).append('"')
    }
    private fun StringBuilder.kv(key: String, value: Int) {
        append('"').append(escape(key)).append('"').append(':').append(value)
    }
    private fun StringBuilder.kv(key: String, value: Long) {
        append('"').append(escape(key)).append('"').append(':').append(value)
    }
    private fun StringBuilder.comma() { append(',') }
    private fun now(): Long = System.currentTimeMillis()
    private fun escape(s: String): String = buildString(s.length) {
        for (ch in s) when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
}
