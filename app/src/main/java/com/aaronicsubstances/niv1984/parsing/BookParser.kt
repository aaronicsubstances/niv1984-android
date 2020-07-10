package com.aaronicsubstances.niv1984.parsing

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class BookParser {

    enum class ChapterFragmentKind {
        NONE, HEADING
    }

    enum class NoteKind {
        DEFAULT, CROSS_REFERENCES
    }

    enum class NoteContentKind {
        NONE, EM, STRONG_EM,
        REF_VERSE_START, REF_VERSE
    }

    enum class FancyContentKind {
        NONE, EM, STRONG_EM, SELAH
    }

    data class Chapter(
        val chapterNumber: Int,
        val parts: List<Any> // ChapterFragment | Verse | Note
    )

    data class ChapterFragment (
        val kind: ChapterFragmentKind,
        val parts: List<Any> // FancyContent | NoteRef
    )

    data class NoteRef(
        val noteNumber: Int
    )

    data class Note(
        val noteNumber: Int,
        val kind: NoteKind,
        val parts: List<NoteContent>
    )

    data class NoteContent(
        val kind: NoteContentKind,
        val body: String
    )

    data class Verse(
        val verseNumber: Int,
        val parts: List<Any> // WordsOfJesus | FancyContent | NoteRef
    )

    data class WordsOfJesus(
        val parts: List<Any> // FancyContent | NoteRef
    )

    data class FancyContent(
        val kind: FancyContentKind,
        val content: String
    )

    companion object {
        private const val TAG_BOOK = "book"
        private const val TAG_CHAPTER = "chapter"
        private const val TAG_VERSE = "verse"
        private const val TAG_CHAPTER_FRAGMENT = "fragment"
        private const val TAG_ELEMENT_CONTENT = "content"
        private const val TAG_WJ = "wj"
        private const val ATTR_KIND = "kind"
        private const val ATTR_NUMBER = "num"

        private const val TAG_NOTE = "note"
        private const val TAG_NOTE_REF = "note_ref"
    }

    fun parse(inputStream: InputStream): List<Chapter> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()
        val results = mutableListOf<Chapter>()
        parseBook(parser, results)
        return results
    }

    private inline fun parseElement(parser: XmlPullParser,
                                    elementName: String,
                                    processingFun: () -> Boolean) {
        parser.require(XmlPullParser.START_TAG, null, elementName)
        while (parser.nextTag() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            val processed = processingFun()
            if (!processed) {
                skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, null, elementName)
    }

    private fun skip(parser: XmlPullParser) {
        check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun parseBook(parser: XmlPullParser, results: MutableList<Chapter>) {
        parseElement(parser, TAG_BOOK) {
            when(parser.name) {
                TAG_CHAPTER -> {
                    val numAttr = parser.getAttributeValue(null, ATTR_NUMBER)
                    val chapterNum = Integer.parseInt(numAttr)
                    val chapterResults = mutableListOf<Any>();
                    parseChapter(parser, chapterResults)
                    results.add(Chapter(chapterNum, chapterResults))
                    true
                }
                else -> false
            }
        }
    }

    private fun parseChapter(parser: XmlPullParser, results: MutableList<Any>) {
        parseElement(parser, TAG_CHAPTER) {
            when(parser.name) {
                TAG_VERSE -> {
                    readVerse(parser, results)
                    true
                }
                TAG_CHAPTER_FRAGMENT -> {
                    readChapterFragment(parser, results)
                    true
                }
                TAG_NOTE -> {
                    readNote(parser, results)
                    true
                }
                else -> false
            }
        }
    }

    private fun readChapterFragment(parser: XmlPullParser, results: MutableList<Any>) {
        val kindAttr = parser.getAttributeValue(null, ATTR_KIND)
        val fragmentKind = if (kindAttr == null) {
            ChapterFragmentKind.NONE
        }
        else {
            try {
                enumValueOf<ChapterFragmentKind>(kindAttr.toUpperCase())
            }
            catch (ex: IllegalArgumentException) {
                ChapterFragmentKind.NONE
            }
        }
        val parts = mutableListOf<Any>()
        parseElement(parser, TAG_CHAPTER_FRAGMENT) {
            when (parser.name) {
                TAG_ELEMENT_CONTENT -> {
                    parts.add(readContent(parser))
                    true
                }
                TAG_NOTE_REF -> {
                    parts.add(readNoteRef(parser))
                    true
                }
                else -> false
            }
        }
        results.add(ChapterFragment(fragmentKind, parts))
    }

    private fun readVerse(parser: XmlPullParser, results: MutableList<Any>) {
        val numAttr = parser.getAttributeValue(null, ATTR_NUMBER)
        val verseNum = Integer.parseInt(numAttr)
        val parts = mutableListOf<Any>()
        parseElement(parser, TAG_VERSE) {
            when (parser.name) {
                TAG_WJ -> {
                    readWordsOfJesus(parser, parts)
                    true
                }
                TAG_ELEMENT_CONTENT -> {
                    parts.add(readContent(parser))
                    true
                }
                TAG_NOTE_REF -> {
                    parts.add(readNoteRef(parser))
                    true
                }
                else -> false
            }
        }
        results.add(Verse(verseNum, parts))
    }

    private fun readWordsOfJesus(parser: XmlPullParser, results: MutableList<Any>) {
        val parts = mutableListOf<Any>()
        parseElement(parser, TAG_WJ) {
            when (parser.name) {
                TAG_ELEMENT_CONTENT -> {
                    parts.add(readContent(parser))
                    true
                }
                TAG_NOTE_REF -> {
                    parts.add(readNoteRef(parser))
                    true
                }
                else -> false
            }
        }
        results.add(WordsOfJesus(parts))
    }

    private fun readNoteRef(parser: XmlPullParser): NoteRef {
        parser.require(XmlPullParser.START_TAG, null, TAG_NOTE_REF)
        val text = readText(parser, TAG_NOTE_REF)
        val noteNum = Integer.parseInt(text)
        return NoteRef(noteNum)
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

    private fun readNote(parser: XmlPullParser, results: MutableList<Any>) {
        val noteNumText = parser.getAttributeValue(null, ATTR_NUMBER)
        val noteNum = if (noteNumText != null) Integer.parseInt(noteNumText) else 0
        val kindAttr = parser.getAttributeValue(null, ATTR_KIND)
        val noteKind = if (kindAttr == null) {
            NoteKind.DEFAULT
        }
        else {
            try {
                enumValueOf<NoteKind>(kindAttr.toUpperCase())
            }
            catch (ex: IllegalArgumentException) {
                NoteKind.DEFAULT
            }
        }
        val parts = mutableListOf<NoteContent>()
        parseElement(parser, TAG_NOTE) {
            when (parser.name) {
                TAG_ELEMENT_CONTENT -> {
                    parts.add(readNoteBody(parser))
                    true
                }
                else -> false
            }
        }
        results.add(Note(noteNum, noteKind, parts))
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