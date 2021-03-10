package com.aaronicsubstances.niv1984.models

import androidx.room.*

@Entity(tableName = "bible_index_record")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, tokenizerArgs = [ "remove_diacritics=0" ],
    notIndexed = [ "chapter_number", "verse_number", "bible_version", "book_number" ])
data class BibleIndexRecord(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    var docId: Int,

    @ColumnInfo(name = "bible_version")
    var bibleVersion: String,

    @ColumnInfo(name = "book_number")
    var bookNumber: Int,

    @ColumnInfo(name = "chapter_number")
    var chapterNumber: Int,

    @ColumnInfo(name = "verse_number")
    var verseNumber: Int,

    var content: String
)

@Dao
interface BibleIndexRecordDao {

    @Query(
        """SELECT rowid AS docId, 
                        bible_version AS bibleVersion, 
                        book_number AS bookNumber,
                        chapter_number AS chapterNumber, 
                        verse_number AS verseNumber,
                        snippet(bible_index_record) AS text
                 FROM bible_index_record 
                 WHERE content MATCH :q
                    AND bible_version IN (:selectedBibleVersions)
                    AND book_number BETWEEN :minBookNumber AND :maxBookNumber
                    AND rowid NOT IN (:extraExclusions)
                    AND rowid NOT IN (SELECT itemKey FROM BatchedDataSourceEntity b 
                        WHERE b.category = :category AND b.batchVersion = :batchVersion
                        AND b.batchNumber < :batchNumber)
                 ORDER BY CAST(book_number AS INT), CAST(chapter_number AS INT), 
                    CASE
                        WHEN verse_number < 1 THEN 2147483647 
                        ELSE CAST(verse_number AS INT)
                    END,
                    rowid
                 LIMIT :limit"""
    )
    suspend fun search(q: String, selectedBibleVersions: List<String>,
                       minBookNumber: Int, maxBookNumber: Int, extraExclusions: List<Int>,
                       category: String, batchVersion: String, batchNumber: Int, limit: Int): List<SearchResult>
}