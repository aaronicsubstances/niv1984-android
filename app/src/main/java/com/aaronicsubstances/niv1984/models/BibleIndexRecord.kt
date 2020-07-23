package com.aaronicsubstances.niv1984.models

import androidx.room.*

@Entity(tableName = "bible_index_record")
@Fts4
data class BibleIndexRecord(
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
    @Query(
        """SELECT rowId, bible_version, book_number, chapter_number, verse_number, 
                        is_foot_note, content FROM bible_index_record 
                 WHERE content MATCH :term AND rowId > :lastRowId""")
    suspend fun search(term: String, lastRowId: Int): List<BibleIndexRecord>
}