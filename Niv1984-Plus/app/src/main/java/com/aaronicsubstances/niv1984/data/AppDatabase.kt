package com.aaronicsubstances.niv1984.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aaronicsubstances.niv1984.models.UserHighlightData
import com.aaronicsubstances.niv1984.models.UserHighlightDataDao

@TypeConverters( DateTypeConverter::class, TimestampTypeConverter::class,
    UuidTypeConverter::class)
@Database(entities = [ UserHighlightData::class ],
    version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userHighlightDataDao(): UserHighlightDataDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /*private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE bible_index_record")
                database.execSQL("DROP TABLE BatchedDataSourceEntity")
                database.execSQL("DROP TABLE BookCacheEntry")
            }
        }*/

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "main.db")
                    //.addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}