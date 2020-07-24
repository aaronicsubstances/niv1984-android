package com.aaronicsubstances.niv1984.models

import com.aaronicsubstances.largelistpaging.LargeListItem
import java.util.*

data class SearchResult(var rowId: Int,
                        var bibleVersion: String,
                        var bookNumber: Int,
                        var chapterNumber: Int,
                        var verseNumber: Int,
                        var isFootNote: Boolean,
                        var text: String,
                        var isHtml: Boolean): LargeListItem {

    override fun getKey() = rowId

    override fun getCacheDate() = throw NotImplementedError()

    override fun setCacheDate(cacheDate: Date?) { }

    override fun getRank() = -1

    override fun setRank(rank: Int) { }
}