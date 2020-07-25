package com.aaronicsubstances.niv1984.models

import com.aaronicsubstances.largelistpaging.LargeListItem

data class SearchResult(var rowId: Int,
                        var bibleVersion: String,
                        var bookNumber: Int,
                        var chapterNumber: Int,
                        var verseNumber: Int,
                        var isFootNote: Boolean,
                        var text: String,
                        var isHtml: Boolean): LargeListItem {

    override fun getKey() = rowId
}