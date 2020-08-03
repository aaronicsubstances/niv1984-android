package com.aaronicsubstances.niv1984.utils

import android.text.Html
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.models.SearchResult
import com.aaronicsubstances.niv1984.models.UserHighlightData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUtilsInstrumentedTest {

    @Test
    fun testColorResToString() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        var expected = "#D81B60"
        var actual = AppUtils.colorResToString(R.color.colorAccent, appContext)
        assertEquals(expected, actual)

        expected = if (AppUtils.isNightMode(appContext)) "#87654321" else "#12345678"
        actual = AppUtils.colorResToString(R.color.testColor, appContext)
        assertEquals(expected, actual)
    }

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
        var serialized = AppUtils.serializeAsJson(expected)
        //println("Serialized: $serialized")
        val actual = AppUtils.deserializeFromJson(serialized, expected.javaClass)
        assertEquals(expected, actual)

        val expectedHighlight = UserHighlightData.ChapterData(1,
            arrayOf(UserHighlightData.VerseBlockData(1, 0, arrayOf(HighlightRange(0, 1)))))
        serialized = AppUtils.serializeAsJson(expectedHighlight)
        val actualHighlight = AppUtils.deserializeFromJson(serialized, expectedHighlight.javaClass)
        assertEquals(expectedHighlight, actualHighlight)
    }

    @Test
    fun testHtmlNormalisation() {
        var expected = "Amen "
        var actual = AppUtils.parseHtml("<span> Amen </span>").toString()
        assertEquals(expected, actual)

        expected = "Amen "
        actual = AppUtils.parseHtml(" Amen ").toString()
        assertEquals(expected, actual)

        expected = "Amen "
        actual = AppUtils.parseHtml("     \nAmen \n ").toString()
        assertEquals(expected, actual)

        expected = "And Amen !"
        actual = AppUtils.parseHtml("And<span> Amen\n </span>!").toString()
        assertEquals(expected, actual)

        expected = "And Amen !"
        actual = AppUtils.parseHtml("And <span> Amen\n </span> !").toString()
        assertEquals(expected, actual)

        expected = "And Amen !"
        actual = AppUtils.parseHtml("And<span> Amen\n </span>!").toString()
        assertEquals(expected, actual)

        expected = "And Amen ! "
        actual = AppUtils.parseHtml("\nAnd\n<span>\nAmen\n\n</span>\n!\n").toString()
        assertEquals(expected, actual)

        expected = "\u00a0Amen\u00a0\u00a0 "
        actual = AppUtils.parseHtml("  \u00a0Amen\u00a0\u00a0  ").toString()
        assertEquals(expected, actual)

        expected = "<Amen'"
        actual = AppUtils.parseHtml("&lt;Amen&apos;").toString()
        assertEquals(expected, actual)

        expected = "6 "
        actual = AppUtils.parseHtml(" <b>6</b> ").toString()
        assertEquals(expected, actual)

        expected = "<  \n>"
        actual = AppUtils.parseHtml("&lt;<hr1>&gt;",
            Html.TagHandler { opening, tag, output, _ ->
                if (tag == "hr1" && opening) {
                    output.append("  \n")
                }
            }).toString()
        assertEquals(expected, actual)
    }
}