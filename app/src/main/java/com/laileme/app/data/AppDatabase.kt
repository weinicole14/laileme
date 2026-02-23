package com.laileme.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.laileme.app.data.dao.DiaryDao
import com.laileme.app.data.dao.PeriodDao
import com.laileme.app.data.dao.SleepDao
import com.laileme.app.data.entity.DiaryEntry
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.data.entity.SleepRecord

@Database(entities = [PeriodRecord::class, DiaryEntry::class, SleepRecord::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "laileme_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
