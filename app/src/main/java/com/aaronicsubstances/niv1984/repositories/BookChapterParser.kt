package com.aaronicsubstances.niv1984.repositories

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class BookChapterParser {

    data class Result(
        val parts: List<Any> // ChapterFragment | Verse
    )

    data class ChapterFragment (
        val kind: ChapterFragmentKind,
        val parts: List<Any> // FancyContent | Note
    )

    enum class ChapterFragmentKind {
        NONE, HEADING
    }

    data class Note(
        val kind: NoteKind,
        val parts: List<NoteContent>
    )

    enum class NoteKind {
        DEFAULT, CROSS_REFERENCES
    }

    data class NoteContent(
        val kind: NoteContentKind,
        val body: String
    )

    enum class NoteContentKind {
        NONE, EM, STRONG_EM,
        REF_VERSE_START, REF_VERSE
    }

    data class Verse(
        val verseNumber: Int,
        val parts: List<Any> // WordsOfJesus | FancyContent | Note
    )

    data class WordsOfJesus(
        val parts: List<Any> // FancyContent | Note
    )

    data class FancyContent(
        val kind: FancyContentKind,
        val content: String
    )

    enum class FancyContentKind {
        NONE, EM, STRONG_EM, SELAH
    }

    companion object {
        private val TAG_CHAPTER = "chapter"
        private val TAG_VERSE = "verse"
        private val TAG_CHAPTER_FRAGMENT = "fragment"
        private val TAG_NOTE = "note"
        private val TAG_ELEMENT_CONTENT = "content"
        private val TAG_WJ = "wj"
        private val ATTR_KIND = "kind"
        private val ATTR_NUMBER = "number"
    }

    fun parse(inputStream: InputStream, results: MutableList<Any>) {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()

        parser.require(XmlPullParser.START_TAG, null, TAG_CHAPTER)
        while (parser.nextTag() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                TAG_CHAPTER_FRAGMENT -> readChapterFragment(parser, results)
                TAG_VERSE -> readVerse(parser, results)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_CHAPTER)
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun readChapterFragment(parser: XmlPullParser, results: MutableList<Any>) {
        parser.require(XmlPullParser.START_TAG, null, TAG_CHAPTER_FRAGMENT)
        val kindAttr = parser.getAttributeValue(null, ATTR_KIND)
        val fragmentKind = if (kindAttr == null) {
            ChapterFragmentKind.NONE
        }
        else {
            try {
                enumValueOf<ChapterFragmentKind>(kindAttr)
            }
            catch (ex: IllegalArgumentException) {
                ChapterFragmentKind.NONE
            }
        }
        val parts = mutableListOf<Any>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                TAG_ELEMENT_CONTENT -> parts.add(readContent(parser))
                TAG_NOTE -> readNote(parser, parts)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_CHAPTER_FRAGMENT)
        results.add(ChapterFragment(fragmentKind, parts))
    }

    private fun readVerse(parser: XmlPullParser, results: MutableList<Any>) {
        parser.require(XmlPullParser.START_TAG, null, TAG_VERSE)
        val numAttr = parser.getAttributeValue(null, ATTR_NUMBER)
        val verseNum = Integer.parseInt(numAttr)
        val parts = mutableListOf<Any>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                TAG_WJ -> readWordsOfJesus(parser, parts)
                TAG_ELEMENT_CONTENT -> parts.add(readContent(parser))
                TAG_NOTE -> readNote(parser, parts)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_VERSE)
        results.add(Verse(verseNum, parts))
    }

    private fun readNote(parser: XmlPullParser, results: MutableList<Any>) {
        parser.require(XmlPullParser.START_TAG, null, TAG_NOTE)
        val kindAttr = parser.getAttributeValue(null, ATTR_KIND)
        val noteKind = if (kindAttr == null) {
            NoteKind.DEFAULT
        }
        else {
            try {
                enumValueOf<NoteKind>(kindAttr)
            }
            catch (ex: IllegalArgumentException) {
                NoteKind.DEFAULT
            }
        }
        val parts = mutableListOf<NoteContent>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                TAG_ELEMENT_CONTENT -> parts.add(readNoteBody(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_NOTE)
        results.add(Note(noteKind, parts))
    }

    private fun readWordsOfJesus(parser: XmlPullParser, results: MutableList<Any>) {
        parser.require(XmlPullParser.START_TAG, null, TAG_WJ)
        val parts = mutableListOf<Any>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                TAG_ELEMENT_CONTENT -> parts.add(readContent(parser))
                TAG_NOTE -> readNote(parser, parts)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, null, TAG_WJ)
        results.add(WordsOfJesus(parts))
    }

    private fun readContent(parser: XmlPullParser): FancyContent {
        parser.require(XmlPullParser.START_TAG, null, TAG_ELEMENT_CONTENT)
        val kindAttr = parser.getAttributeValue(null, ATTR_KIND)
        val contentKind = if (kindAttr == null) {
            FancyContentKind.NONE
        }
        else {
            try {
                enumValueOf<FancyContentKind>(kindAttr.toUpperCase())
            }
            catch (ex: IllegalArgumentException) {
                FancyContentKind.NONE
            }
        }
        val text = readText(parser, TAG_ELEMENT_CONTENT)
        return FancyContent(contentKind, text)
    }

    private fun readNoteBody(parser: XmlPullParser): NoteContent {
        parser.require(XmlPullParser.START_TAG, null, TAG_ELEMENT_CONTENT)
        val kindAttr = parser.getAttributeValue(null, ATTR_KIND)
        val noteBodyKind = if (kindAttr == null) {
            NoteContentKind.NONE
        }
        else {
            try {
                enumValueOf<NoteContentKind>(kindAttr.toUpperCase())
            }
            catch (ex: IllegalArgumentException) {
                NoteContentKind.NONE
            }
        }
        val text = readText(parser, TAG_ELEMENT_CONTENT)
        return NoteContent(noteBodyKind, text)
    }

    private fun readText(parser: XmlPullParser, tagName: String): String {
        parser.require(XmlPullParser.START_TAG, null, tagName)
        var result = StringBuilder()
        while (parser.next() != XmlPullParser.END_TAG) {
            result.append(parser.text)

        }
        parser.require(XmlPullParser.END_TAG, null, tagName)
        return result.toString()
    }
}