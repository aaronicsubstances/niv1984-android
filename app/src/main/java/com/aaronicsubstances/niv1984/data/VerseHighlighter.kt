package com.aaronicsubstances.niv1984.data

class VerseHighlighter {
    val rawText = StringBuilder()
    val markupList = mutableListOf<Markup>()

    fun appendText(s: String) {
        rawText.append(s)
    }

    fun appendMarkup(id: Int, m: String) {
        markupList.add(Markup(rawText.length, id, m))
    }

    data class Markup(val pos: Int, val id: Int, val m: String)
}