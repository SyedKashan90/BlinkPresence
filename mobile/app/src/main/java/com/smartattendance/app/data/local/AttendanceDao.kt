package com.smartattendance.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineAttendanceDao {
    @Insert
    suspend fun enqueue(entry: OfflineAttendanceEntity): Long

    @Query("SELECT * FROM offline_attendance_queue ORDER BY createdAtIso ASC")
    suspend fun all(): List<OfflineAttendanceEntity>

    @Query("SELECT * FROM offline_attendance_queue ORDER BY createdAtIso ASC")
    fun observeAll(): Flow<List<OfflineAttendanceEntity>>

    @Query("SELECT COUNT(*) FROM offline_attendance_queue")
    fun observePendingCount(): Flow<Int>

    @Update
    suspend fun update(entry: OfflineAttendanceEntity)

    @Delete
    suspend fun delete(entry: OfflineAttendanceEntity)
}

@Dao
interface CachedAttendanceDao {
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<CachedAttendanceEntity>)

    @Query("SELECT * FROM cached_attendance ORDER BY markedAt DESC")
    fun observeAll(): Flow<List<CachedAttendanceEntity>>

    @Query("DELETE FROM cached_attendance")
    suspend fun clear()
}
