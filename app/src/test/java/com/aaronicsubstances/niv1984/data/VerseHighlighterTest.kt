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
}