package com.aaronicsubstances.niv1984.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern

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



        private val TABLE_BIBLE_INDEX_RECORD = "bible_index_record"
        private val COL_CONTENT = "content"
        private val COL_CHAPTER = "chapter_number"
        private val COL_BIBLE_VERSION = "bible_version"
        private val COL_BOOK_NUMBER = "book_number"
        private val COL_VERSE = "verse_number"

        private val logger = LoggerFactory.getLogger(AppDatabase::class.qualifiedName)

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

    private class DatabaseSeeder(
        private val context: Context
    ): RoomDatabase.Callback() {

        private val chapterCounts = arrayListOf(
            50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150,
            31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13,
            5, 5, 3, 5, 1, 1, 1, 22
        )

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)

            logger.warn("about to populate database...")
            db.beginTransaction()
            try {
                if (mustPopulate(db)) {
                    populateDatabase(db)
                }
                db.setTransactionSuccessful()
            }
            finally {
                db.endTransaction()
            }
        }

        fun mustPopulate(db: SupportSQLiteDatabase): Boolean {
            val recordCount = db.query("SELECT COUNT(*) FROM " +
                    "$TABLE_BIBLE_INDEX_RECORD").use { c ->
                c.moveToFirst()
                c.getInt(0)
            }
            if (recordCount > 0) {
                logger.warn("Found $recordCount records in table $TABLE_BIBLE_INDEX_RECORD. " +
                        "Seeding will therefore be skipped.")
                return false
            }
            return true
        }

        fun populateDatabase(db: SupportSQLiteDatabase) {
            val startTime = Date().time
            val statement = db.compileStatement("INSERT INTO $TABLE_BIBLE_INDEX_RECORD" +
                    " ($COL_BIBLE_VERSION, $COL_BOOK_NUMBER, $COL_CHAPTER, $COL_VERSE, $COL_CONTENT) " +
                    " VALUES " +
                    " (?, ?, ?, ?, ?)")
            val bibleVersions = arrayListOf("kjv1769", "niv1984", "asante2012")
            for (bibleVersion in bibleVersions) {
                val bvStartTime = Date().time
                for (bookNumber in 1..chapterCounts.size) {
                    for (chapter in 1..chapterCounts[bookNumber - 1]) {
                        indexVerses(statement, bibleVersion, bookNumber, chapter)
                    }
                }
                logger.warn("Done indexing bible version {} in {} secs",
                    (Date().time - bvStartTime) / 1000.0)
            }
            val timeTaken = (Date().time - startTime) / 1000.0
            logger.warn("Database population took {} secs", timeTaken)
        }

        fun indexVerses(statement: SupportSQLiteStatement,
                        bibleVersion: String, bookNumber: Int, chapter: Int) {
            val assetPath = String.format("%s/%02d/%03d.tvn", bibleVersion,
                bookNumber, chapter)
            val assetStream = context.assets.open(assetPath)
            BufferedReader(InputStreamReader(assetStream, "utf-8")).useLines { lines ->
                val vContent = StringBuilder()
                var vNum: String? = null
                for (line in lines) {
                    val colonIndex = line.indexOf(':')
                    val tag = line.substring (0, colonIndex)
                    val value = line.substring (colonIndex + 1)
                    when(tag) {
                        "content" -> {
                            vContent.append(value)
                        }
                        "v_start" -> {
                            vNum = value
                        }
                        "v_end" -> {
                            val content = normalizeContent(vContent.toString())
                            vContent.setLength(0)

                            statement.clearBindings()
                            statement.bindString(1, bibleVersion)
                            statement.bindString(2, bookNumber.toString())
                            statement.bindString(3, chapter.toString())
                            statement.bindString(4, vNum)
                            statement.bindString(5, content)

                            statement.execute()
                        }
                    }
                }
            }
        }

        private fun normalizeContent(c: String): String {
            var p = Pattern.compile("&#(\\d+);")
            val sb = StringBuffer()
            val matcher = p.matcher(c)
            while (matcher.find()) {
                val replacement = Integer.parseInt(matcher.group(1)).toChar().toString()
                matcher.appendReplacement(sb, replacement)
            }
            matcher.appendTail(sb)
            var normalized = sb.toString()

            // remove unnecessary whitespace.
            normalized = normalized.trim().replace(Regex.fromLiteral("\\s+"), " ")

            //TODO: replace twi non-English alphabets with english ones.

            return normalized
        }
    }
}