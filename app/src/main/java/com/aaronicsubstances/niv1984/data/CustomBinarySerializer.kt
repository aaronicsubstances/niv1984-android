package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.utils.AppUtils
import java.io.*

object CustomBinarySerializer {
    private const val SERIALIZER_VERSION = 1

    fun serializeMarkups(markups: List<VerseHighlighter.Markup>): ByteArray {
        val inMemDest = ByteArrayOutputStream()
        DataOutputStream(inMemDest).use { binaryWriter ->
            binaryWriter.writeInt(SERIALIZER_VERSION)
            binaryWriter.writeInt(markups.size)
            for (m in markups) {
                binaryWriter.writeUTF(m.tag)
                binaryWriter.writeInt(m.pos)
                binaryWriter.writeBoolean(m.placeholder != null)
                if (m.placeholder != null) {
                    binaryWriter.writeUTF(m.placeholder)
                }
                binaryWriter.writeBoolean(m.id != null)
                if (m.id != null) {
                    binaryWriter.writeUTF(m.id)
                }
                binaryWriter.writeBoolean(m.addedDuringUpdate)
                binaryWriter.writeBoolean(m.removeDuringHighlighting)
            }
        }
        return inMemDest.toByteArray()
    }

    fun deserializeMarkups(blob: ByteArray): List<VerseHighlighter.Markup> {
        val markups = mutableListOf<VerseHighlighter.Markup>()
        val inMemSrc = ByteArrayInputStream(blob)
        DataInputStream(inMemSrc).use { binaryReader ->
            val serializerVersionUsed = binaryReader.readInt()
            AppUtils.assert(serializerVersionUsed == SERIALIZER_VERSION)
            val markupCount = binaryReader.readInt()
            (0 until markupCount).forEach {
                val tag = binaryReader.readUTF()
                val pos = binaryReader.readInt()
                val placeholderPresent = binaryReader.readBoolean()
                val placeholder = if (!placeholderPresent) null else {
                    binaryReader.readUTF()
                }
                val idPresent = binaryReader.readBoolean()
                val id = if (!idPresent) null else {
                    binaryReader.readUTF()
                }
                val addedDuringUpdate = binaryReader.readBoolean()
                val removeDuringHighlighting = binaryReader.readBoolean()
                val m = VerseHighlighter.Markup(tag, pos, placeholder,
                        id, addedDuringUpdate, removeDuringHighlighting)
                markups.add(m)
            }
        }
        return markups
    }

    fun serializeHighlightRanges(ranges: List<HighlightRange>): ByteArray {
        val inMemDest = ByteArrayOutputStream()
        DataOutputStream(inMemDest).use { binaryWriter ->
            binaryWriter.writeInt(SERIALIZER_VERSION)
            binaryWriter.writeInt(ranges.size)
            for (m in ranges) {
                binaryWriter.writeInt(m.startIndex)
                binaryWriter.writeInt(m.endIndex)
            }
        }
        return inMemDest.toByteArray()
    }

    fun deserializeHighlightRanges(blob: ByteArray): List<HighlightRange> {
        val ranges = mutableListOf<HighlightRange>()
        val inMemSrc = ByteArrayInputStream(blob)
        DataInputStream(inMemSrc).use { binaryReader ->
            val serializerVersionUsed = binaryReader.readInt()
            AppUtils.assert(serializerVersionUsed == SERIALIZER_VERSION)
            val rangeCount = binaryReader.readInt()
            (0 until rangeCount).forEach {
                val startIndex = binaryReader.readInt()
                val endIndex = binaryReader.readInt()
                val m = HighlightRange(startIndex, endIndex)
                ranges.add(m)
            }
        }
        return ranges
    }
}