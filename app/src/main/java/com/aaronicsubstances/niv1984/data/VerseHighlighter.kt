package com.aaronicsubstances.niv1984.data

class VerseHighlighter {
    companion object {
        private val WS_REGEX = Regex("\\s+")

        fun addHighlightRange(
            existingRanges: List<Pair<Int, Int>>,
            newRange: Pair<Int, Int>
        ): List<Pair<Int, Int>> {
            val editableRanges = clearHighlightRange(existingRanges, newRange)
            addNewHighlightRange(editableRanges, newRange)
            return optimizeHighlightRanges(editableRanges)
        }

        internal fun addNewHighlightRange(
            editableRanges: MutableList<Pair<Int, Int>>,
            newRange: Pair<Int, Int>) {
            // PostCondition: editable ranges remain sorted
            // assuming editableRanges is already sorted.

            // look for appropriate place to insert newRange,
            // using inner loop of insertion sort
            var i = 0
            while (i < editableRanges.size) {
                if (editableRanges[i].first > newRange.first) {
                    break
                }
                i++
            }
            editableRanges.add(i, newRange)
        }

        internal fun optimizeHighlightRanges(editableRanges: MutableList<Pair<Int, Int>>):
                List<Pair<Int, Int>> {
            // optimize number of ranges by merging adjacent ranges
            val indicesToRemove = mutableListOf<Int>()
            var i = 0
            while (i < editableRanges.size) {
                var (s, t) = editableRanges[i]
                var endI = i + 1 // ensure progress at all cost to avoid endless looping
                while (endI < editableRanges.size) {
                    val adj = editableRanges[endI]
                    if (t != adj.first) {
                        break
                    }
                    t = adj.second
                    endI++
                }
                if (endI - i > 1) {
                    (i + 1 until endI).forEach { indicesToRemove.add(it) }
                    editableRanges[i] = Pair(s, t)
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
            existingRanges: List<Pair<Int, Int>>,
            rangeToClear: Pair<Int, Int>
        ): List<Pair<Int, Int>> {
            return clearHighlightRange(existingRanges, rangeToClear)
        }

        internal fun clearHighlightRange(
            existingRanges: List<Pair<Int, Int>>,
            rangeToClear: Pair<Int, Int>
        ): MutableList<Pair<Int, Int>> {
            // PostCondition requirement: maintain disjoint/nonoverlapping property
            // assuming existingRanges is already disjoint.

            val editableRanges = mutableListOf<Pair<Int, Int>>()
            for (r in existingRanges) {
                // if r is entirely outside rangeToClear, readd it
                // if r is entirely inside rangeToClear, don't readd it.
                // if r overlaps rangeToClear, then readd one or both of left and right portions of it.
                if (r.second <= rangeToClear.first || r.first >= rangeToClear.second) {
                    editableRanges.add(r)
                }
                else {
                    if (r.first < rangeToClear.first) {
                        // add left portion outside of rangeToClear
                        editableRanges.add(Pair(r.first, rangeToClear.first))
                    }
                    if (r.second > rangeToClear.second) {
                        // add right portion outside of rangeToClear
                        editableRanges.add(Pair(rangeToClear.second, r.second))
                    }
                }
            }
            return editableRanges
        }
    }

    val rawText = StringBuilder()
    val markupList = mutableListOf<Markup>()
    private var positionAdjustment: Int = 0

    fun addInitText(s: String) {
        rawText.append(s)
    }

    fun addInitMarkup(id: String, m: String) {
        markupList.add(Markup(id, rawText.length, m))
    }

    fun addInitMarkupWithPlaceholder(id: String, m: String, placeholder: String) {
        markupList.add(Markup(id, rawText.length, m, placeholder))
    }

    fun beginProcessing() {
        positionAdjustment = 0

        // look for substrings in between markup positions and normalize them as HTML would.
        val markupPositions = markupList.map { it.pos }.toSet()
        var i = 0
        val originalRawTextLength = rawText.length
        while (i < originalRawTextLength) {
            val startI = i
            i++ // ensure progress at all cost to avoid endless looping
            while (i < originalRawTextLength && !markupPositions.contains(i)) {
                i++
            }

            // now replace all contiguous whitespace with single space, don't trim,
            // and shift all subsequent positions backwards
            val normalized = rawText.substring(
                startI + positionAdjustment,
                i + positionAdjustment
            ).replace(WS_REGEX, " ")
            rawText.replace(startI + positionAdjustment, i + positionAdjustment, normalized)
            val diff = normalized.length - (i - startI)
            if (diff < 0) {
                for (m in markupList) {
                    // deletions should only adjust positions beyond deletePos.
                    if (m.pos > startI) {
                        m.pos += diff
                    }
                }
                positionAdjustment += diff
            }
        }

        // reset position adjustment for inserting placeholders.
        positionAdjustment = 0
        for (m in markupList) {
            m.pos += positionAdjustment
            if (m.placeholder != null) {
                rawText.insert(m.pos + positionAdjustment, m.placeholder)
                positionAdjustment += m.placeholder.length
            }
        }

        // reset position adjustment for update
        positionAdjustment = 0
    }

    fun updateText(insertPos: Int, s: String, precedesSamePosMarkup: Boolean) {
        var samePosPlaceholderLen = 0
        for (m in markupList) {
            // validate
            if (m.placeholder != null && insertPos > m.pos
                    && insertPos < m.pos + m.placeholder.length) {
                throw IllegalArgumentException("Update operation invalid because it will " +
                        "edit placeholder markup $m")
            }
            // insertions should adjust positions beyond insertPos.
            // if insertPos equals mark up pos, then use boolean parameter
            // determine whether it is <s> <markups> (ie precedesSamePosMarkup = true)
            // or it is <markup> <s> (ie precedesSamePosMarkup = false).
            if (m.pos > insertPos) {
                m.pos += s.length
            }
            else if (m.pos == insertPos) {
                if (precedesSamePosMarkup) {
                    m.pos += s.length;
                }
                else if (m.placeholder != null && m.placeholder.isNotEmpty()) {
                    assert(samePosPlaceholderLen == 0)
                    samePosPlaceholderLen = m.placeholder.length
                }
            }
        }
        rawText.insert(insertPos + positionAdjustment + samePosPlaceholderLen, s)
        positionAdjustment += s.length
    }

    fun finalizeProcessing() {
        positionAdjustment = 0
        for (m in markupList) {
            val diff: Int
            if (m.placeholder == null) {
                rawText.insert(positionAdjustment + m.pos, m.tag)
                diff = m.tag.length
            }
            else {
                rawText.replace(positionAdjustment + m.pos,
                    positionAdjustment + m.pos + m.placeholder.length,
                    m.tag)
                diff = m.tag.length - m.placeholder.length
            }
            m.pos += positionAdjustment
            positionAdjustment += diff
        }
    }

    data class Markup(val id: String, var pos: Int, val tag: String, val placeholder: String? = null)
}