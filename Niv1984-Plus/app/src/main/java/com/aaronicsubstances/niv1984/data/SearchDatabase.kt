package com.aaronicsubstances.niv1984.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aaronicsubstances.niv1984.models.*

@TypeConverters( DateTypeConverter::class, TimestampTypeConverter::class,
    UuidTypeConverter::class)
@Database(entities = [ BibleIndexRecord::class, BatchedDataSourceEntity::class, BookCacheEntry::class ],
    version = 1, exportSchema = true)
abstract class SearchDatabase : RoomDatabase() {

    abstract fun bibleIndexRecordDao(): BibleIndexRecordDao
    abstract fun batchedDataSourceDao(): BatchedDataSourceEntityDao
    abstract fun bookCacheEntryDao(): BookCacheEntryDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: SearchDatabase? = null

        fun getDatabase(context: Context): SearchDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SearchDatabase::class.java,
                    "search.db"
                )
                    .createFromAsset("search_data.db")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}