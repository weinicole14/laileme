package com.laileme.app.data.dao

import androidx.room.*
import com.laileme.app.data.entity.PeriodRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM period_records ORDER BY startDate DESC, id DESC")
    fun getAllRecords(): Flow<List<PeriodRecord>>

    @Query("SELECT * FROM period_records ORDER BY startDate DESC, id DESC LIMIT 1")
    fun getLatestRecord(): Flow<PeriodRecord?>

    @Query("SELECT * FROM period_records WHERE startDate >= :startTime AND startDate <= :endTime")
    fun getRecordsBetween(startTime: Long, endTime: Long): Flow<List<PeriodRecord>>

    /** 查找所有未结束的经期记录 */
    @Query("SELECT * FROM period_records WHERE endDate IS NULL ORDER BY startDate DESC")
    suspend fun getActiveRecords(): List<PeriodRecord>

    /** 获取最近一条已完成的经期记录（用于自动计算周期） */
    @Query("SELECT * FROM period_records WHERE endDate IS NOT NULL ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestCompletedRecord(): PeriodRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PeriodRecord): Long

    @Update
    suspend fun update(record: PeriodRecord)

    @Delete
    suspend fun delete(record: PeriodRecord)

    /** 批量删除 */
    @Delete
    suspend fun deleteAll(records: List<PeriodRecord>)
}
