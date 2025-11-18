package de.cologneintelligence.audio_recording.recorder

import de.cologneintelligence.audio_recording.AndroidAppContext
import java.io.File

actual fun listRecordings(): List<RecordingFile> {
    val ctx = AndroidAppContext.context
    val dir = File(ctx.cacheDir, "recordings")
    if (!dir.exists() || !dir.isDirectory) return emptyList()
    val files = dir.listFiles() ?: return emptyList()
    return files
        .filter { it.isFile && it.name.endsWith(".m4a", ignoreCase = true) }
        .map { f ->
            RecordingFile(
                uri = f.absolutePath,
                name = f.name,
                bytes = f.length(),
                lastModifiedMillis = f.lastModified()
            )
        }
}
