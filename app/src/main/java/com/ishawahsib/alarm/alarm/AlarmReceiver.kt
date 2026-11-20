package com.ishawahsib.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ishawahsib.alarm.IshaWahsibApp
import com.ishawahsib.alarm.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGER) return

        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1) return

        // تثبيت المعالج مستيقظاً حتى تبدأ خدمة الرنين الأمامية
        AlarmWakeLock.acquire(context)
        IshaWahsibApp.ensureAlarmNotificationChannel(context)

        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_START_RINGING
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // تحديث حالة المنبه في قاعدة البيانات أو جدولة التكرار التالي عبر goAsync
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AlarmDatabase.getInstance(context).alarmDao()
                val alarm = dao.getById(alarmId)
                if (alarm != null) {
                    if (alarm.isRepeating()) {
                        AlarmScheduler.schedule(context, alarm)
                    } else {
                        dao.setEnabled(alarmId, false)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.ishawahsib.alarm.ACTION_ALARM_TRIGGER"
    }
}
