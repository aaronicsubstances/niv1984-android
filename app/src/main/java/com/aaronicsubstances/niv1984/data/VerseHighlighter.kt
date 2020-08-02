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
        // look for substrings in between markup positions and normalize them as HTML would,
        // by replacing all contiguous whitespace with single space. Do not trim.
        val normalizingFn: (s: String)->String = { it.replace(WS_REGEX, " ") }
        val markupPositions = mutableMapOf<Int, Int?>()
        for (m in markupList) {
            markupPositions[m.pos] = null
        }
        transformRawText(markupPositions, normalizingFn)

        // insert placeholders.
        var positionAdjustment = 0
        for (m in markupList) {
            m.pos += positionAdjustment
            if (m.placeholder != null) {
                rawText.insert(m.pos + positionAdjustment, m.placeholder)
                positionAdjustment += m.placeholder.length
            }
        }
    }

    /*
     * Look for substrings in between markup positions and transform them.
     */
    private fun transformRawText(markupPositions: Map<Int, Int?>,
                                 transformer: (s: String)->String) {
        var positionAdjustment = 0
        val originalRawTextLength = rawText.length
        var i = 0
        while (i < originalRawTextLength) {
            val startI = i
            i++ // ensure progress at all cost to avoid endless looping
            while (i < originalRawTextLength && !markupPositions.containsKey(i)) {
                i++
            }

            val transformed = transformer(rawText.substring(
                startI + positionAdjustment,
                i + positionAdjustment
            ))
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
        }
    }

    /*
     * NB: expected usage is to always call this method with different insertPos value.
     */
    fun updateMarkup(insertPos: Int, tag: String, precedesSamePosMarkup: Boolean) {
        var mIdx = 0
        var samePosPlaceholderLen = 0
        while (mIdx < markupList.size) {
            val m = markupList[mIdx]
            // validate
            if (m.placeholder != null && insertPos > m.pos
                    && insertPos < m.pos + m.placeholder.length) {
                throw IllegalArgumentException("Update operation invalid because it will " +
                        "edit placeholder markup ${mIdx}. $m")
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
            "",
            insertPos + samePosPlaceholderLen,
            tag,
            addedDuringUpdate = true
        ))
    }

    internal fun escapeHtmlSections(escapeFn: (s: String) -> String) {
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
        transformRawText(markupPositions, escapeFn)
    }

    /*
     * NB: escapeFn is used to avoid need to mock TextUtils.htmlEncode
     */
    fun finalizeProcessing(escapeFn: ((s: String) -> String)? = null) {
        escapeFn?.let{ escapeHtmlSections(it) }

        // this code uses the same ideas from SourceCodeTransformer.
        var positionAdjustment = 0
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

    data class Markup(
        val id: String,
        var pos: Int,
        val tag: String,
        val placeholder: String? = null,
        val addedDuringUpdate: Boolean = false
    )
}