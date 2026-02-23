package com.laileme.app.data.dao

import androidx.room.*
import com.laileme.app.data.entity.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    fun getEntryByDate(date: Long): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDateOnce(date: Long): DiaryEntry?

    /** 获取所有日记（suspend版本，用于同步） */
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    suspend fun getAllList(): List<DiaryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)
}
