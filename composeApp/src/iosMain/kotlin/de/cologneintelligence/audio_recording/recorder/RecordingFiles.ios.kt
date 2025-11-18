package de.cologneintelligence.audio_recording.recorder

// Spike: no iOS file persistence yet; return empty list for now.
actual fun listRecordings(): List<RecordingFile> = emptyList()
