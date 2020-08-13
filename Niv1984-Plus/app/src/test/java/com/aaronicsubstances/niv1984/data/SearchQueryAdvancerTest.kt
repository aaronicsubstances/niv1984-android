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
            true, 0)
        Assert.assertEquals(expected, actual)

        expected = "\"GOD father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("GOD", "father"),
            true, 0)
        Assert.assertEquals(expected, actual)

        expected = "\"god father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("god", "father"),
            true, 0)
        Assert.assertEquals(expected, actual)

        expected = "\"\u0186d\u0254 1 father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            true, 0)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForAllMatch() {
        var expected = "\"god\""
        var actual = SearchQueryAdvancer.transformUserQuery(listOf("god"),
            false, 0)
        Assert.assertEquals(expected, actual)

        expected = "\"GOD\" NEAR/3 \"father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("GOD", "father"),
            false, 0)
        Assert.assertEquals(expected, actual)

        expected = "\"god\" NEAR/3 \"father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("god", "father"),
            false, 0)
        Assert.assertEquals(expected, actual)

        expected = "\"\u0186d\u0254\" NEAR/3 \"1\" NEAR/3 \"father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            false, 0)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForAnyMatch() {
        var expected = "\"god\""
        var actual = ""
        listOf(true, false).forEach {
            actual = SearchQueryAdvancer.transformUserQuery(
                listOf("god"),
                it, 0
            )
            Assert.assertEquals(expected, actual)
        }

        expected = "\"GOD\" OR \"father\""
        listOf(true, false).forEach {
            actual = SearchQueryAdvancer.transformUserQuery(
                listOf("GOD", "father"),
                it, 1
            )
            Assert.assertEquals(expected, actual)
        }

        expected = "\"god\" OR \"father\""
        listOf(true, false).forEach {
            actual = SearchQueryAdvancer.transformUserQuery(
                listOf("god", "father"),
                it, 1
            )
            Assert.assertEquals(expected, actual)
        }

        expected = "\"\u0186d\u0254\" OR \"1\" OR \"father\""
        listOf(true, false).forEach {
            actual = SearchQueryAdvancer.transformUserQuery(
                listOf("\u0186d\u0254", "1", "father"),
                it, 2
            )
            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun testTransformUserQueryForSubsetMatch() {
        var expected = "\"\u0186d\u0254\" NEAR/3 \"1\" OR \"1\" NEAR/3 \"father\""
        var actual = SearchQueryAdvancer.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            false, 1)
        Assert.assertEquals(expected, actual)

        expected = "\"\u0186d\u0254 1\" OR \"1 father\""
        actual = SearchQueryAdvancer.transformUserQuery(listOf("\u0186d\u0254", "1", "father"),
            true, 1)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testTransformUserQueryForEmptyMatch() {
        var expected = ""
        var actual = SearchQueryAdvancer.transformUserQuery(listOf(),
            true, 0)
        Assert.assertEquals(expected, actual)

        expected = ""
        actual = SearchQueryAdvancer.transformUserQuery(listOf(),
            false, 0)
        Assert.assertEquals(expected, actual)

        expected = ""
        actual = SearchQueryAdvancer.transformUserQuery(listOf(),
            true, 1)
        Assert.assertEquals(expected, actual)

        expected = ""
        actual = SearchQueryAdvancer.transformUserQuery(listOf(),
            false, 1)
        Assert.assertEquals(expected, actual)
    }
}