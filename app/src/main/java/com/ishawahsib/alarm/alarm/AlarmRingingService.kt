package com.ishawahsib.alarm.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ishawahsib.alarm.IshaWahsibApp
import com.ishawahsib.alarm.R
import com.ishawahsib.alarm.data.AlarmDatabase
import com.ishawahsib.alarm.data.AlarmEntity
import com.ishawahsib.alarm.data.MathDifficulty
import com.ishawahsib.alarm.ui.AlarmLockScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var vibrator: Vibrator? = null
    private var volumeRampJob: Job? = null

    private var activeAlarmId: Int = -1
    private var isRinging: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SOLVED_DISMISS -> {
                stopRingingAndSelf()
                return START_NOT_STICKY
            }

            ACTION_TEST_RING -> {
                val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                startAlarmSession(alarmId, isTest = true)
            }

            else -> {
                val alarmId = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
                startAlarmSession(alarmId, isTest = false)
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun startAlarmSession(alarmId: Int, isTest: Boolean) {
        if (isRinging && activeAlarmId == alarmId) {
            relaunchLockScreenIfNeeded(alarmId)
            return
        }
        activeAlarmId = alarmId
        isRinging = true

        IshaWahsibApp.ensureAlarmNotificationChannel(this)
        val initialNotification = buildAlarmNotification(
            alarmId = alarmId,
            label = getString(R.string.alarm_ringing_title),
            difficulty = MathDifficulty.MEDIUM,
            problemCount = 2
        )

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            initialNotification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )

        serviceScope.launch(Dispatchers.IO) {
            val alarm = if (alarmId > 0) {
                AlarmDatabase.getInstance(this@AlarmRingingService).alarmDao().getById(alarmId)
            } else {
                null
            }

            val resolvedLabel = alarm?.label?.ifBlank { getString(R.string.alarm_ringing_title) }
                ?: if (isTest) "اختبار المنبّه الذكي" else getString(R.string.alarm_ringing_title)
            val difficulty = alarm?.difficulty ?: MathDifficulty.MEDIUM
            val problemCount = alarm?.problemCount ?: 2

            launch(Dispatchers.Main.immediate) {
                val updatedNotification = buildAlarmNotification(alarmId, resolvedLabel, difficulty, problemCount)
                val nm = getSystemService(android.app.NotificationManager::class.java)
                nm?.notify(NOTIFICATION_ID, updatedNotification)

                startAlarmAudio(alarm)
                if (alarm?.vibrate != false) {
                    startAlarmVibration()
                }
                launchLockScreenActivity(alarmId, resolvedLabel, difficulty, problemCount)
            }
        }
    }

    private fun buildAlarmNotification(
        alarmId: Int,
        label: String,
        difficulty: MathDifficulty,
        problemCount: Int
    ): Notification {
        val lockIntent = buildLockScreenIntent(alarmId, label, difficulty, problemCount)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.coerceAtLeast(1),
            lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // لا يوجد زر إغلاق أو غفوة هنا: يجب حل مسألة الرياضيات أولاً
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(label)
            .setContentText(getString(R.string.alarm_ringing_subtitle))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    private fun launchLockScreenActivity(
        alarmId: Int,
        label: String,
        difficulty: MathDifficulty,
        problemCount: Int
    ) {
        val lockIntent = buildLockScreenIntent(alarmId, label, difficulty, problemCount)
        runCatching { startActivity(lockIntent) }
    }

    private fun relaunchLockScreenIfNeeded(alarmId: Int) {
        val lockIntent = Intent(this, AlarmLockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        runCatching { startActivity(lockIntent) }
    }

    private fun buildLockScreenIntent(
        alarmId: Int,
        label: String,
        difficulty: MathDifficulty,
        problemCount: Int
    ): Intent {
        return Intent(this, AlarmLockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_DIFFICULTY, difficulty.name)
            putExtra(EXTRA_PROBLEM_COUNT, problemCount)
        }
    }

    private fun startAlarmAudio(alarm: AlarmEntity?) {
        stopAlarmAudio()

        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(alarmAttributes)
            .setAcceptsDelayedFocusGain(false)
            .build()
        audioFocusRequest = focusRequest
        audioManager?.requestAudioFocus(focusRequest)

        val soundUri = resolveAlarmSoundUri(alarm?.ringtoneUri)
        val gradual = alarm?.gradualVolume ?: true

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingingService, soundUri)
                setAudioAttributes(alarmAttributes)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = true
                prepare()
                val initialVol = if (gradual) 0.15f else 1.0f
                setVolume(initialVol, initialVol)
                start()
            }

            if (gradual) {
                volumeRampJob?.cancel()
                volumeRampJob = serviceScope.launch {
                    val steps = 20
                    for (step in 1..steps) {
                        delay(1000L)
                        val vol = (0.15f + (0.85f * step / steps)).coerceAtMost(1.0f)
                        mediaPlayer?.setVolume(vol, vol)
                    }
                }
            }
        } catch (_: Throwable) {
            // محاولة احتياطية بالنغمة الافتراضية للنظام في حال تعذر مسار النغمة المختارة
            runCatching {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (fallbackUri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(this@AlarmRingingService, fallbackUri)
                        setAudioAttributes(alarmAttributes)
                        isLooping = true
                        prepare()
                        setVolume(1.0f, 1.0f)
                        start()
                    }
                }
            }
        }
    }

    private fun resolveAlarmSoundUri(customUri: String?): Uri {
        if (!customUri.isNullOrBlank()) {
            return Uri.parse(customUri)
        }
        return RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun startAlarmVibration() {
        val pattern = longArrayOf(0, 700, 350, 700, 350)
        val effect = VibrationEffect.createWaveform(pattern, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator?.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            @Suppress("DEPRECATION")
            vibrator?.vibrate(effect, audioAttrs)
        }
    }

    private fun stopAlarmAudio() {
        volumeRampJob?.cancel()
        volumeRampJob = null
        runCatching {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        }
        mediaPlayer = null
        audioFocusRequest?.let { req ->
            audioManager?.abandonAudioFocusRequest(req)
        }
        audioFocusRequest = null
    }

    private fun stopRingingAndSelf() {
        isRinging = false
        activeAlarmId = -1
        stopAlarmAudio()
        runCatching { vibrator?.cancel() }
        AlarmWakeLock.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarmAudio()
        runCatching { vibrator?.cancel() }
        AlarmWakeLock.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "isha_wahsib_alarm_channel_v1"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START_RINGING = "com.ishawahsib.alarm.START_RINGING"
        const val ACTION_TEST_RING = "com.ishawahsib.alarm.TEST_RING"
        const val ACTION_SOLVED_DISMISS = "com.ishawahsib.alarm.SOLVED_DISMISS"

        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_DIFFICULTY = "extra_difficulty"
        const val EXTRA_PROBLEM_COUNT = "extra_problem_count"

        fun createSolvedDismissIntent(context: Context): Intent {
            return Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_SOLVED_DISMISS
            }
        }
    }
}
