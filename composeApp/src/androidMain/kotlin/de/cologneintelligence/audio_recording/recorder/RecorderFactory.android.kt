package de.cologneintelligence.audio_recording.recorder

import de.cologneintelligence.audio_recording.AndroidAppContext

actual fun createRecorder(): Recorder {
    val ctx = AndroidAppContext.context
    return try {
        MediaRecorderRecorder(ctx)
    } catch (_: Throwable) {
        // Fallback to fake if something goes wrong during construction
        FakeRecorder()
    }
}
