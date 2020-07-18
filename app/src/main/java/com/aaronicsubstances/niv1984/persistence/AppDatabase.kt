package com.aaronicsubstances.niv1984.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.*

@TypeConverters( DateTypeConverter::class, TimestampTypeConverter::class,
    UuidTypeConverter::class)
@Database(entities = [ UserNote::class, UserNoteIndexRecord::class, BibleIndexRecord::class ],
    version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userNoteDao(): UserNoteDao
    abstract fun bibleIndexRecordDao(): BibleIndexRecordDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        suspend fun getDatabase(context: Context): AppDatabase = withContext(Dispatchers.IO) {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "main.db"
                )
                    .createFromAsset("seed_data.db")
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}