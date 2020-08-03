package com.aaronicsubstances.niv1984.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VerseHighlighterTest {

    @Test
    fun testAddNewHighlightRange() {
        var expected = mutableListOf(Pair(4, 5))
        var newRange = Pair(4, 5)
        var actual = mutableListOf<Pair<Int, Int>>()
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(Pair(4, 5), Pair(5, 6))
        newRange = Pair(4, 5)
        actual = mutableListOf(Pair(5, 6))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(Pair(4, 5), Pair(5, 6))
        newRange = Pair(5, 6)
        actual = mutableListOf(Pair(4, 5))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(Pair(4, 5), Pair(5, 6), Pair(18, 20))
        newRange = Pair(5, 6)
        actual = mutableListOf(Pair(4, 5), Pair(18, 20))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(Pair(4, 5), Pair(5, 6), Pair(18, 20))
        newRange = Pair(18, 20)
        actual = mutableListOf(Pair(4, 5), Pair(5, 6))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(Pair(4, 5), Pair(5, 6), Pair(18, 20), Pair(22, 25))
        newRange = Pair(18, 20)
        actual = mutableListOf(Pair(4, 5), Pair(5, 6), Pair(22, 25))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)
    }

    @Test
    fun testOptimizeHighlightRanges() {
        var expected = listOf<Pair<Int, Int>>()
        var actual = VerseHighlighter.optimizeHighlightRanges(mutableListOf())
        assertEquals(expected, actual)

        expected = listOf(Pair(4, 5))
        actual = VerseHighlighter.optimizeHighlightRanges(mutableListOf(Pair(4, 5)))
        assertEquals(expected, actual)

        expected = listOf(Pair(4, 6))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(Pair(4, 5), Pair(5, 6)))
        assertEquals(expected, actual)

        expected = listOf(Pair(4, 6), Pair(9, 11))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(Pair(4, 5), Pair(5, 6), Pair(9, 11)))
        assertEquals(expected, actual)

        expected = listOf(Pair(4, 6), Pair(9, 17))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(Pair(4, 5), Pair(5, 6), Pair(9, 11), Pair(11, 17)))
        assertEquals(expected, actual)

        expected = listOf(Pair(4, 6), Pair(7, 8), Pair(9, 17))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(Pair(4, 6), Pair(7, 8), Pair(9, 17)))
        assertEquals(expected, actual)
    }

    @Test
    fun testClearHighlightRangeTest() {
        var expected = listOf(Pair(4, 5))
        var actual = VerseHighlighter.clearHighlightRange(
            listOf(Pair(4, 5)), Pair(5, 6))
        assertEquals(expected, actual)

        expected = listOf(Pair(4, 5), Pair(6, 10))
        actual = VerseHighlighter.clearHighlightRange(
            listOf(Pair(4, 10)), Pair(5, 6))
        assertEquals(expected, actual)

        expected = listOf()
        actual = VerseHighlighter.clearHighlightRange(
            listOf(Pair(4, 10)), Pair(1, 16))
        assertEquals(expected, actual)

        expected = listOf()
        actual = VerseHighlighter.clearHighlightRange(
            listOf(Pair(4, 10)), Pair(4, 10))
        assertEquals(expected, actual)

        expected = listOf(Pair(16, 20))
        actual = VerseHighlighter.clearHighlightRange(
            listOf(Pair(4, 10), Pair(14, 20)), Pair(1, 16))
        assertEquals(expected, actual)

        expected = listOf(Pair(4, 8), Pair(16, 20))
        actual = VerseHighlighter.clearHighlightRange(
            listOf(Pair(4, 10), Pair(14, 20)), Pair(8, 16))
        assertEquals(expected, actual)
    }
}