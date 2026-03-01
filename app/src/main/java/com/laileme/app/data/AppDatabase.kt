package com.laileme.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.laileme.app.data.dao.DiaryDao
import com.laileme.app.data.dao.PeriodDao
import com.laileme.app.data.dao.SleepDao
import com.laileme.app.data.dao.SecretDao
import com.laileme.app.data.entity.DiaryEntry
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.data.entity.SleepRecord
import com.laileme.app.data.entity.SecretRecord

@Database(entities = [PeriodRecord::class, DiaryEntry::class, SleepRecord::class, SecretRecord::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun sleepDao(): SleepDao
    abstract fun secretDao(): SecretDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `secret_records` (
                        `date` INTEGER NOT NULL,
                        `hadSex` INTEGER NOT NULL,
                        `protection` TEXT NOT NULL,
                        `feeling` INTEGER NOT NULL,
                        `mood` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "laileme_database"
                )
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
