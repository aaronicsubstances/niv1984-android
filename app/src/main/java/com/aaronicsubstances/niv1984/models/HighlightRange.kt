package com.aaronicsubstances.niv1984.models

import androidx.room.*

@Entity
data class HighlightRange(

    @PrimaryKey(autoGenerate = true)
    var id: Int,

    var bibleVersion: String,

    var bookNumber: Int,

    var chapterNumber: Int,

    var verseNumber: Int,

    var verseBlockIndex: Int,

    var startIndex: Int,

    var endIndex: Int
)

@Dao
interface HighlightRangeDao {
    @Query("""SELECT * from HighlightRange
        WHERE bibleVersion = :bibleVersion AND bookNumber = :bookNumber
        ORDER BY chapterNumber, verseNumber, verseBlockIndex, startIndex""")
    suspend fun getHighlightRanges(bibleVersion: String, bookNumber: Int): List<HighlightRange>
}