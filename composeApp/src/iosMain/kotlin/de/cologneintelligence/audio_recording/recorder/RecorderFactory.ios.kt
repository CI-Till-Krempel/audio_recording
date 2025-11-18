package de.cologneintelligence.audio_recording.recorder

actual fun createRecorder(): Recorder = FakeRecorder()
