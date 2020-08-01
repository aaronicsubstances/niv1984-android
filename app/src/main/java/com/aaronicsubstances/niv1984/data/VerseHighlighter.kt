package com.aaronicsubstances.niv1984.data

class VerseHighlighter {
    companion object {
        private val WS_REGEX = Regex("\\s+")
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
                    // deletions should only adjust positions beyond i.
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