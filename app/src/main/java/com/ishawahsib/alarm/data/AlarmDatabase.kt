package com.ishawahsib.alarm.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * قاعدة بيانات SQLite أصلية مبنية فوق DeviceProtectedStorageContext
 * لكي تعمل بسلاسة حتى في وضع Direct Boot (LOCKED_BOOT_COMPLETED) بعد إعادة التشغيل.
 */
class AlarmDatabase private constructor(context: Context) :
    SQLiteOpenHelper(
        context.applicationContext.createDeviceProtectedStorageContext(),
        DB_NAME,
        null,
        DB_VERSION
    ) {

    private val alarmsFlow = MutableStateFlow<List<AlarmEntity>>(emptyList())

    private val daoImpl = object : AlarmDao {
        override fun observeAll(): Flow<List<AlarmEntity>> = alarmsFlow.asStateFlow()

        override suspend fun getEnabledAlarms(): List<AlarmEntity> = withContext(Dispatchers.IO) {
            queryAlarms("isEnabled = 1", null)
        }

        override suspend fun getById(id: Int): AlarmEntity? = withContext(Dispatchers.IO) {
            queryAlarms("id = ?", arrayOf(id.toString())).firstOrNull()
        }

        override suspend fun insert(alarm: AlarmEntity): Long = withContext(Dispatchers.IO) {
            val rowId = writableDatabase.insert(TABLE_ALARMS, null, alarm.toContentValues(includeId = false))
            refreshFlow()
            rowId
        }

        override suspend fun update(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
            writableDatabase.update(
                TABLE_ALARMS,
                alarm.toContentValues(includeId = false),
                "id = ?",
                arrayOf(alarm.id.toString())
            )
            refreshFlow()
        }

        override suspend fun delete(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
            writableDatabase.delete(TABLE_ALARMS, "id = ?", arrayOf(alarm.id.toString()))
            refreshFlow()
        }

        override suspend fun setEnabled(id: Int, enabled: Boolean) = withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("isEnabled", if (enabled) 1 else 0)
            }
            writableDatabase.update(TABLE_ALARMS, values, "id = ?", arrayOf(id.toString()))
            refreshFlow()
        }
    }

    init {
        refreshFlow()
    }

    fun alarmDao(): AlarmDao = daoImpl

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ALARMS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                label TEXT NOT NULL,
                isEnabled INTEGER NOT NULL,
                repeatDaysMask INTEGER NOT NULL,
                difficulty TEXT NOT NULL,
                problemCount INTEGER NOT NULL,
                ringtoneUri TEXT,
                vibrate INTEGER NOT NULL,
                gradualVolume INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALARMS")
        onCreate(db)
    }

    private fun refreshFlow() {
        alarmsFlow.value = queryAlarms(null, null)
    }

    private fun queryAlarms(selection: String?, selectionArgs: Array<String>?): List<AlarmEntity> {
        val list = mutableListOf<AlarmEntity>()
        val cursor = readableDatabase.query(
            TABLE_ALARMS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "hour ASC, minute ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.toAlarmEntity())
            }
        }
        return list
    }

    private fun AlarmEntity.toContentValues(includeId: Boolean): ContentValues {
        return ContentValues().apply {
            if (includeId && id > 0) put("id", id)
            put("hour", hour)
            put("minute", minute)
            put("label", label)
            put("isEnabled", if (isEnabled) 1 else 0)
            put("repeatDaysMask", repeatDaysMask)
            put("difficulty", difficulty.name)
            put("problemCount", problemCount)
            if (ringtoneUri != null) put("ringtoneUri", ringtoneUri) else putNull("ringtoneUri")
            put("vibrate", if (vibrate) 1 else 0)
            put("gradualVolume", if (gradualVolume) 1 else 0)
        }
    }

    private fun Cursor.toAlarmEntity(): AlarmEntity {
        val diffRaw = getString(getColumnIndexOrThrow("difficulty"))
        val diff = runCatching { MathDifficulty.valueOf(diffRaw) }.getOrDefault(MathDifficulty.MEDIUM)
        val uriIdx = getColumnIndexOrThrow("ringtoneUri")
        return AlarmEntity(
            id = getInt(getColumnIndexOrThrow("id")),
            hour = getInt(getColumnIndexOrThrow("hour")),
            minute = getInt(getColumnIndexOrThrow("minute")),
            label = getString(getColumnIndexOrThrow("label")),
            isEnabled = getInt(getColumnIndexOrThrow("isEnabled")) == 1,
            repeatDaysMask = getInt(getColumnIndexOrThrow("repeatDaysMask")),
            difficulty = diff,
            problemCount = getInt(getColumnIndexOrThrow("problemCount")),
            ringtoneUri = if (isNull(uriIdx)) null else getString(uriIdx),
            vibrate = getInt(getColumnIndexOrThrow("vibrate")) == 1,
            gradualVolume = getInt(getColumnIndexOrThrow("gradualVolume")) == 1
        )
    }

    companion object {
        private const val DB_NAME = "isha_wahsib_alarms.db"
        private const val DB_VERSION = 1
        private const val TABLE_ALARMS = "alarms"

        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmDatabase(context).also { INSTANCE = it }
            }
        }
    }
}
