package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.models.SearchResult
import org.junit.Test

import org.junit.Assert.*

class SearchResultDataSourceUnitTest {

    @Test
    fun testSplitUserQuery() {
        var expected = listOf("god")
        var actual = SearchResultDataSource.splitUserQuery("god")
        assertEquals(expected, actual)

        expected = listOf("GOD", "father")
        actual = SearchResultDataSource.splitUserQuery("GOD-father")
        assertEquals(expected, actual)

        expected = listOf("god", "father")
        actual = SearchResultDataSource.splitUserQuery("god        father")
        assertEquals(expected, actual)

        expected = listOf("\u0186d\u0254", "1", "father")
        actual = SearchResultDataSource.splitUserQuery("\u0186d\u0254%1#father")
        assertEquals(expected, actual)

        expected = listOf()
        actual = SearchResultDataSource.splitUserQuery("")
        assertEquals(expected, actual)

        expected = listOf()
        actual = SearchResultDataSource.splitUserQuery("  -+= .\n ")
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForExactMatch() {
        var expected = "\"god\""
        var actual = SearchResultDataSource.transformUserQuery(listOf("god"),
            true, 0)
        assertEquals(expected, actual)

        expected = "\"GOD father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("GOD", "father"),
            true, 0)
        assertEquals(expected, actual)

        expected = "\"god father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("god", "father"),
            true, 0)
        assertEquals(expected, actual)

        expected = "\"\u0186d\u0254 1 father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            true, 0)
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForAnyMatch() {
        var expected = "\"god\""
        var actual = SearchResultDataSource.transformUserQuery(listOf("god"),
            false, 0)
        assertEquals(expected, actual)

        expected = "\"GOD\" OR \"father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("GOD", "father"),
            false, 1)
        assertEquals(expected, actual)

        expected = "\"god\" OR \"father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("god", "father"),
            false, 1)
        assertEquals(expected, actual)

        expected = "\"\u0186d\u0254\" OR \"1\" OR \"father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            false, 2)
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForAllMatch() {
        var expected = "\"god\""
        var actual = SearchResultDataSource.transformUserQuery(listOf("god"),
            false, 0)
        assertEquals(expected, actual)

        expected = "\"GOD\" NEAR/3 \"father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("GOD", "father"),
            false, 0)
        assertEquals(expected, actual)

        expected = "\"god\" NEAR/3 \"father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("god", "father"),
            false, 0)
        assertEquals(expected, actual)

        expected = "\"\u0186d\u0254\" NEAR/3 \"1\" NEAR/3 \"father\""
        actual = SearchResultDataSource.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            false, 0)
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForSubsetMatch() {
        var expected = "\"\u0186d\u0254\" NEAR/3 \"1\" OR \"1\" NEAR/3 \"father\""
        var actual = SearchResultDataSource.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            false, 1)
        assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForEmptyMatch() {
        var expected = ""
        var actual = SearchResultDataSource.transformUserQuery(listOf(),
            true, 0)
        assertEquals(expected, actual)

        expected = ""
        actual = SearchResultDataSource.transformUserQuery(listOf(),
            false, 0)
        assertEquals(expected, actual)

        expected = ""
        actual = SearchResultDataSource.transformUserQuery(listOf(),
            false, 1)
        assertEquals(expected, actual)
    }
}
