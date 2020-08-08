package com.aaronicsubstances.niv1984.models

import androidx.room.Ignore
import com.aaronicsubstances.largelistpaging.ExtendedLargeListItem

class SearchResult: ExtendedLargeListItem {
    var docId= 0
    var bibleVersion = ""
    var bookNumber: Int = 0
    var chapterNumber = 0
    var verseNumber = 0
    var text = ""

    @Ignore
    var rank: Int = 0

    @Ignore
    var lastUpdateTimestamp = 0L

    override fun fetchKey() = docId
    override fun fetchRank() = rank
    override fun storeRank(value: Any)  {
        this.rank = value as Int
    }
    override fun fetchLastUpdateTimestamp() = lastUpdateTimestamp
    override fun storeLastUpdateTimestamp(value: Long) {
        this.lastUpdateTimestamp = value
    }

    override fun toString(): String {
        return "SearchResult(rowId=$docId, bibleVersion='$bibleVersion', " +
                "bookNumber=$bookNumber, chapterNumber=$chapterNumber, verseNumber=$verseNumber, " +
                "text='$text', rank=$rank, lastUpdateTimestamp=$lastUpdateTimestamp)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchResult

        if (docId != other.docId) return false
        if (bibleVersion != other.bibleVersion) return false
        if (bookNumber != other.bookNumber) return false
        if (chapterNumber != other.chapterNumber) return false
        if (verseNumber != other.verseNumber) return false
        if (text != other.text) return false
        if (rank != other.rank) return false
        if (lastUpdateTimestamp != other.lastUpdateTimestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = docId
        result = 31 * result + bibleVersion.hashCode()
        result = 31 * result + bookNumber
        result = 31 * result + chapterNumber
        result = 31 * result + verseNumber
        result = 31 * result + text.hashCode()
        result = 31 * result + rank
        result = 31 * result + lastUpdateTimestamp.hashCode()
        return result
    }
}

data class SearchResultAdapterItem(val viewType: Int,
                                   val item: SearchResult)