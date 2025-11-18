package de.cologneintelligence.audio_recording.recorder

/**
 * Minimal representation of a persisted recording file discovered on the device.
 * Implementations should return absolute `uri` paths for now (e.g., file paths on Android).
 */
data class RecordingFile(
    val uri: String,
    val name: String,
    val bytes: Long,
    val lastModifiedMillis: Long,
)

/**
 * Returns all known recordings present on the device for the current platform, without
 * using any preferences or databases. Implementations should rely solely on file listing.
 *
 * Sorting is left to the caller; typical UI sorts by `lastModifiedMillis` descending.
 */
expect fun listRecordings(): List<RecordingFile>
