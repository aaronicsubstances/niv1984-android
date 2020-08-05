package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.models.VerseBlockHighlightRange
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

    @Test
    fun testDetermineBlockRangesAffectedBySelection() {
        var data = listOf<VerseBlockHighlightRange>()
        
        var expected = listOf<VerseBlockHighlightRange>()
        var actual = VerseHighlighter.determineBlockRangesAffectedBySelection(0, 0, data)
        assertEquals(expected, actual)
        
        data = listOf(VerseBlockHighlightRange(1, 0, HighlightRange(0, 74)),
                VerseBlockHighlightRange(2, 0, HighlightRange(76, 184)),
                VerseBlockHighlightRange(3, 0, HighlightRange(186, 308)),
                VerseBlockHighlightRange(4, 0, HighlightRange(310, 472)),
                VerseBlockHighlightRange(5, 0, HighlightRange(474, 568)),
                VerseBlockHighlightRange(6, 0, HighlightRange(570, 656)),
                VerseBlockHighlightRange(7, 0, HighlightRange(658, 712)),
                VerseBlockHighlightRange(8, 0, HighlightRange(714, 868)),
                VerseBlockHighlightRange(9, 0, HighlightRange(870, 1053)),
                VerseBlockHighlightRange(10, 0, HighlightRange(1055, 1200)),
                VerseBlockHighlightRange(11, 0, HighlightRange(1202, 1351)),
                VerseBlockHighlightRange(12, 0, HighlightRange(1353, 1473)),
                VerseBlockHighlightRange(13, 0, HighlightRange(1475, 1626)),
                VerseBlockHighlightRange(13, 1, HighlightRange(1628, 1727)),
                VerseBlockHighlightRange(13, 2, HighlightRange(1729, 1822)),
                VerseBlockHighlightRange(14, 0, HighlightRange(1824, 1985)),
                VerseBlockHighlightRange(15, 0, HighlightRange(1987, 2120)),
                VerseBlockHighlightRange(16, 0, HighlightRange(2122, 2223)),
                VerseBlockHighlightRange(17, 0, HighlightRange(2225, 2327)),
                VerseBlockHighlightRange(18, 0, HighlightRange(2329, 2468)),
                VerseBlockHighlightRange(19, 0, HighlightRange(2470, 2565)),
                VerseBlockHighlightRange(20, 0, HighlightRange(2567, 2640)),
                VerseBlockHighlightRange(21, 0, HighlightRange(2642, 2710)),
                VerseBlockHighlightRange(22, 0, HighlightRange(2712, 2838)),
                VerseBlockHighlightRange(23, 0, HighlightRange(2840, 2923)),
                VerseBlockHighlightRange(24, 0, HighlightRange(2925, 3072)),
                VerseBlockHighlightRange(25, 0, HighlightRange(3074, 3178)),
                VerseBlockHighlightRange(26, 0, HighlightRange(3180, 3381)),
                VerseBlockHighlightRange(27, 0, HighlightRange(3383, 3529)),
                VerseBlockHighlightRange(28, 0, HighlightRange(3531, 3604)),
                VerseBlockHighlightRange(28, 1, HighlightRange(3606, 3711)),
                VerseBlockHighlightRange(29, 0, HighlightRange(3713, 3918)),
                VerseBlockHighlightRange(30, 0, HighlightRange(3920, 4064)),
                VerseBlockHighlightRange(30, 1, HighlightRange(4066, 4204)))

        expected = listOf()
        actual = VerseHighlighter.determineBlockRangesAffectedBySelection(0, 0, data)
        assertEquals(expected, actual)

        expected = listOf(VerseBlockHighlightRange(13, 0, HighlightRange(4, 151)))
        actual = VerseHighlighter.determineBlockRangesAffectedBySelection(1479, 1626, data)
        assertEquals(expected, actual)

        expected = listOf(VerseBlockHighlightRange(13, 0, HighlightRange(0, 151)))
        actual = VerseHighlighter.determineBlockRangesAffectedBySelection(1473, 1626, data)
        assertEquals(expected, actual)

        expected = listOf(VerseBlockHighlightRange(11, 0, HighlightRange(98, 149)),
                VerseBlockHighlightRange(12, 0, HighlightRange(0, 120)),
                VerseBlockHighlightRange(13, 0, HighlightRange(0, 151)),
                VerseBlockHighlightRange(13, 1, HighlightRange(0, 72)))
        actual = VerseHighlighter.determineBlockRangesAffectedBySelection(1300, 1700, data)
        assertEquals(expected, actual)

        expected = data.map {
            VerseBlockHighlightRange(it.verseNumber, it.verseBlockIndex,
                    HighlightRange(0, it.range.endIndex - it.range.startIndex))
        }
        actual = VerseHighlighter.determineBlockRangesAffectedBySelection(0, 4204, data)
        assertEquals(expected, actual)

        // expected is unchanged.
        actual = VerseHighlighter.determineBlockRangesAffectedBySelection(0, 5000, data)
        assertEquals(expected, actual)
    }
}