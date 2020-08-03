package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.data.VerseHighlighter.Markup

import org.junit.Assert
import org.junit.Test

class VerseHighlighterInstrumentedTest {
    private val nbsp = "\u00a0"

    @Test
    fun testEscapeHtmlSections1() {
        val instance = VerseHighlighter()
        instance.addInitMarkup(Markup("<font>", id="1"))
        instance.addInitText("a \ttitle>$nbsp  ")
        instance.addInitMarkup(Markup("</font>", id="2"))

        instance.beginProcessing()
        Assert.assertEquals("a title>$nbsp", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<font>", 0, id="1"),
                Markup("</font>", 9, id="2")
            ), instance.markupList
        )

        instance.escapeHtmlSections()
        Assert.assertEquals("a title&gt;$nbsp", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<font>", 0, id="1"),
                Markup("</font>", 12, id="2")
            ), instance.markupList
        )
    }

    @Test
    fun testEscapeHtmlSections2() {
        val instance = VerseHighlighter()
        instance.addInitText("  ")
        instance.addInitMarkup("<p>")
        instance.addInitMarkup("<font>")
        instance.addInitMarkup(Markup("hr", placeholder = "a   <title  >"))
        instance.addInitMarkup("</font>")
        instance.addInitMarkup("</p>")

        instance.beginProcessing()
        Assert.assertEquals("a   <title  >", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<font>", 0),
                Markup("hr", 0, placeholder= "a   <title  >"),
                Markup("</font>", 13),
                Markup("</p>", 13)
            ), instance.markupList
        )

        instance.escapeHtmlSections()
        Assert.assertEquals("a   <title  >", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<font>", 0),
                Markup("hr", 0, placeholder = "a   <title  >"),
                Markup("</font>", 13),
                Markup("</p>", 13)
            ), instance.markupList
        )
    }

    @Test
    fun test1() {
        val instance = VerseHighlighter()
        instance.addInitMarkup(Markup("<font>", id="1"))
        instance.addInitText("a  title  ")
        instance.addInitMarkup(Markup("</font>", id="2"))

        instance.beginProcessing()
        Assert.assertEquals("a title", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<font>", 0, id="1"),
                Markup("</font>", 7, id="2")
            ), instance.markupList
        )

        instance.finalizeProcessing()
        Assert.assertEquals("<font>a title</font>", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<font>", 0, id="1"),
                Markup("</font>", 13, id="2")
            ), instance.markupList
        )
    }

    @Test
    fun test2() {
        val instance = VerseHighlighter()
        instance.addInitText("  ")
        instance.addInitMarkup("<p>")
        instance.addInitMarkup("<font>")
        instance.addInitText(" a <title>  ")
        instance.addInitMarkup("</font>")
        instance.addInitMarkup("</p>")

        instance.beginProcessing()
        Assert.assertEquals("a <title>", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<font>", 0),
                Markup("</font>", 9),
                Markup("</p>", 9)
            ), instance.markupList
        )

        instance.finalizeProcessing()
        Assert.assertEquals("<p><font>a &lt;title&gt;</font></p>",
            instance.rawText.toString()
        )
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<font>", 3),
                Markup("</font>", 24),
                Markup("</p>", 31)
            ), instance.markupList
        )
    }

    @Test
    fun test3() {
        val instance = VerseHighlighter()
        instance.addInitText("  ")
        instance.addInitMarkup("<p>")
        instance.addInitMarkup("<font>")
        instance.addInitMarkup(Markup("hr", placeholder = "|a  title |"))
        instance.addInitMarkup("</font>")
        instance.addInitMarkup("</p>")

        instance.beginProcessing()
        Assert.assertEquals("|a  title |", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<font>", 0),
                Markup("hr", 0, placeholder = "|a  title |"),
                Markup("</font>", 11),
                Markup("</p>", 11)
            ), instance.markupList
        )

        instance.finalizeProcessing()
        Assert.assertEquals("<p><font>hr</font></p>", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<font>", 3),
                Markup("hr", 9, placeholder = "|a  title |"),
                Markup("</font>", 11),
                Markup("</p>", 18)
            ), instance.markupList
        )
    }

    @Test
    fun test4() {
        val instance = VerseHighlighter()
        instance.addInitText("s")
        instance.addInitMarkup("<p>")
        instance.addInitMarkup("<font>")
        instance.addInitMarkup(Markup("hr", placeholder = "|a  title |"))
        instance.addInitMarkup("</font>")
        instance.addInitMarkup("</p>")
        instance.addInitText("  ")

        instance.beginProcessing()
        Assert.assertEquals("s|a  title |", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 1),
                Markup("<font>", 1),
                Markup("hr", 1, placeholder ="|a  title |"),
                Markup("</font>", 12),
                Markup("</p>", 12)
            ), instance.markupList
        )

        instance.updateMarkup(1, "<span>", false)
        Assert.assertEquals(
            listOf(
                Markup("<p>", 1),
                Markup("<font>", 1),
                Markup("hr", 1, placeholder = "|a  title |"),
                Markup("<span>", 12, addedDuringUpdate = true),
                Markup("</font>", 12),
                Markup("</p>", 12)
            ), instance.markupList
        )

        instance.updateMarkup(12, "</span>", true)
        Assert.assertEquals(
            listOf(
                Markup("<p>", 1),
                Markup("<font>", 1),
                Markup("hr", 1, placeholder = "|a  title |"),
                Markup("<span>", 12, addedDuringUpdate = true),
                Markup("</span>", 12, addedDuringUpdate = true),
                Markup("</font>", 12),
                Markup("</p>", 12)
            ), instance.markupList
        )

        instance.finalizeProcessing()
        Assert.assertEquals("s<p><font>hr<span></span></font></p>", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 1),
                Markup("<font>", 4),
                Markup("hr", 10, placeholder = "|a  title |"),
                Markup("<span>", 12, addedDuringUpdate = true),
                Markup("</span>", 18, addedDuringUpdate = true),
                Markup("</font>", 25),
                Markup("</p>", 32)
            ), instance.markupList
        )
    }

    @Test
    fun test5() {
        val instance = VerseHighlighter()
        instance.addInitMarkup("<p>")
        instance.addInitText("font")
        instance.addInitMarkup(Markup("<hr>", placeholder = "line"))
        instance.addInitText("abbr\t")
        instance.addInitMarkup("</p>")
        instance.addInitText("\n")

        instance.beginProcessing()
        Assert.assertEquals("fontlineabbr", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<hr>", 4, placeholder = "line"),
                Markup("</p>", 12)
            ), instance.markupList
        )

        instance.updateMarkup(1, "<span>", false)
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<span>", 1, addedDuringUpdate = true),
                Markup("<hr>", 4, placeholder = "line"),
                Markup("</p>", 12)
            ), instance.markupList
        )

        instance.updateMarkup(4, "</span>", true)
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<span>", 1, addedDuringUpdate = true),
                Markup("</span>", 4, addedDuringUpdate = true),
                Markup("<hr>", 4, placeholder = "line"),
                Markup("</p>", 12)
            ), instance.markupList
        )

        instance.finalizeProcessing()
        Assert.assertEquals("<p>f<span>ont</span><hr>abbr</p>", instance.rawText.toString())
        Assert.assertEquals(
            listOf(
                Markup("<p>", 0),
                Markup("<span>", 4, addedDuringUpdate = true),
                Markup("</span>", 13, addedDuringUpdate = true),
                Markup("<hr>", 20, placeholder = "line"),
                Markup("</p>", 28)
            ), instance.markupList
        )
    }
}