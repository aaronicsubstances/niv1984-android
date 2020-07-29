package com.aaronicsubstances.niv1984.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.SearchResult
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class AppUtilsTest {

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
        val serialized = AppUtils.serializeAsJson(expected)
        //println("Serialized: $serialized")
        val actual = AppUtils.deserializeFromJson(serialized, expected.javaClass)
        assertEquals(expected, actual)
    }
}