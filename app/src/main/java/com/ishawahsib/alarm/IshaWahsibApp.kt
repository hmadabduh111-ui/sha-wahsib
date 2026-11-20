package com.ishawahsib.alarm

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.ishawahsib.alarm.alarm.AlarmRingingService

class IshaWahsibApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureAlarmNotificationChannel(this)
    }

    companion object {
        fun ensureAlarmNotificationChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val existing = manager.getNotificationChannel(AlarmRingingService.CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                AlarmRingingService.CHANNEL_ID,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_channel_description)
                enableLights(true)
                enableVibration(false) // VibratorManager داخل الخدمة يتحكم بالاهتزاز بشكل مستمر
                setSound(null, null)   // MediaPlayer داخل الخدمة يشغل الصوت على مسار USAGE_ALARM لمنع الازدواج
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
