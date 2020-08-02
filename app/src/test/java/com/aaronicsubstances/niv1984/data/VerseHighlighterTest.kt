package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.data.VerseHighlighter.Markup

import org.junit.Assert.assertEquals
import org.junit.Test

class VerseHighlighterTest {

    @Test
    fun test1() {
        val instance = VerseHighlighter()
        instance.addInitMarkup("1", "<font>")
        instance.addInitText("a  title  ")
        instance.addInitMarkup("2", "</font>")

        instance.beginProcessing()
        assertEquals("a title ", instance.rawText.toString())
        assertEquals(listOf(Markup("1", 0, "<font>"),
            Markup("2", 8, "</font>")), instance.markupList)

        instance.finalizeProcessing()
        assertEquals("<font>a title </font>", instance.rawText.toString())
        assertEquals(listOf(Markup("1", 0, "<font>"),
            Markup("2", 14, "</font>")), instance.markupList)
    }

    @Test
    fun test2() {
        val instance = VerseHighlighter()
        instance.addInitText("  ")
        instance.addInitMarkup("", "<p>")
        instance.addInitMarkup("", "<font>")
        instance.addInitText(" a  title  ")
        instance.addInitMarkup("", "</font>")
        instance.addInitMarkup("", "</p>")

        instance.beginProcessing()
        assertEquals("  a title ", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 1, "<font>"),
            Markup("", 10, "</font>"),
            Markup("", 10, "</p>")), instance.markupList)

        instance.finalizeProcessing()
        assertEquals(" <p><font> a title </font></p>", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 4, "<font>"),
            Markup("", 19, "</font>"),
            Markup("", 26, "</p>")), instance.markupList)
    }

    @Test
    fun test3() {
        val instance = VerseHighlighter()
        instance.addInitText("  ")
        instance.addInitMarkup("", "<p>")
        instance.addInitMarkup("", "<font>")
        instance.addInitMarkupWithPlaceholder("", "hr", " a  title  ")
        instance.addInitMarkup("", "</font>")
        instance.addInitMarkup("", "</p>")

        instance.beginProcessing()
        assertEquals("  a  title  ", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 1, "<font>"),
            Markup("", 1, "hr", " a  title  "),
            Markup("", 12, "</font>"),
            Markup("", 12, "</p>")), instance.markupList)

        instance.finalizeProcessing()
        assertEquals(" <p><font>hr</font></p>", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 4, "<font>"),
            Markup("", 10, "hr", " a  title  "),
            Markup("", 12, "</font>"),
            Markup("", 19, "</p>")), instance.markupList)
    }

    @Test
    fun test4() {
        val instance = VerseHighlighter()
        instance.addInitText("  ")
        instance.addInitMarkup("", "<p>")
        instance.addInitMarkup("", "<font>")
        instance.addInitMarkupWithPlaceholder("", "hr", " a  title  ")
        instance.addInitMarkup("", "</font>")
        instance.addInitMarkup("", "</p>")

        instance.beginProcessing()
        assertEquals("  a  title  ", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 1, "<font>"),
            Markup("", 1, "hr", " a  title  "),
            Markup("", 12, "</font>"),
            Markup("", 12, "</p>")), instance.markupList)

        instance.updateText(1, "<span>", false)
        assertEquals("  a  title  <span>", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 1, "<font>"),
            Markup("", 1, "hr", " a  title  "),
            Markup("", 18, "</font>"),
            Markup("", 18, "</p>")), instance.markupList)

        instance.updateText(12, "</span>", true)
        assertEquals("  a  title  <span></span>", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 1, "<font>"),
            Markup("", 1, "hr", " a  title  "),
            Markup("", 25, "</font>"),
            Markup("", 25, "</p>")), instance.markupList)

        instance.finalizeProcessing()
        assertEquals(" <p><font>hr<span></span></font></p>", instance.rawText.toString())
        assertEquals(listOf(Markup("", 1, "<p>"),
            Markup("", 4, "<font>"),
            Markup("", 10, "hr", " a  title  "),
            Markup("", 25, "</font>"),
            Markup("", 32, "</p>")), instance.markupList)
    }

    @Test
    fun test5() {
        val instance = VerseHighlighter()
        instance.addInitMarkup("", "<p>")
        instance.addInitText("font")
        instance.addInitMarkupWithPlaceholder("", "<hr>", "line")
        instance.addInitText("abbr")
        instance.addInitMarkup("", "</p>")
        instance.addInitText("\n")

        instance.beginProcessing()
        assertEquals("fontlineabbr ", instance.rawText.toString())
        assertEquals(listOf(Markup("", 0, "<p>"),
            Markup("", 4, "<hr>", "line"),
            Markup("", 12, "</p>")), instance.markupList)

        instance.updateText(1, "<span>", false)
        assertEquals("f<span>ontlineabbr ", instance.rawText.toString())
        assertEquals(listOf(Markup("", 0, "<p>"),
            Markup("", 10, "<hr>", "line"),
            Markup("", 18, "</p>")), instance.markupList)

        instance.updateText(4, "</span>", true)
        assertEquals("f<span>ont</span>lineabbr ", instance.rawText.toString())
        assertEquals(listOf(Markup("", 0, "<p>"),
            Markup("", 17, "<hr>", "line"),
            Markup("", 25, "</p>")), instance.markupList)

        instance.finalizeProcessing()
        assertEquals("<p>f<span>ont</span><hr>abbr</p> ", instance.rawText.toString())
        assertEquals(listOf(Markup("", 0, "<p>"),
            Markup("", 20, "<hr>", "line"),
            Markup("", 28, "</p>")), instance.markupList)
    }

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
    fun clearHighlightRangeTest() {
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