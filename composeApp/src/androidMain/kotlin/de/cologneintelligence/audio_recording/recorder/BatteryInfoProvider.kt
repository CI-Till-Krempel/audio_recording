package de.cologneintelligence.audio_recording.recorder

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Provides current battery percentage on Android. May return null if not available.
 */
internal object BatteryInfoProvider {
    fun getBatteryPercent(context: Context): Int? {
        // Primary: BatteryManager property (API 21+)
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val prop = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (prop != null && prop in 0..100) return prop

        // Fallback: sticky broadcast
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) {
            val pct = (level * 100f / scale).toInt()
            if (pct in 0..100) return pct else return null
        }
        return null
    }
}
