package com.ishawahsib.alarm.data

enum class MathDifficulty(val arabicLabel: String) {
    EASY("سهل"),
    MEDIUM("متوسط"),
    HARD("صعب")
}

data class AlarmEntity(
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "منبّه الاستيقاظ",
    val isEnabled: Boolean = true,
    /**
     * قناع الأيام السبعة (Bitmask):
     * 0 = مرة واحدة فقط
     * Bit 0 (1) = الأحد .. Bit 6 (64) = السبت
     */
    val repeatDaysMask: Int = 0,
    val difficulty: MathDifficulty = MathDifficulty.MEDIUM,
    val problemCount: Int = 2,
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = true
) {
    fun isRepeating(): Boolean = repeatDaysMask != 0

    fun isDaySelected(calendarDayOfWeek: Int): Boolean {
        val bitIndex = calendarDayOfWeek - 1
        return (repeatDaysMask and (1 shl bitIndex)) != 0
    }
}
