package com.aaronicsubstances.niv1984.data

import android.content.Context
import android.text.TextUtils
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.parsing.BookParser
import com.aaronicsubstances.niv1984.parsing.BookParser.Chapter
import com.aaronicsubstances.niv1984.parsing.BookParser.ChapterFragment
import com.aaronicsubstances.niv1984.parsing.BookParser.ChapterFragmentKind
import com.aaronicsubstances.niv1984.parsing.BookParser.FancyContent
import com.aaronicsubstances.niv1984.parsing.BookParser.FancyContentKind
import com.aaronicsubstances.niv1984.parsing.BookParser.Note
import com.aaronicsubstances.niv1984.parsing.BookParser.NoteContent
import com.aaronicsubstances.niv1984.parsing.BookParser.NoteContentKind
import com.aaronicsubstances.niv1984.parsing.BookParser.NoteKind
import com.aaronicsubstances.niv1984.parsing.BookParser.NoteRef
import com.aaronicsubstances.niv1984.parsing.BookParser.Verse
import com.aaronicsubstances.niv1984.parsing.BookParser.WordsOfJesus
import com.aaronicsubstances.niv1984.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookLoader(private val context: Context,
                 private val bookNumber: Int,
                 var bibleVersions: List<String>) {

    private val wjColor = "red"

    suspend fun load(): BookDisplay {
        return withContext(Dispatchers.IO) {
            val rawChapters = loadRawBookAsset(bibleVersions[0])
            val chapterIndices = mutableListOf<Int>()
            val displayItems = processChapters(bibleVersions[0], rawChapters, chapterIndices)
            val book = BookDisplay(bookNumber, bibleVersions, displayItems, chapterIndices)
            book
        }
    }

    private fun loadRawBookAsset(bibleVersionCode: String): List<Chapter> {
        val assetPath = String.format("%s/%02d.xml", bibleVersionCode, bookNumber)
        return context.assets.open(assetPath).use {
            val parser = BookParser()
            parser.parse(it)
        }
    }

    private fun processChapters(bibleVersionCode: String,
                                rawChapters: List<Chapter>,
                                chapterIndices: MutableList<Int>): List<BookDisplayItem> {
        val displayItems = mutableListOf<BookDisplayItem>()
        for (rawChapter in rawChapters) {
            chapterIndices.add(displayItems.size)

            // add title item
            val titleText = AppConstants.bibleVersions.getValue(
                bibleVersionCode).getChapterTitle(bookNumber, rawChapter.chapterNumber)
            displayItems.add(
                BookDisplayItem(
                    bibleVersionCode, rawChapter.chapterNumber,
                    0, BookDisplayItemViewType.TITLE, 0, titleText))

            val footNotes = mutableListOf<BookDisplayItem>()

            var runningIndexInChapter = 1 // due to title
            for (part in rawChapter.parts) {
                when (part) {
                    is ChapterFragment -> {
                        // always increment so every part has unique number regardless of
                        // whether it appears in a single bible version display or
                        // multiple bible versions display
                        runningIndexInChapter++
                        // skip chapter fragments if displaying multiple bible
                        // versions
                        if (bibleVersions.size == 1) {
                            val item = processChapterFragment(
                                bibleVersionCode, rawChapter.chapterNumber,
                                part, runningIndexInChapter
                            )
                            displayItems.add(item)
                        }
                    }
                    is Verse -> {
                        runningIndexInChapter++
                        val item = processVerse(
                            bibleVersionCode, rawChapter.chapterNumber,
                            part, runningIndexInChapter
                        )
                        displayItems.add(item)
                    }
                    else -> {
                        part as Note
                        val footNoteItem = processNote(bibleVersionCode, rawChapter.chapterNumber,
                            part)
                        footNotes.add(footNoteItem)
                    }
                }
            }
            displayItems.add(
                BookDisplayItem(
                    bibleVersionCode, rawChapter.chapterNumber,
                    runningIndexInChapter++, BookDisplayItemViewType.DIVIDER, 0, ""
                )
            )
            compressFootNotes(footNotes)
            footNotes.forEach {
                it.indexInChapter = runningIndexInChapter++
                displayItems.add(it)
            }
            displayItems.add(
                BookDisplayItem(bibleVersionCode, rawChapter.chapterNumber,
                    runningIndexInChapter++, BookDisplayItemViewType.DIVIDER, 0, ""))
        }
        return displayItems
    }

    private fun processChapterFragment(
        bibleVersionCode: String,
        chapterNumber: Int,
        rawFragment: ChapterFragment,
        indexInChapter: Int
    ): BookDisplayItem {
        val viewType = if (rawFragment.kind == ChapterFragmentKind.HEADING) {
            BookDisplayItemViewType.HEADER
        }
        else {
            BookDisplayItemViewType.CHAPTER_FRAGMENT
        }

        val out = StringBuilder()
        for (part in rawFragment.parts) {
            when (part) {
                is NoteRef -> {
                    processNoteRef(bibleVersionCode, chapterNumber, part, out)
                }
                else -> {
                    part as FancyContent
                    processFancyContent(part, out)
                }
            }
        }
        return BookDisplayItem(bibleVersionCode, chapterNumber, indexInChapter,
            viewType, 0, out.toString())
    }

    private fun processVerse(
        bibleVersionCode: String,
        chapterNumber: Int,
        rawVerse: Verse,
        indexInChapter: Int
    ): BookDisplayItem {
        val out = StringBuilder()

        val selectedBibleVersion = AppConstants.bibleVersions.getValue(
            bibleVersionCode)
        if (bibleVersions.size > 1) {
            out.append("<strong>(${selectedBibleVersion.abbreviation}) </strong>")
        }
        out.append("${rawVerse.verseNumber}. ")
        for (part in rawVerse.parts) {
            when (part) {
                is WordsOfJesus -> {
                    out.append("<font color='$wjColor'>")
                    for (wjPart in part.parts) {
                        when (wjPart) {
                            is NoteRef -> {
                                processNoteRef(bibleVersionCode, chapterNumber, wjPart, out)
                            }
                            else -> {
                                wjPart as FancyContent
                                processFancyContent(wjPart, out)
                            }
                        }
                    }
                    out.append("<font>")
                }
                is NoteRef -> {
                    processNoteRef(bibleVersionCode, chapterNumber, part, out)
                }
                else -> {
                    part as FancyContent
                    processFancyContent(part, out)
                }
            }
        }
        return BookDisplayItem(bibleVersionCode, chapterNumber, indexInChapter,
            BookDisplayItemViewType.VERSE, rawVerse.verseNumber, out.toString())
    }

    private fun processFancyContent(rawContent: FancyContent, out: StringBuilder) {
        val escapedContent = TextUtils.htmlEncode(rawContent.content)
        when (rawContent.kind) {
            FancyContentKind.EM, FancyContentKind.SELAH -> {
                out.append("<em>$escapedContent</em>")
            }
            FancyContentKind.STRONG_EM -> {
                out.append("<strong>$escapedContent</strong>")
            }
            FancyContentKind.PICTOGRAM -> {
                out.append("<big>$escapedContent</big>")
            }
            else -> {
                out.append(escapedContent)
            }
        }
    }

    private fun processNoteRef(
        bibleVersionCode: String,
        chapterNumber: Int,
        rawNoteRef: NoteRef,
        out: StringBuilder
    ) {
        val lowerA = 'a'.toInt()
        val charRef = (lowerA + rawNoteRef.noteNumber -1).toChar()
        var text = "<sup><a href='$bibleVersionCode-$chapterNumber-${rawNoteRef.noteNumber}'>" +
            "$charRef</a></sup>"
        out.append(text)
    }

    private fun processNote(
        bibleVersionCode: String,
        chapterNumber: Int,
        rawNote: Note
    ): BookDisplayItem {
        val viewType = if (rawNote.kind == NoteKind.CROSS_REFERENCES) {
            BookDisplayItemViewType.CROSS_REFERENCES
        }
        else {
            BookDisplayItemViewType.FOOTNOTE
        }
        var out = StringBuilder()
        val lowerA = 'a'.toInt()
        val charRef = (lowerA + rawNote.noteNumber -1).toChar()
        out.append("<sup><b>$charRef</b></sup>")
        for (part in rawNote.parts) {
            val escapedContent = TextUtils.htmlEncode(part.body)
            when (part.kind) {
                NoteContentKind.STRONG_EM -> {
                    out.append("<strong>$escapedContent</strong>")
                }
                NoteContentKind.EM, NoteContentKind.REF_VERSE, NoteContentKind.REF_VERSE_START -> {
                    out.append("<em>$escapedContent</em>")
                }
                else -> {
                    out.append(escapedContent)
                }
            }
        }
        return BookDisplayItem(bibleVersionCode, chapterNumber, 0,
            viewType, 0, out.toString())
    }

    private fun compressFootNotes(footNotes: MutableList<BookDisplayItem>) {
        if (footNotes.isEmpty()) {
            return
        }
        //TODO("not implemented")
    }
}