package com.aaronicsubstances.niv1984.data

import android.content.Context
import android.text.TextUtils
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.parsing.BookParser
import com.aaronicsubstances.niv1984.parsing.BookParser.BlockQuote
import com.aaronicsubstances.niv1984.parsing.BookParser.Chapter
import com.aaronicsubstances.niv1984.parsing.BookParser.ChapterFragment
import com.aaronicsubstances.niv1984.parsing.BookParser.ChapterFragmentKind
import com.aaronicsubstances.niv1984.parsing.BookParser.FancyContent
import com.aaronicsubstances.niv1984.parsing.BookParser.FancyContentKind
import com.aaronicsubstances.niv1984.parsing.BookParser.Note
import com.aaronicsubstances.niv1984.parsing.BookParser.NoteContentKind
import com.aaronicsubstances.niv1984.parsing.BookParser.NoteKind
import com.aaronicsubstances.niv1984.parsing.BookParser.NoteRef
import com.aaronicsubstances.niv1984.parsing.BookParser.Verse
import com.aaronicsubstances.niv1984.parsing.BookParser.WordsOfJesus
import com.aaronicsubstances.niv1984.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.AssertionError
import kotlin.math.min

class BookLoader(private val context: Context,
                 private val bookNumber: Int,
                 val bibleVersions: List<String>,
                 private val wjColor: String = "red",
                 private val displayMultipleSideBySide: Boolean = false) {

    suspend fun load(): BookDisplay {
        return withContext(Dispatchers.IO) {
            val rawChapters = loadRawBookAsset(bibleVersions[0])
            val chapterIndices = mutableListOf<Int>()
            val displayItems = processChapters(bibleVersions[0], rawChapters, chapterIndices)

            val book = if (bibleVersions.size > 1) {
                val rawChapters2 = loadRawBookAsset(bibleVersions[1])
                val chapterIndices2 = mutableListOf<Int>()
                val displayItems2 = processChapters(bibleVersions[1], rawChapters2, chapterIndices2)
                val totalChapterCount = AppConstants.BIBLE_BOOK_CHAPTER_COUNT[bookNumber - 1]
                val combinedDisplayItems = mutableListOf<BookDisplayItem>()
                val combinedChapterIndices = mutableListOf<Int>()
                (0 until totalChapterCount).forEach() {
                    combinedChapterIndices.add(combinedDisplayItems.size)
                    val cIdx1 = chapterIndices[it]
                    val cIdx2 = chapterIndices2[it]
                    var cEndIdx1 = displayItems.size
                    var cEndIdx2 = displayItems2.size
                    if (it < totalChapterCount - 1) {
                        cEndIdx1 = chapterIndices[it + 1]
                        cEndIdx2 = chapterIndices2[it + 1]
                    }
                    mergeVersions(it + 1, combinedDisplayItems, displayItems, cIdx1, cEndIdx1,
                        displayItems2, cIdx2, cEndIdx2)
                }
                BookDisplay(bookNumber, bibleVersions, combinedDisplayItems, combinedChapterIndices)
            }
            else {
                BookDisplay(bookNumber, bibleVersions, displayItems, chapterIndices)
            }
            book
        }
    }

    private fun mergeVersions(
        chapterNumber: Int,
        combinedDisplayItems: MutableList<BookDisplayItem>,
        displayItems1: List<BookDisplayItem>,
        cIdx1: Int, cEndIdx1: Int,
        displayItems2: List<BookDisplayItem>,
        cIdx2: Int, cEndIdx2: Int
    ) {
        // skip all but verses, dividers and footnotes

        var locInfo = locateDividersForMerge(cIdx1, cEndIdx1, displayItems1)
        val dividerIdx1 = locInfo[0]
        var pt1 = locInfo[1]

        locInfo = locateDividersForMerge(cIdx2, cEndIdx2, displayItems2)
        val dividerIdx2 = locInfo[0]
        var pt2 = locInfo[1]

        // select title of first bible version to represent combination.
        combinedDisplayItems.add(displayItems1[pt1 - 1])
        if (displayMultipleSideBySide) {
            combinedDisplayItems[combinedDisplayItems.size - 1].pairedItem = displayItems2[pt2 - 1]
        }

        var vNum = 1
        while (true) {
            // copy over items with given verse number for each version

            var verseRange = getVerseRange(vNum, pt1, dividerIdx1, displayItems1)
            if (verseRange == null) {
                break
            }
            val subList1 = displayItems1.subList(verseRange[0], verseRange[1])
            pt1 = verseRange[1]

            verseRange = getVerseRange(vNum, pt2, dividerIdx2, displayItems2)
            if (verseRange == null) {
                break
            }
            val subList2 = displayItems2.subList(verseRange[0], verseRange[1])
            pt2 = verseRange[1]

            if (displayMultipleSideBySide) {
                val commonSize = min(subList1.size, subList2.size)
                (0 until commonSize).forEach {
                    val commonItem = subList1[it]
                    commonItem.pairedItem = subList2[it]
                    combinedDisplayItems.add(commonItem)
                }
                if (subList1.size > subList2.size) {
                    combinedDisplayItems.addAll(subList1.subList(commonSize, subList1.size).map {
                        it.pairedItem = BookDisplayItem(bibleVersions[1], chapterNumber, 0,
                            BookDisplayItemViewType.VERSE, vNum, "")
                        it
                    })
                }
                else if (subList2.size > subList1.size) {
                    combinedDisplayItems.addAll(subList2.subList(commonSize, subList2.size).map {
                        BookDisplayItem(bibleVersions[0], chapterNumber, 0,
                            BookDisplayItemViewType.VERSE, vNum, "", pairedItem = it)
                    })
                }
            }
            else {
                combinedDisplayItems.addAll(subList1)
                combinedDisplayItems.addAll(subList2)
            }

            vNum++
        }
        if (pt1 != dividerIdx1) {
            throw AssertionError("$pt1 != $dividerIdx1")
        }
        if (pt2 != dividerIdx2) {
            throw AssertionError("$pt2 != $dividerIdx2")
        }

        // add footnotes as one, for each bible version.
        combinedDisplayItems.addAll(displayItems1.subList(dividerIdx1, cEndIdx1))
        combinedDisplayItems.addAll(displayItems2.subList(dividerIdx2, cEndIdx2))
    }

    private fun locateDividersForMerge(
        cIdx: Int, cEndIdx: Int,
        displayItems: List<BookDisplayItem>
    ): IntArray {
        var dividerIdx = cEndIdx - 1
        if (displayItems[dividerIdx].viewType != BookDisplayItemViewType.DIVIDER) {
            throw AssertionError("${displayItems[dividerIdx].viewType} != DIVIDER")
        }
        dividerIdx--
        while (true) {
            if (displayItems[dividerIdx].viewType == BookDisplayItemViewType.DIVIDER) {
                break
            }
            dividerIdx--
        }

        // deal with verses next.

        var pt = cIdx
        if (displayItems[pt].viewType != BookDisplayItemViewType.TITLE) {
            throw AssertionError("${displayItems[pt].viewType} != TITLE")
        }

        return intArrayOf(dividerIdx, pt + 1)
    }

    private fun getVerseRange(
        vNum: Int,
        initialPt: Int,
        dividerIdx: Int,
        displayItems: List<BookDisplayItem>
    ): IntArray? {
        var pt = initialPt
        while (pt < dividerIdx) {
            val item = displayItems[pt]
            if (item.viewType == BookDisplayItemViewType.VERSE &&
                item.verseNumber == vNum) {
                break
            }
            pt++
        }
        if (pt >= dividerIdx) {
            return null
        }
        var retResult = intArrayOf(pt, pt)
        while (pt < dividerIdx) {
            val item = displayItems[pt]
            if (item.verseNumber != vNum) {
                break
            }
            pt++
        }
        retResult[1] = pt
        return retResult
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
            val bibleVersionInst = AppConstants.bibleVersions.getValue(
                bibleVersionCode)
            var titleText = bibleVersionInst.getChapterTitle(bookNumber, rawChapter.chapterNumber)
            if (bibleVersions.size > 1 && displayMultipleSideBySide) {
                titleText = "(${bibleVersionInst.abbreviation}) " + titleText
            }
            displayItems.add(
                BookDisplayItem(
                    bibleVersionCode, rawChapter.chapterNumber,
                    0, BookDisplayItemViewType.TITLE, 0, titleText))

            val footNotes = mutableListOf<BookDisplayItem>()

            for (part in rawChapter.parts) {
                when (part) {
                    is ChapterFragment -> {
                        val item = processChapterFragment(
                            bibleVersionCode, rawChapter.chapterNumber,
                            part)
                        displayItems.add(item)
                    }
                    is Verse -> {
                        val items = processVerse(
                            bibleVersionCode, rawChapter.chapterNumber,
                            part)
                        displayItems.addAll(items)
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
                    0, BookDisplayItemViewType.DIVIDER, 0, "",
                    isFirstDivider = true)
            )

            displayItems.addAll(compressFootNotes(footNotes))
            displayItems.add(
                BookDisplayItem(bibleVersionCode, rawChapter.chapterNumber,
                    0, BookDisplayItemViewType.DIVIDER, 0, ""))

            // assign index in chapter.
            displayItems.forEachIndexed { idx, item ->
                item.indexInChapter = idx
            }
        }
        return displayItems
    }

    private fun processChapterFragment(
        bibleVersionCode: String,
        chapterNumber: Int,
        rawFragment: ChapterFragment
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
        return BookDisplayItem(bibleVersionCode, chapterNumber, 0,
            viewType, 0, out.toString())
    }

    private fun processVerse(
        bibleVersionCode: String,
        chapterNumber: Int,
        rawVerse: Verse
    ): List<BookDisplayItem> {
        var prependText: String? = ""
        val selectedBibleVersion = AppConstants.bibleVersions.getValue(
            bibleVersionCode)
        if (bibleVersions.size > 1 && !displayMultipleSideBySide) {
            prependText += "<strong>(${selectedBibleVersion.abbreviation}) </strong>"
        }
        prependText += "${rawVerse.verseNumber}. "

        val verseItems = mutableListOf<BookDisplayItem>()
        val out = StringBuilder()
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
                    out.append("</font>")
                }
                is BlockQuote -> {
                    if (out.isNotEmpty()) {
                        if (prependText != null) {
                            out.insert(0, prependText)
                            prependText = null
                        }
                        val currItem = BookDisplayItem(bibleVersionCode, chapterNumber, 0,
                            BookDisplayItemViewType.VERSE, rawVerse.verseNumber, out.toString())
                        verseItems.add(currItem)
                        out.clear()
                    }
                    val nextItem = processBlockQuote(bibleVersionCode, chapterNumber,
                        part, rawVerse.verseNumber)
                    if (prependText != null) {
                        nextItem.text = prependText + nextItem.text
                        prependText = null
                    }
                    verseItems.add(nextItem)
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

        if (out.isNotEmpty()) {
            if (prependText != null) {
                out.insert(0, prependText)
            }
            val currItem = BookDisplayItem(bibleVersionCode, chapterNumber, 0,
                BookDisplayItemViewType.VERSE, rawVerse.verseNumber, out.toString())
            verseItems.add(currItem)
        }

        return verseItems
    }

    private fun processBlockQuote(
        bibleVersionCode: String,
        chapterNumber: Int,
        rawQuote: BlockQuote,
        verseNumber: Int
    ): BookDisplayItem {
        val out = StringBuilder()
        for (part in rawQuote.parts) {
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
                    out.append("</font>")
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
        return BookDisplayItem(bibleVersionCode, chapterNumber, 0,
            BookDisplayItemViewType.VERSE, verseNumber, out.toString(),
            blockQuoteKind = rawQuote.kind)
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
        if (viewType == BookDisplayItemViewType.FOOTNOTE) {
            val lowerA = 'a'.toInt()
            val charRef = (lowerA + rawNote.noteNumber - 1).toChar()
            out.append("<sup>$charRef</sup>")
        }
        for (part in rawNote.parts) {
            val escapedContent = TextUtils.htmlEncode(part.body)
            when (part.kind) {
                NoteContentKind.STRONG_EM -> {
                    out.append("<strong>$escapedContent</strong>")
                }
                NoteContentKind.EM, NoteContentKind.REF_VERSE -> {
                    out.append("<em>$escapedContent</em>")
                }
                NoteContentKind.REF_VERSE_START -> {
                    if (viewType == BookDisplayItemViewType.FOOTNOTE) {
                        out.append("<em>$escapedContent</em>")
                    }
                    else {
                        out.append("<strong>$escapedContent</strong>")
                    }
                }
                else -> {
                    out.append(escapedContent)
                }
            }
        }
        return BookDisplayItem(bibleVersionCode, chapterNumber, 0,
            viewType, 0, out.toString())
    }

    private fun compressFootNotes(footNotes: List<BookDisplayItem>): List<BookDisplayItem> {
        var buf = StringBuilder()
        var combined: BookDisplayItem? = null
        val compressed = mutableListOf<BookDisplayItem>()
        for (f in footNotes) {
            if (f.viewType == BookDisplayItemViewType.FOOTNOTE) {
                compressed.add(f)
                if (combined != null) {
                    combined.text = buf.toString()
                    combined = null
                    buf.clear()
                }
            }
            else {
                if (combined == null) {
                    compressed.add(f)
                    combined = f
                }
                buf.append(f.text).append(" ")
            }
        }
        if (combined != null) {
            combined.text = buf.toString()
        }
        return compressed
    }
}