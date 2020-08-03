package com.aaronicsubstances.niv1984.models

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity
data class UserHighlightData(

    @PrimaryKey(autoGenerate = true)
    var id: Int,

    var bibleVersion: String,

    var bookNumber: Int,

    var chapterNumber: Int,

    var data: String
) {

    data class ChapterData(var chapterNumber: Int,
                           var verseBlocks: Array<VerseBlockData>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ChapterData

            if (chapterNumber != other.chapterNumber) return false
            if (!verseBlocks.contentEquals(other.verseBlocks)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = chapterNumber
            result = 31 * result + verseBlocks.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "ChapterData(chapterNumber=$chapterNumber, verseBlocks=${verseBlocks.contentToString()})"
        }
    }

    data class VerseBlockData(var verseNumber: Int,
                              var blockIndex: Int,
                              var ranges: Array<HighlightRange>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as VerseBlockData

            if (verseNumber != other.verseNumber) return false
            if (blockIndex != other.blockIndex) return false
            if (!ranges.contentEquals(other.ranges)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = verseNumber
            result = 31 * result + blockIndex
            result = 31 * result + ranges.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "VerseBlockData(verseNumber=$verseNumber, blockIndex=$blockIndex, " +
                    "ranges=${ranges.contentToString()})"
        }
    }
}

data class HighlightRange(var startIndex: Int, var endIndex: Int)

@Dao
interface UserHighlightDataDao {
    @Query("""SELECT * from UserHighlightData
        WHERE bibleVersion = :bibleVersion AND bookNumber = :bookNumber""")
    suspend fun getHighlightData(bibleVersion: String, bookNumber: Int): List<UserHighlightData>
}