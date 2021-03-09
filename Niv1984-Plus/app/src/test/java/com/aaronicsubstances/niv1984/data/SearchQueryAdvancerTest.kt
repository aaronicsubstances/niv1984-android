package com.aaronicsubstances.niv1984.data

import org.junit.Assert
import org.junit.Test

class SearchQueryAdvancerTest {

    @Test
    fun testSplitUserQuery() {
        var expected = listOf("god")
        var actual = SearchQueryAdvancer.splitUserQuery("god")
        Assert.assertEquals(expected, actual)

        expected = listOf("GOD", "father")
        actual = SearchQueryAdvancer.splitUserQuery("GOD-father")
        Assert.assertEquals(expected, actual)

        expected = listOf("god", "father")
        actual = SearchQueryAdvancer.splitUserQuery("god        father")
        Assert.assertEquals(expected, actual)

        expected = listOf("\u0186d\u0254", "1", "father")
        actual = SearchQueryAdvancer.splitUserQuery("\u0186d\u0254%1#father")
        Assert.assertEquals(expected, actual)

        expected = listOf()
        actual = SearchQueryAdvancer.splitUserQuery("")
        Assert.assertEquals(expected, actual)

        expected = listOf()
        actual = SearchQueryAdvancer.splitUserQuery("  -+= .\n ")
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForExactMatch() {
        var expected = "\"god\""
        var actual = SearchQueryAdvancer.transformUserQuery(listOf("god"),
            true)
        Assert.assertEquals(expected, actual)

        expected = "\"GOD father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("GOD", "father"),
            true)
        Assert.assertEquals(expected, actual)

        expected = "\"god father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("god", "father"),
            true)
        Assert.assertEquals(expected, actual)

        expected = "\"\u0186d\u0254 1 father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            true)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForInexactMatch() {
        var expected = "\"god\""
        var actual = SearchQueryAdvancer.transformUserQuery(listOf("god"),
            false)
        Assert.assertEquals(expected, actual)

        expected = "\"GOD\" NEAR/3 \"father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("GOD", "father"),
            false)
        Assert.assertEquals(expected, actual)

        expected = "\"god\" NEAR/3 \"father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("god", "father"),
            false)
        Assert.assertEquals(expected, actual)

        expected = "\"\u0186d\u0254\" NEAR/3 \"1\" NEAR/3 \"father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            false)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForEmptyMatch() {
        var expected = ""
        var actual = SearchQueryAdvancer.transformUserQuery(listOf(),
            true)
        Assert.assertEquals(expected, actual)

        expected = ""
        actual = SearchQueryAdvancer.transformUserQuery(listOf(),
            false)
        Assert.assertEquals(expected, actual)
    }
}