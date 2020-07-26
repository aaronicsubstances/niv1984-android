package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.models.SearchResult
import org.junit.Test

import org.junit.Assert.*

class SearchResultDataSourceUnitTest {

    @Test
    fun testSerialization() {
        val expected = SearchResult().apply {
            this.bibleVersion = "bv"
            this.bookNumber = 1
            this.chapterNumber = 2
            this.verseNumber = 3
            this.docId = 4
            this.text = "tex"
            this.rank = 6
            this.lastUpdateTimestamp = 7L
        }
        val serialized = SearchResultDataSource.serializeItem(expected)
        //println("Serialized: $serialized")
        val actual = SearchResultDataSource.deserializeItem(serialized)
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryWithAnd() {
        var expected = "\"god\""
        var actual = SearchResultDataSource.transformUserQuery("god",
            true, false)
        assertEquals(expected, actual)

        expected = "\"GOD father\""
        actual = SearchResultDataSource.transformUserQuery("GOD-father",
            true, false)
        assertEquals(expected, actual)

        expected = "\"god        father\""
        actual = SearchResultDataSource.transformUserQuery("god        father",
            true, false)
        assertEquals(expected, actual)

        expected = "\"\u0186d\u0254 1 father\""
        actual = SearchResultDataSource.transformUserQuery("\u0186d\u0254%1#father",
            true, false)
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryWithOr() {
        var expected = "\"god\""
        var actual = SearchResultDataSource.transformUserQuery("god",
            false, true)
        assertEquals(expected, actual)

        expected = "\"GOD\" OR \"father\""
        actual = SearchResultDataSource.transformUserQuery("GOD-father",
            false, true)
        assertEquals(expected, actual)

        expected = "\"god\" OR \"father\""
        actual = SearchResultDataSource.transformUserQuery("god        father",
            false, true)
        assertEquals(expected, actual)

        expected = "\"\u0186d\u0254\" OR \"1\" OR \"father\""
        actual = SearchResultDataSource.transformUserQuery("\u0186d\u0254%1#father",
            false, true)
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryWithNear() {
        var expected = "\"god\""
        var actual = SearchResultDataSource.transformUserQuery("god",
            false, false)
        assertEquals(expected, actual)

        expected = "\"GOD\" NEAR/3 \"father\""
        actual = SearchResultDataSource.transformUserQuery("GOD-father",
            false, false)
        assertEquals(expected, actual)

        expected = "\"god\" NEAR/3 \"father\""
        actual = SearchResultDataSource.transformUserQuery("god        father",
            false, false)
        assertEquals(expected, actual)

        expected = "\"\u0186d\u0254\" NEAR/3 \"1\" NEAR/3 \"father\""
        actual = SearchResultDataSource.transformUserQuery("\u0186d\u0254%1#father",
            false, false)
        assertEquals(expected, actual)
    }
}
