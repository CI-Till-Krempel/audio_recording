package de.cologneintelligence.audio_recording.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.cologneintelligence.audio_recording.AndroidAppContext
import de.cologneintelligence.audio_recording.MainActivity

/**
 * Lightweight foreground service used to keep the app alive while recording
 * continues in the background and on the lock screen (Epic B2).
 */
class RecordingForegroundService : android.app.Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(this)
        // Notification ID must be stable so updates replace it
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "recorder_foreground"
        private const val CHANNEL_NAME = "Recording"
        private const val NOTIFICATION_ID = 1001

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
                val existing = nm?.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Ongoing notification while recording"
                        setShowBadge(false)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    }
                    nm?.createNotificationChannel(channel)
                }
            }
        }

        fun start(context: Context) {
            ensureChannel(context)
            val intent = Intent(context, RecordingForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java)
            context.stopService(intent)
        }

        private fun buildNotification(context: Context): Notification {
            ensureChannel(context)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val piFlags = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            val contentPI = PendingIntent.getActivity(context, 0, openAppIntent, piFlags)

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Recording in progress")
                .setContentText("Tap to return to the app")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentPI)
                .build()
        }
    }
}
