package com.laileme.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laileme.app.data.entity.SecretRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SecretDao {
    @Query("SELECT * FROM secret_records ORDER BY date DESC")
    fun getAll(): Flow<List<SecretRecord>>

    @Query("SELECT * FROM secret_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getRecordsBetween(startDate: Long, endDate: Long): Flow<List<SecretRecord>>

    @Query("SELECT * FROM secret_records WHERE date = :date LIMIT 1")
    fun getRecordByDate(date: Long): Flow<SecretRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SecretRecord)

    @Query("DELETE FROM secret_records WHERE date = :date")
    suspend fun deleteByDate(date: Long)

    @Query("DELETE FROM secret_records")
    suspend fun deleteAll()
}