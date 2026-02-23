package com.laileme.app.data.dao

import androidx.room.*
import com.laileme.app.data.entity.SleepRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records WHERE date = :date")
    suspend fun getByDate(date: String): SleepRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SleepRecord)

    @Query("SELECT * FROM sleep_records ORDER BY date DESC")
    fun getAll(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SleepRecord>>
}
