package de.cologneintelligence.audio_recording

import android.content.Context

/**
 * Simple holder to provide an application [Context] to Android-only implementations
 * from common code factories.
 */
object AndroidAppContext {
    lateinit var context: Context
}
