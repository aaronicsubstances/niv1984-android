package com.aaronicsubstances.niv1984.data

import android.text.Html
import android.text.TextUtils
import com.aaronicsubstances.niv1984.models.HighlightRange
import com.aaronicsubstances.niv1984.utils.AppUtils

class VerseHighlighter {
    companion object {
        private val TEMP_TAG_PREFIX = "t_"
        private val WS_REGEX = Regex("\\s")
        private val NBSP = "\u00a0"

        fun addHighlightRange(
            existingRanges: Array<HighlightRange>,
            newRange: HighlightRange
        ): List<HighlightRange> {
            val editableRanges = clearHighlightRange(existingRanges, newRange)
            addNewHighlightRange(editableRanges, newRange)
            return optimizeHighlightRanges(editableRanges)
        }

        internal fun addNewHighlightRange(
            editableRanges: MutableList<HighlightRange>,
            newRange: HighlightRange) {
            // PostCondition: editable ranges remain sorted
            // assuming editableRanges is already sorted.

            // look for appropriate place to insert newRange,
            // using inner loop of insertion sort
            var i = 0
            while (i < editableRanges.size) {
                if (editableRanges[i].startIndex > newRange.startIndex) {
                    break
                }
                i++
            }
            editableRanges.add(i, newRange)
        }

        internal fun optimizeHighlightRanges(editableRanges: MutableList<HighlightRange>):
                List<HighlightRange> {
            // optimize number of ranges by merging adjacent ranges
            val indicesToRemove = mutableListOf<Int>()
            var i = 0
            while (i < editableRanges.size) {
                var (s, t) = editableRanges[i]
                var endI = i + 1 // ensure progress at all cost to avoid endless looping
                while (endI < editableRanges.size) {
                    val adj = editableRanges[endI]
                    if (t != adj.startIndex) {
                        break
                    }
                    t = adj.endIndex
                    endI++
                }
                if (endI - i > 1) {
                    (i + 1 until endI).forEach { indicesToRemove.add(it) }
                    editableRanges[i] = HighlightRange(s, t)
                }
                i = endI
            }
            if (indicesToRemove.isEmpty()) {
                return editableRanges
            }
            return editableRanges.filterIndexed { index, _ ->
                !indicesToRemove.contains(index)
            }
        }

        fun removeHighlightRange(
            existingRanges: Array<HighlightRange>,
            rangeToClear: HighlightRange
        ): List<HighlightRange> {
            return clearHighlightRange(existingRanges, rangeToClear)
        }

        internal fun clearHighlightRange(
            existingRanges: Array<HighlightRange>,
            rangeToClear: HighlightRange
        ): MutableList<HighlightRange> {
            // PostCondition requirement: maintain disjoint/nonoverlapping property
            // assuming existingRanges is already disjoint.

            val editableRanges = mutableListOf<HighlightRange>()
            for (r in existingRanges) {
                // if r is entirely outside rangeToClear, readd it
                // if r is entirely inside rangeToClear, don't readd it.
                // if r overlaps rangeToClear, then readd one or both of left and right portions of it.
                if (r.endIndex <= rangeToClear.startIndex || r.startIndex >= rangeToClear.endIndex) {
                    editableRanges.add(r)
                }
                else {
                    if (r.startIndex < rangeToClear.startIndex) {
                        // add left portion outside of rangeToClear
                        editableRanges.add(HighlightRange(r.startIndex, rangeToClear.startIndex))
                    }
                    if (r.endIndex > rangeToClear.endIndex) {
                        // add right portion outside of rangeToClear
                        editableRanges.add(HighlightRange(rangeToClear.endIndex, r.endIndex))
                    }
                }
            }
            return editableRanges
        }
    }

    val rawText = StringBuilder()
    val markupList = mutableListOf<Markup>()

    fun addInitText(s: String): VerseHighlighter {
        // for some reason Html.fromHtml() does not collapse tabs into space,
        // so explicitly perform replacement, and allow only &nbsp; as exception
        // to whitespace collapsing.
        rawText.append(TextUtils.htmlEncode(s.replace(WS_REGEX) {
            if (it.value == NBSP) NBSP else " "
        }))
        return this
    }

    fun addInitMarkup(tag: String): VerseHighlighter {
        return addInitMarkup(Markup(tag))
    }

    fun addInitMarkup(m: Markup): VerseHighlighter {
        rawText.append("<$TEMP_TAG_PREFIX${markupList.size} />")
        markupList.add(m)
        return this
    }

    fun isEmpty(): Boolean {
        return rawText.isEmpty()
    }

    fun clear() {
        rawText.setLength(0)
        markupList.clear()
    }

