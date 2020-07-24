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
    var bookNumber: Int,

    @ColumnInfo(name = "chapter_number")
    var chapterNumber: Int,

    @ColumnInfo(name = "verse_number")
    var verseNumber: Int,

    @ColumnInfo(name = "is_foot_note")
    var isFootNote: Boolean,

    var content: String
)

@Dao
interface BibleIndexRecordDao {

    @Query(
        """SELECT rowid AS rowId, 
                        bible_version AS bibleVersion, 
                        book_number AS bookNumber,
                        chapter_number AS chapterNumber, 
                        verse_number AS verseNumber, 
                        is_foot_note AS isFootNote,
                        snippet(bible_index_record) AS text, 
                        1 AS isHtml
                 FROM bible_index_record 
                 WHERE content MATCH :q
                    AND bible_version IN (:bibleVersions)
                    AND verse_number >= :minVerseNumber
                    AND book_number BETWEEN :minBookNumber AND :maxBookNumber
                    AND rowid > :minRowId
                 ORDER BY rowid ASC
                 LIMIT :limit""")
    suspend fun searchFuzzyForward(q: String, bibleVersions: List<String>, minVerseNumber: Int,
                            minBookNumber: Int, maxBookNumber: Int,
                            minRowId: Int, limit: Int)
            : List<SearchResult>

    @Query(
        """SELECT rowid AS rowId, 
                        bible_version AS bibleVersion, 
                        book_number AS bookNumber,
                        chapter_number AS chapterNumber, 
                        verse_number AS verseNumber, 
                        is_foot_note AS isFootNote,
                        snippet(bible_index_record) AS text, 
                        1 AS isHtml
                 FROM bible_index_record 
                 WHERE content MATCH :q
                    AND bible_version IN (:bibleVersions)
                    AND verse_number >= :minVerseNumber
                    AND book_number BETWEEN :minBookNumber AND :maxBookNumber
                    AND rowid < :maxRowId
                 ORDER BY rowid DESC
                 LIMIT :limit""")
    suspend fun searchFuzzyBackward(q: String, bibleVersions: List<String>, minVerseNumber: Int,
                                   minBookNumber: Int, maxBookNumber: Int,
                                   maxRowId: Int, limit: Int)
            : List<SearchResult>

    @Query(
        """SELECT rowid AS rowId, 
                        bible_version AS bibleVersion, 
                        book_number AS bookNumber,
                        chapter_number AS chapterNumber, 
                        verse_number AS verseNumber, 
                        is_foot_note AS isFootNote,
                        content AS text, 
                        0 AS isHtml
                 FROM bible_index_record 
                 WHERE content LIKE :q ESCAPE '\'
                    AND bible_version IN (:bibleVersions)
                    AND verse_number >= :minVerseNumber
                    AND book_number BETWEEN :minBookNumber AND :maxBookNumber
                    AND rowid > :minRowId
                 ORDER BY rowid ASC
                 LIMIT :limit""")
    suspend fun searchExactForward(q: String, bibleVersions: List<String>, minVerseNumber: Int,
                                   minBookNumber: Int, maxBookNumber: Int,
                                   minRowId: Int, limit: Int)
            : List<SearchResult>

    @Query(
        """SELECT rowid AS rowId, 
                        bible_version AS bibleVersion, 
                        book_number AS bookNumber,
                        chapter_number AS chapterNumber, 
                        verse_number AS verseNumber, 
                        is_foot_note AS isFootNote,
                        content AS text, 
                        0 AS isHtml
                 FROM bible_index_record 
                 WHERE content LIKE :q ESCAPE '\'
                    AND bible_version IN (:bibleVersions)
                    AND verse_number >= :minVerseNumber
                    AND book_number BETWEEN :minBookNumber AND :maxBookNumber
                    AND rowid < :maxRowId
                 ORDER BY rowid DESC
                 LIMIT :limit""")
    suspend fun searchExactBackward(q: String, bibleVersions: List<String>, minVerseNumber: Int,
                                    minBookNumber: Int, maxBookNumber: Int,
                                    maxRowId: Int, limit: Int)
            : List<SearchResult>
}