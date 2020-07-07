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

    class Key(val chapterNumber: Int,
              val contentIndex: Int,
              val bibleVersionCode: String): Comparable<Key> {

        override fun compareTo(other: Key): Int {
            var result = this.chapterNumber.compareTo(other.chapterNumber)
            if (result == 0) {
                result = this.contentIndex.compareTo(other.contentIndex)
            }
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Key

            if (chapterNumber != other.chapterNumber) return false
            if (contentIndex != other.contentIndex) return false
            if (bibleVersionCode != other.bibleVersionCode) return false

            return true
        }

        override fun hashCode(): Int {
            var result = chapterNumber
            result = 31 * result + contentIndex
            result = 31 * result + bibleVersionCode.hashCode()
            return result
        }

        override fun toString(): String {
            return "Key(chapterNumber=$chapterNumber, contentIndex=$contentIndex, " +
                    "bibleVersionCode='$bibleVersionCode')"
        }
    }

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