    fun beginProcessing() {
        val rawHtml = AppUtils.parseHtml("<body>$rawText</body>",
            Html.TagHandler { opening, tag, output, _ ->
                if (opening && tag.startsWith(TEMP_TAG_PREFIX)) {
                    val idx = Integer.parseInt(tag.substring(TEMP_TAG_PREFIX.length))
                    val m = markupList[idx]
                    m.pos = output.length
                    if (m.placeholder != null) {
                        output.append(m.placeholder)
                    }
                }
            }).toString()
        rawText.clear()
        rawText.append(rawHtml)

        /*
         * NB: for some reason, Html.fromHtml() trims leading whitespace, but
         * collapses trailing whitespace to single space character.
         * Hence remove trailing space.
         */
        if (rawText.isNotEmpty() && rawText[rawText.length - 1] == ' ') {
            var i = markupList.size - 1
            while (i >= 0) {
                if (markupList[i].pos != rawText.length) {
                    break
                }
                markupList[i].pos--
                i--
            }
            rawText.setLength(rawText.length - 1)
        }
    }

    fun updateMarkup(insertPos: Int, tag: String, precedesSamePosMarkup: Boolean) {
        if (insertPos < 0 || insertPos > rawText.length) {
            throw IllegalArgumentException("Invalid insert position: $insertPos")
        }
        var mIdx = 0
        var samePosPlaceholderLen = 0
        while (mIdx < markupList.size) {
            val m = markupList[mIdx]
            // validate
            if (m.placeholder != null && insertPos > m.pos
                    && insertPos < m.pos + m.placeholder.length) {
                throw IllegalArgumentException("Update operation invalid because it will " +
                        "edit placeholder markup at index ${mIdx}: $m")
            }
            // if insertPos equals a markup pos, then resolve duplication using boolean parameter.
            // Determine whether it is <s> <markups> (ie precedesSamePosMarkup = true)
            // or it is <markup> <s> (ie precedesSamePosMarkup = false).
            if (insertPos < m.pos) {
                break
            }
            if (insertPos == m.pos) {
                if (precedesSamePosMarkup) {
                    // cannot precede another markup also added during update at same position.
                    if (!m.addedDuringUpdate) {
                        break
                    }
                }
                else {
                    // to properly follow a placeholder markup at same pos,
                    // insert pos must be edited to point beyond it.
                    if (m.placeholder != null) {
                        assert(samePosPlaceholderLen == 0)
                        samePosPlaceholderLen = m.placeholder.length
                    }
                }
            }
            mIdx++
        }
        markupList.add(mIdx, Markup(
            tag,
            insertPos + samePosPlaceholderLen,
            addedDuringUpdate = true
        ))
    }

    fun finalizeProcessing() {
        // first escape raw text in between markup positions.
        escapeHtmlSections()

        val transformer = SourceCodeTransformer(rawText)
        for (m in markupList) {
            val newPos = transformer.positionAdjustment
            if (m.placeholder == null) {
                transformer.addTransform(m.tag, m.pos)
            }
            else {
                transformer.addTransform(m.tag, m.pos, m.pos + m.placeholder.length)
            }
            m.pos += newPos
        }
    }

    internal fun escapeHtmlSections() {
        val markupPositions = mutableMapOf<Int, Int?>()
        for (m in markupList) {
            // don't overwrite placeholder positions with non-placeholder ones.
            if (m.placeholder != null && m.placeholder.isNotEmpty()) {
                markupPositions[m.pos] = m.pos + m.placeholder.length
            }
            else if (!markupPositions.containsKey(m.pos)) {
                markupPositions[m.pos] = null
            }
        }

        var positionAdjustment = 0
        val originalRawTextLength = rawText.length
        var i = 0
        while (i < originalRawTextLength) {
            val startI = i
            while (i < originalRawTextLength && !markupPositions.containsKey(i)) {
                i++
            }

            val subSection = rawText.substring(
                startI + positionAdjustment,
                i + positionAdjustment
            )
            val transformed = TextUtils.htmlEncode(subSection)
            rawText.replace(startI + positionAdjustment, i + positionAdjustment, transformed)
            val diff = transformed.length - (i - startI)
            if (diff != 0) {
                for (m in markupList) {
                    // Only adjust positions beyond replacement position.
                    if (m.pos > startI + positionAdjustment) {
                        m.pos += diff
                    }
                }
                positionAdjustment += diff
            }
            if (i < originalRawTextLength) {
                val endI = markupPositions.getValue(i)
                if (endI != null) {
                    i = endI
                }
            }
            // deal with empty strings, ie adjacent markup positions, and
            // ensure progress at all cost to avoid endless looping
            if (i == startI) {
                i++
            }
        }
    }

    /*
     * A tag or placeholder must not begin or end with whitespace to enable correct
     * predictions of markup positions.
     */
    data class Markup(
        val tag: String,
        var pos: Int = 0,
        val placeholder: String? = null,
        val id: String? = null,
        val addedDuringUpdate: Boolean = false
    )
}