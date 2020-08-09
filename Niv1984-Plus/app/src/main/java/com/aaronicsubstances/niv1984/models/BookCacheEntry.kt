package com.aaronicsubstances.niv1984.models

import androidx.room.*

@Entity
data class BookCacheEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var groupId: String,
    var cacheVersion: String,
    var perChapterItemRank: Int,

    // BookDisplayItem fields.
    var serializedViewType: String,
    var chapterNumber: Int,
    var verseNumber: Int,
    var isFirstVerseContent: Boolean,

    // BookDisplayItemContent fields.
    var text: String,
    var serializedBlockQuoteKind: String?,
    var isFirstDivider: Boolean,
    var footNoteId: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var serializedHighlightModeRemovableMarkups: ByteArray?
)

@Dao
abstract class BookCacheEntryDao {

    @Transaction
    open suspend fun setEntries(groupId: String, entries: List<BookCacheEntry>) {
        purgeGroup(groupId)
        insert(entries)
    }

    @Query("""DELETE FROM BookCacheEntry WHERE groupId =:groupId""")
    internal abstract suspend fun purgeGroup(groupId: String)

    @Insert
    internal abstract suspend fun insert(entries: List<BookCacheEntry>)

    @Query("""SELECT * FROM BookCacheEntry
        WHERE groupId = :groupId AND cacheVersion = :cacheVersion
         ORDER BY chapterNumber, perChapterItemRank
    """)
    abstract suspend fun getEntries(groupId: String, cacheVersion: String): List<BookCacheEntry>

    @Query("""DELETE FROM BookCacheEntry 
        WHERE groupId IN (:groupIds) AND chapterNumber = :chapterNumber""")
    abstract suspend fun purgeEntries(groupIds: List<String>, chapterNumber: Int)
}