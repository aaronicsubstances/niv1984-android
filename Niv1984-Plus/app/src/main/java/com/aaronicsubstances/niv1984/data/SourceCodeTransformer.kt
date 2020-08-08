package com.aaronicsubstances.niv1984.data

class SourceCodeTransformer {

    constructor(originalText: String): this(StringBuilder(originalText))

    constructor(originalText: StringBuilder) {
        transformedText = originalText
    }

    /**
     * Gets string resulting from modification of original string by replacements.
     */
    val transformedText: StringBuilder

    /**
     * Gets offset that can be added to an index into original string to
     * get corresponding position in modified string. As long as index does not point
     * within any section which has been changed, that index can be used as
     * a position into modified string using this property.
     */
    var positionAdjustment = 0

    /**
     * Inserts a string into original string and updates adjusting offset information.
     * @param replacement string to insert.
     * @param startPos position in original string to insert at.
     */
    fun addTransform(replacement: String, startPos: Int) {
        transformedText.insert(positionAdjustment + startPos, replacement)
        positionAdjustment += replacement.length
        // alternatively,
        //addTransform(replacement, startPos, startPos);
    }

    /**
     * Changes a section in original string and updates adjusting offset information.
     * @param replacement replacement for string section.
     * @param startPos starting position (inclusive) of original string section
     * @param endPos ending position (exclusive) of original string section
     */
    fun addTransform(replacement: String, startPos: Int, endPos: Int) {
        transformedText.replace(
            positionAdjustment + startPos, positionAdjustment + endPos,
            replacement
        )
        val diff = replacement.length - (endPos - startPos)
        positionAdjustment += diff
    }
}