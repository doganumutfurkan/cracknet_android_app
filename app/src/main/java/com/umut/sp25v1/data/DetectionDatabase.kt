package com.umut.sp25v1.data

import androidx.room.TypeConverters
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.umut.sp25v1.data.dao.DetectionResultDao
import com.umut.sp25v1.data.model.DetectionResult


@Database(entities = [DetectionResult::class], version = 3) // 🔼 Versiyonu artırdık
@TypeConverters(Converters::class)
abstract class DetectionDatabase : RoomDatabase() {
    abstract fun detectionResultDao(): DetectionResultDao

    companion object {
        @Volatile
        private var INSTANCE: DetectionDatabase? = null

        fun getDatabase(context: Context): DetectionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DetectionDatabase::class.java,
                    "detection_database"
                )
                    .addMigrations(MIGRATION_2_3) // ✅ Yeni migration'ı ekliyoruz
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE detection_results ADD COLUMN title TEXT NOT NULL DEFAULT 'Tespit-Bilinmiyor'")
            }
        }
    }
}

