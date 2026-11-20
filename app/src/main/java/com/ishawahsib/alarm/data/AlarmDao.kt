package com.ishawahsib.alarm.data

import kotlinx.coroutines.flow.Flow

interface AlarmDao {
    fun observeAll(): Flow<List<AlarmEntity>>
    suspend fun getEnabledAlarms(): List<AlarmEntity>
    suspend fun getById(id: Int): AlarmEntity?
    suspend fun insert(alarm: AlarmEntity): Long
    suspend fun update(alarm: AlarmEntity)
    suspend fun delete(alarm: AlarmEntity)
    suspend fun setEnabled(id: Int, enabled: Boolean)
}
