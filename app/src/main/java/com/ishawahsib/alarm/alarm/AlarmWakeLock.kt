package com.ishawahsib.alarm.alarm

import android.content.Context
import android.os.PowerManager

object AlarmWakeLock {
    private const val TAG = "IshaWahsib:AlarmWakeLock"

    @Volatile
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire(context: Context, timeoutMs: Long = 10 * 60 * 1000L) {
        synchronized(this) {
            if (wakeLock?.isHeld == true) return
            val pm = context.applicationContext.getSystemService(PowerManager::class.java) ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG).apply {
                setReferenceCounted(false)
                acquire(timeoutMs)
            }
        }
    }

    fun release() {
        synchronized(this) {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (_: Throwable) {
            } finally {
                wakeLock = null
            }
        }
    }
}
