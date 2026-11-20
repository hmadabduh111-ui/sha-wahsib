package com.ishawahsib.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ishawahsib.alarm.data.AlarmDatabase
import com.ishawahsib.alarm.data.AlarmEntity
import com.ishawahsib.alarm.ui.MainActivity
import java.util.Calendar

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "extra_alarm_id"

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(context: Context, alarm: AlarmEntity): Long? {
        if (!alarm.isEnabled) {
            cancel(context, alarm.id)
            return null
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return null
        if (!canScheduleExact(context)) return null

        val triggerAtMillis = computeNextTriggerMillis(alarm)
        val alarmPendingIntent = buildReceiverPendingIntent(context, alarm.id)
        val showAppPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // setAlarmClock هو المعيار الذهبي في أندرويد للمنبّهات:
        // يوقظ الجهاز من وضع Doze العميق ويعمل حتى بعد إغلاق التطبيق من قائمة التطبيقات الأخيرة.
        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showAppPendingIntent)
        alarmManager.setAlarmClock(clockInfo, alarmPendingIntent)
        return triggerAtMillis
    }

    fun cancel(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = buildReceiverPendingIntent(context, alarmId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    suspend fun rescheduleAllEnabled(context: Context) {
        val dao = AlarmDatabase.getInstance(context).alarmDao()
        val enabledAlarms = dao.getEnabledAlarms()
        for (alarm in enabledAlarms) {
            schedule(context, alarm)
        }
    }

    fun computeNextTriggerMillis(alarm: AlarmEntity, fromMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromMillis }
        val candidate = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!alarm.isRepeating()) {
            if (candidate.timeInMillis <= now.timeInMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }

        // البحث عن أقرب يوم مفعّل ضمن الأسبوع القادم
        for (offset in 0..7) {
            val check = (candidate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            val dayOfWeek = check.get(Calendar.DAY_OF_WEEK)
            if (alarm.isDaySelected(dayOfWeek) && check.timeInMillis > now.timeInMillis) {
                return check.timeInMillis
            }
        }

        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate.timeInMillis
    }

    private fun buildReceiverPendingIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
