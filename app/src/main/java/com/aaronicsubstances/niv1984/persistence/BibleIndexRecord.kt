package com.aaronicsubstances.niv1984.persistence

import androidx.room.*

@Entity(tableName = "bible_index_record")
@Fts4
class BibleIndexRecord(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    var rowId: Int,

    @ColumnInfo(name = "bible_version")
    var bibleVersion: String,

    @ColumnInfo(name = "book_number")
    var bookNumber: String,

    @ColumnInfo(name = "chapter_number")
    var chapterNumber: String,

    @ColumnInfo(name = "verse_number")
    var verseNumber: String,

    @ColumnInfo(name = "is_foot_note")
    var isFootNote: String,

    var content: String
)

@Dao
interface BibleIndexRecordDao {
    @Insert
    suspend fun insert(entity: BibleIndexRecord)
}