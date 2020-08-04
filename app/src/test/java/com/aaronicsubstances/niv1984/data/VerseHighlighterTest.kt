package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.models.HighlightRange
import org.junit.Assert.assertEquals
import org.junit.Test

class VerseHighlighterTest {

    @Test
    fun testAddNewHighlightRange() {
        var expected = mutableListOf(HighlightRange(4, 5))
        var newRange = HighlightRange(4, 5)
        var actual = mutableListOf<HighlightRange>()
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6))
        newRange = HighlightRange(4, 5)
        actual = mutableListOf(HighlightRange(5, 6))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6))
        newRange = HighlightRange(5, 6)
        actual = mutableListOf(HighlightRange(4, 5))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6), HighlightRange(18, 20))
        newRange = HighlightRange(5, 6)
        actual = mutableListOf(HighlightRange(4, 5), HighlightRange(18, 20))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6), HighlightRange(18, 20))
        newRange = HighlightRange(18, 20)
        actual = mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)

        expected = mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6), HighlightRange(18, 20), HighlightRange(22, 25))
        newRange = HighlightRange(18, 20)
        actual = mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6), HighlightRange(22, 25))
        VerseHighlighter.addNewHighlightRange(actual, newRange)
        assertEquals(expected, actual)
    }

    @Test
    fun testOptimizeHighlightRanges() {
        var expected = listOf<HighlightRange>()
        var actual = VerseHighlighter.optimizeHighlightRanges(mutableListOf())
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(4, 5))
        actual = VerseHighlighter.optimizeHighlightRanges(mutableListOf(HighlightRange(4, 5)))
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(4, 6))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6)))
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(4, 6), HighlightRange(9, 11))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6), HighlightRange(9, 11)))
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(4, 6), HighlightRange(9, 17))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(HighlightRange(4, 5), HighlightRange(5, 6), HighlightRange(9, 11), HighlightRange(11, 17)))
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(4, 6), HighlightRange(7, 8), HighlightRange(9, 17))
        actual = VerseHighlighter.optimizeHighlightRanges(
            mutableListOf(HighlightRange(4, 6), HighlightRange(7, 8), HighlightRange(9, 17)))
        assertEquals(expected, actual)
    }

    @Test
    fun testClearHighlightRangeTest() {
        var expected = listOf(HighlightRange(4, 5))
        var actual = VerseHighlighter.clearHighlightRange(
            arrayOf(HighlightRange(4, 5)), HighlightRange(5, 6))
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(4, 5), HighlightRange(6, 10))
        actual = VerseHighlighter.clearHighlightRange(
            arrayOf(HighlightRange(4, 10)), HighlightRange(5, 6))
        assertEquals(expected, actual)

        expected = listOf()
        actual = VerseHighlighter.clearHighlightRange(
            arrayOf(HighlightRange(4, 10)), HighlightRange(1, 16))
        assertEquals(expected, actual)

        expected = listOf()
        actual = VerseHighlighter.clearHighlightRange(
            arrayOf(HighlightRange(4, 10)), HighlightRange(4, 10))
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(16, 20))
        actual = VerseHighlighter.clearHighlightRange(
            arrayOf(HighlightRange(4, 10), HighlightRange(14, 20)), HighlightRange(1, 16))
        assertEquals(expected, actual)

        expected = listOf(HighlightRange(4, 8), HighlightRange(16, 20))
        actual = VerseHighlighter.clearHighlightRange(
            arrayOf(HighlightRange(4, 10), HighlightRange(14, 20)), HighlightRange(8, 16))
        assertEquals(expected, actual)
    }
}