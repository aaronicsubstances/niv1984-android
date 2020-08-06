package com.aaronicsubstances.niv1984.models

import androidx.room.*

@Entity
data class UserHighlightData(

    @PrimaryKey(autoGenerate = true)
    var id: Int,

    var bibleVersion: String,

    var bookNumber: Int,

    var chapterNumber: Int,

    var verseNumber: Int,

    var verseBlockIndex: Int,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var data: ByteArray
)

data class HighlightRange(var startIndex: Int, var endIndex: Int)

data class VerseBlockHighlightRange(val verseNumber: Int,
                                    val verseBlockIndex: Int,
                                    val range: HighlightRange)

@Dao
abstract class UserHighlightDataDao {
    @Query("""SELECT * from UserHighlightData
        WHERE bibleVersion = :bibleVersion AND bookNumber = :bookNumber""")
    abstract suspend fun getBookHighlightData(bibleVersion: String, bookNumber: Int): List<UserHighlightData>

    @Query("""SELECT * FROM UserHighlightData
        WHERE bibleVersion = :bibleVersion AND bookNumber = :bookNumber AND chapterNumber = :chapterNumber
        AND verseNumber = :verseNumber AND verseBlockIndex IN (:blockIndices)
    """)
    abstract suspend fun getChapterHighlightData(bibleVersion: String, bookNumber: Int, chapterNumber: Int,
                                            verseNumber: Int, blockIndices: List<Int>): List<UserHighlightData>

    @Insert
    abstract suspend fun insertHighlightData(entities: List<UserHighlightData>)

    @Delete
    abstract suspend fun deleteHighlightData(entities: List<UserHighlightData>)

    @Transaction
    open suspend fun updateHighlightData(entitiesToDelete: List<UserHighlightData>,
                                         entitiesToInsert: List<UserHighlightData>) {
        deleteHighlightData(entitiesToDelete)
        insertHighlightData(entitiesToInsert)
    }
}