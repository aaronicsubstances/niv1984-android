package com.aaronicsubstances.niv1984.models

import com.aaronicsubstances.endlesspaginglib.EndlessListItem
import java.util.Date

class BookReadItem(var key: Key,
                   val viewType: ViewType,
                   val verseNumber: Int,
                   val text: CharSequence) : EndlessListItem {
    override fun getKey(): Any {
        return key
    }

    override fun getCacheDate(): Date? = null

    override fun setCacheDate(cacheDate: Date?) {
    }

    enum class ViewType {
        TITLE, VERSE, HEADER, FOOTNOTE, DIVIDER, CHAPTER_FRAGMENT, CROSS_REFERENCES
    }

    data class Key(val chapterNumber: Int,
                   val contentIndex: Int,
                   val bibleVersionCode: String)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookReadItem

        if (key != other.key) return false
        if (viewType != other.viewType) return false
        if (verseNumber != other.verseNumber) return false
        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + viewType.hashCode()
        result = 31 * result + verseNumber
        result = 31 * result + text.hashCode()
        return result
    }

    override fun toString(): String {
        return "BookReadItem(key=$key, viewType=$viewType, verseNumber=$verseNumber, text=$text)"
    }
}