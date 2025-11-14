package de.cologneintelligence.audio_recording

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform