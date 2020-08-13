package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.data.VerseHighlighter.Markup
import com.aaronicsubstances.niv1984.models.HighlightRange
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AppBinarySerializerTest {

    @Test
    fun testVerseMarkupSerialization() {
        val emptyList = listOf<Markup>()
        var expected = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0)
        var actualSerialized = AppBinarySerializer.serializeMarkups(emptyList)
        assertArrayEquals(expected, actualSerialized)
        var actualDeserialized = AppBinarySerializer.deserializeMarkups(actualSerialized)
        assertEquals(emptyList, actualDeserialized)

        val singleItemList = listOf(Markup("<span>", 10, "3", true))
        expected = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 1,
                0, 6, '<'.toByte(), 's'.toByte(), 'p'.toByte(), 'a'.toByte(), 'n'.toByte(),
                '>'.toByte(),
                0, 0, 0, 10,
                1, 0, 1, '3'.toByte(),
                1)
        actualSerialized = AppBinarySerializer.serializeMarkups(singleItemList)
        assertArrayEquals(expected, actualSerialized)
        actualDeserialized = AppBinarySerializer.deserializeMarkups(actualSerialized)
        assertEquals(singleItemList, actualDeserialized)

        val multipleItemList = listOf(Markup("<b>", 300, "yes", false),
                Markup("", 1000, null, true))
        expected = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 2,
                0, 3, '<'.toByte(), 'b'.toByte(), '>'.toByte(),
                0, 0, 0b0001, 0b00101100,
                1, 0, 3, 'y'.toByte(), 'e'.toByte(), 's'.toByte(),
                0,

                0, 0,
                0, 0, 0b0011, 0b11101000.toByte(),
                0,
                1)
        actualSerialized = AppBinarySerializer.serializeMarkups(multipleItemList)
        assertArrayEquals(expected, actualSerialized)
        actualDeserialized = AppBinarySerializer.deserializeMarkups(actualSerialized)
        assertEquals(multipleItemList, actualDeserialized)
    }

    @Test
    fun testHighlightRangeSerialization() {
        val emptyList = listOf<HighlightRange>()
        var expected = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0)
        var actualSerialized = AppBinarySerializer.serializeHighlightRanges(emptyList)
        assertArrayEquals(expected, actualSerialized)
        var actualDeserialized = AppBinarySerializer.deserializeHighlightRanges(actualSerialized)
        assertEquals(emptyList, actualDeserialized)

        val singleItemList = listOf(HighlightRange(0, 3))
        expected = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 1,
                0, 0, 0, 0, 0, 0, 0, 3)
        actualSerialized = AppBinarySerializer.serializeHighlightRanges(singleItemList)
        assertArrayEquals(expected, actualSerialized)
        actualDeserialized = AppBinarySerializer.deserializeHighlightRanges(actualSerialized)
        assertEquals(singleItemList, actualDeserialized)

        assertEquals(8, 0b1000)
        assertEquals(9, 0b1001)
        assertEquals(128, 0b10000000)

        val multipleItemList = listOf(HighlightRange(127, 128),
                HighlightRange(2, 150), HighlightRange(216, 417))
        expected = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 3,
                0, 0, 0, 127, 0, 0, 0, 0b10000000.toByte(),
                0, 0, 0, 2, 0, 0, 0, 0b10010110.toByte(),
                0, 0, 0, 0b11011000.toByte(), 0, 0, 0b0001, 0b10100001.toByte())
        actualSerialized = AppBinarySerializer.serializeHighlightRanges(multipleItemList)
        assertArrayEquals(expected, actualSerialized)
        actualDeserialized = AppBinarySerializer.deserializeHighlightRanges(actualSerialized)
        assertEquals(multipleItemList, actualDeserialized)
    }
}