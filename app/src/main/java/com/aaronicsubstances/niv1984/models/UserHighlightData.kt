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

    var verseNumber: Int,

    var verseBlockIndex: Int,

    var data: String
)

data class HighlightRange(var startIndex: Int, var endIndex: Int)

@Dao
interface UserHighlightDataDao {
    @Query("""SELECT * from UserHighlightData
        WHERE bibleVersion = :bibleVersion AND bookNumber = :bookNumber""")
    suspend fun getHighlightData(bibleVersion: String, bookNumber: Int): List<UserHighlightData>
}