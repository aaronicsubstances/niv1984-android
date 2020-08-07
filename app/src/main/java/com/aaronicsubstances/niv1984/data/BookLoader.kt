package com.aaronicsubstances.niv1984.data

import android.content.Context
import android.text.TextUtils
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.BookParser
import com.aaronicsubstances.niv1984.utils.BookParser.BlockQuote
import com.aaronicsubstances.niv1984.utils.BookParser.Chapter
import com.aaronicsubstances.niv1984.utils.BookParser.ChapterFragment
import com.aaronicsubstances.niv1984.utils.BookParser.ChapterFragmentKind
import com.aaronicsubstances.niv1984.utils.BookParser.FancyContent
import com.aaronicsubstances.niv1984.utils.BookParser.FancyContentKind
import com.aaronicsubstances.niv1984.utils.BookParser.Note
import com.aaronicsubstances.niv1984.utils.BookParser.NoteContentKind
import com.aaronicsubstances.niv1984.utils.BookParser.NoteKind
import com.aaronicsubstances.niv1984.utils.BookParser.NoteRef
import com.aaronicsubstances.niv1984.utils.BookParser.Verse
import com.aaronicsubstances.niv1984.utils.BookParser.WordsOfJesus
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.aaronicsubstances.niv1984.utils.AsanteTwiBibleVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookLoader(private val context: Context,
                 private val bookNumber: Int,
                 private val bibleVersions: List<String>,
                 private val bibleVersionIndexInUI: Int?,
                 private val displayMultipleSideBySide: Boolean,
                 private val isNightMode: Boolean) {

    // use flame red by default, apricot colour in night mode
    private val wjColor = AppUtils.colorResToString(R.color.wjColor, context)

    companion object {
        private val DUMMY_CONTENT = BookDisplayItemContent(-1, "")
    }

    suspend fun load(): BookDisplay {
        return withContext(Dispatchers.IO) {

            val book = if (bibleVersionIndexInUI == null) {
                val chapterIndices = mutableListOf<Int>()
                val displayItems = processChapters(0, chapterIndices)

                val chapterIndices2 = mutableListOf<Int>()
                val displayItems2 = processChapters(1, chapterIndices2)

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
                BookDisplay(
                    bookNumber,
                    bibleVersions,
                    bibleVersionIndexInUI,
                    combinedDisplayItems,
                    combinedChapterIndices,
                    displayMultipleSideBySide,
                    isNightMode)
            }
            else {
                val chapterIndices = mutableListOf<Int>()
                val displayItems = processChapters(bibleVersionIndexInUI, chapterIndices)
                BookDisplay(bookNumber, bibleVersions, bibleVersionIndexInUI, displayItems, chapterIndices,
                    displayMultipleSideBySide, isNightMode)
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
        var pt1 = locInfo[0]
        val dividerIdx1 = locInfo[1]

        locInfo = locateDividersForMerge(cIdx2, cEndIdx2, displayItems2)
        var pt2 = locInfo[0]
        val dividerIdx2 = locInfo[1]

        // set title for combination.
        if (displayMultipleSideBySide) {
            val firstPart = displayItems1[pt1 - 1]
            val secondPart = displayItems2[pt2 - 1]
            combinedDisplayItems.add(BookDisplayItem(BookDisplayItemViewType.TITLE,
                chapterNumber, 0, DUMMY_CONTENT,
                firstPartialContent = listOf(firstPart.fullContent),
                secondPartialContent = listOf(secondPart.fullContent)))
        }
        else {
            // select title of first bible version to represent combination.
            combinedDisplayItems.add(displayItems1[pt1 - 1])
        }

        var vNum = 1
        while (true) {
            // copy over items with given verse number for each version

            var verseRange1 = getVerseRange(vNum, pt1, dividerIdx1, displayItems1)
            if (verseRange1 == null) {
                break
            }

            var verseRange2 = getVerseRange(vNum, pt2, dividerIdx2, displayItems2)
            if (verseRange2 == null) {
                break
            }

            val subList1 = displayItems1.subList(verseRange1[0], verseRange1[1])
            pt1 = verseRange1[1]

            val subList2 = displayItems2.subList(verseRange2[0], verseRange2[1])
            pt2 = verseRange2[1]

            if (displayMultipleSideBySide) {
                combinedDisplayItems.add(BookDisplayItem(BookDisplayItemViewType.VERSE,
                    chapterNumber, vNum, DUMMY_CONTENT,
                    firstPartialContent = subList1.map { it.fullContent },
                    secondPartialContent = subList2.map { it.fullContent }))
            }
            else {
                combinedDisplayItems.addAll(subList1)
                combinedDisplayItems.addAll(subList2)
            }

            vNum++
        }

        if (permitAsymmetricVerseCounts(chapterNumber)) {
            while (true) {

                var verseRange1 = getVerseRange(vNum, pt1, dividerIdx1, displayItems1)
                if (verseRange1 == null) {
                    break
                }

                val subList1 = displayItems1.subList(verseRange1[0], verseRange1[1])
                pt1 = verseRange1[1]

                if (displayMultipleSideBySide) {
                    combinedDisplayItems.add(
                        BookDisplayItem(BookDisplayItemViewType.VERSE,
                            chapterNumber, vNum, DUMMY_CONTENT,
                            firstPartialContent = subList1.map { it.fullContent },
                            secondPartialContent = subList1.map { BookDisplayItemContent(1, "") })
                    )
                } else {
                    combinedDisplayItems.addAll(subList1)
                }

                vNum++
            }

            while (true) {

                var verseRange2 = getVerseRange(vNum, pt2, dividerIdx2, displayItems2)
                if (verseRange2 == null) {
                    break
                }

                val subList2 = displayItems2.subList(verseRange2[0], verseRange2[1])
                pt2 = verseRange2[1]

                if (displayMultipleSideBySide) {
                    combinedDisplayItems.add(
                        BookDisplayItem(BookDisplayItemViewType.VERSE,
                            chapterNumber, vNum, DUMMY_CONTENT,
                            firstPartialContent = subList2.map { BookDisplayItemContent(0, "") },
                            secondPartialContent = subList2.map { it.fullContent })
                    )
                } else {
                    combinedDisplayItems.addAll(subList2)
                }

                vNum++

            }
        }
        AppUtils.assert(pt1 == dividerIdx1) {
            "$chapterNumber.$vNum: $pt1 != $dividerIdx1"
        }
        AppUtils.assert(pt2 == dividerIdx2) {
            "$chapterNumber.$vNum: $pt2 != $dividerIdx2"
        }

        // add footnotes as one, for each bible version.
        combinedDisplayItems.addAll(displayItems1.subList(dividerIdx1, cEndIdx1))
        combinedDisplayItems.addAll(displayItems2.subList(dividerIdx2, cEndIdx2))
    }

    private fun permitAsymmetricVerseCounts(chapterNumber: Int): Boolean {
        if (bibleVersions.contains(AsanteTwiBibleVersion.code)) {
            // Revelation 12
            if (bookNumber == 66 && chapterNumber == 12) {
                return true
            }
            // 3 John
            if (bookNumber == 64) {
                return true
            }
        }
        return false
    }

    private fun locateDividersForMerge(
        cIdx: Int, cEndIdx: Int,
        displayItems: List<BookDisplayItem>
    ): IntArray {
        var dividerIdx = cEndIdx - 1
        AppUtils.assert(displayItems[dividerIdx].viewType == BookDisplayItemViewType.DIVIDER) {
            "${displayItems[dividerIdx].viewType} != DIVIDER"
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
        AppUtils.assert(displayItems[pt].viewType == BookDisplayItemViewType.TITLE) {
            "${displayItems[pt].viewType} != TITLE"
        }

        return intArrayOf(pt + 1, dividerIdx)
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

    private suspend fun processChapters(
        bibleVersionIndex: Int,
        chapterIndices: MutableList<Int>
    ): List<BookDisplayItem> {
        val bibleVersionCode = bibleVersions[bibleVersionIndex]

        // first read from cache
        val cacheLoader = BookCache(context, bookNumber)
        val cachedChapterIndices = mutableListOf<Pair<Int, Int>>()
        val cached = cacheLoader.load(bibleVersionCode, isNightMode, cachedChapterIndices)
        // reassign bible version indices
        cached.forEach { it.fullContent.bibleVersionIndex = bibleVersionIndex }

        val totalChapterCount = AppConstants.BIBLE_BOOK_CHAPTER_COUNT[bookNumber - 1]
        val cachedChapterNumbers = cachedChapterIndices.map { it.first }
        if (chapterIndices.size == totalChapterCount) {
            // all chapters are intact, no need to proceed further.
            // just transfer chapter indices.
            chapterIndices.addAll(cachedChapterNumbers)
            return cached
        }

        // Getting here means there are missing chapters,
        // so load book in raw XML
        val assetPath = String.format("%s/%02d.xml", bibleVersionCode, bookNumber)
        val rawChapters = context.assets.open(assetPath).use {
            val parser = BookParser()
            parser.parse(it, cachedChapterNumbers)
        }

        // load verse highlights from SQLite.
        val bookHighlighter = BookHighlighter(context, bookNumber,
            bibleVersions[bibleVersionIndex])
        bookHighlighter.load(cachedChapterNumbers)

        // Now process each chapter, inserting highlights as appropriate.
        // However if a chapter already exists in cache, copy its items over.
        val displayItems = mutableListOf<BookDisplayItem>()
        for (rawChapter in rawChapters) {
            chapterIndices.add(displayItems.size)

            // try and fetch chapter from cache if present.
            val indexIntoCachedChapterIndices = cachedChapterNumbers.indexOf(rawChapter.chapterNumber)
            if (indexIntoCachedChapterIndices != -1) {
                val indexIntoCache = cachedChapterIndices[indexIntoCachedChapterIndices].second
                val endIndexIntoCache = if (indexIntoCachedChapterIndices + 1 < cachedChapterIndices.size) {
                    cachedChapterIndices[indexIntoCachedChapterIndices + 1].second
                }
                else {
                    cached.size
                }
                (indexIntoCache until endIndexIntoCache).forEach {
                    displayItems.add(cached[it])
                }
                continue
            }

            AppUtils.assert(rawChapter.parts.isNotEmpty())
            val chapterHasHighlights = bookHighlighter.loadChapterHighlights(rawChapter.chapterNumber)

            // add title item
            val bibleVersionInst = AppConstants.bibleVersions.getValue(
                bibleVersions[bibleVersionIndex])
            var titleText = bibleVersionInst.getChapterTitle(bookNumber, rawChapter.chapterNumber)
            displayItems.add(
                BookDisplayItem(BookDisplayItemViewType.TITLE,
                    rawChapter.chapterNumber, 0,
                    BookDisplayItemContent(bibleVersionIndex, titleText)))

            val footNotes = mutableListOf<BookDisplayItem>()

            for (part in rawChapter.parts) {
                when (part) {
                    is ChapterFragment -> {
                        val item = processChapterFragment(
                            bibleVersionIndex, rawChapter.chapterNumber,
                            part)
                        displayItems.add(item)
                    }
                    is Verse -> {
                        val items = processVerse(
                            bibleVersionIndex, rawChapter.chapterNumber,
                            part, bookHighlighter, chapterHasHighlights)
                        displayItems.addAll(items)
                    }
                    else -> {
                        part as Note
                        val footNoteItem = processNote(bibleVersionIndex, rawChapter.chapterNumber,
                            part)
                        footNotes.add(footNoteItem)
                    }
                }
            }

            displayItems.add(
                BookDisplayItem(BookDisplayItemViewType.DIVIDER,
                    rawChapter.chapterNumber,0,
                    BookDisplayItemContent(bibleVersionIndex, "", isFirstDivider = true)
                ))

            displayItems.addAll(compressFootNotes(footNotes))
            displayItems.add(
                BookDisplayItem(BookDisplayItemViewType.DIVIDER,
                    rawChapter.chapterNumber,0,
                    BookDisplayItemContent(bibleVersionIndex, "")))
        }

        // lastly save read items to cache
        cacheLoader.save(bibleVersionCode, isNightMode, displayItems)

        return displayItems
    }

    private fun processChapterFragment(
        bibleVersionIndex: Int,
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
                    processNoteRef(bibleVersionIndex, chapterNumber, part, out)
                }
                else -> {
                    part as FancyContent
                    processFancyContent(part, out)
                }
            }
        }
        return BookDisplayItem(viewType, chapterNumber, 0,
            BookDisplayItemContent(bibleVersionIndex, out.toString()))
    }

    private fun processVerse(
        bibleVersionIndex: Int,
        chapterNumber: Int,
        rawVerse: Verse,
        bookHighlighter: BookHighlighter,
        chapterHasHighlights: Boolean
    ): List<BookDisplayItem> {
        var prependText: String? = "${rawVerse.verseNumber}. "
        val verseItems = mutableListOf<BookDisplayItem>()
        val out = VerseHighlighter(chapterHasHighlights)
        for (part in rawVerse.parts) {
            when (part) {
                is WordsOfJesus -> {
                    if (prependText != null) {
                        out.addInitText(prependText)
                        prependText = null
                    }
                    out.addInitMarkup("<font color='$wjColor'>")
                    for (wjPart in part.parts) {
                        when (wjPart) {
                            is NoteRef -> {
                                processNoteRef(bibleVersionIndex, chapterNumber, wjPart, out)
                            }
                            else -> {
                                wjPart as FancyContent
                                processFancyContent(wjPart, out)
                            }
                        }
                    }
                    out.addInitMarkup("</font>")
                }
                is BlockQuote -> {
                    if (!out.isEmpty()) {
                        val blockText = bookHighlighter.processBlockText(
                            rawVerse.verseNumber, verseItems.size, out)
                        val removableMarkups = bookHighlighter.getHighlightModeEditableMarkups(out)
                        val currItem = BookDisplayItem(BookDisplayItemViewType.VERSE,
                            chapterNumber, rawVerse.verseNumber,
                            BookDisplayItemContent(bibleVersionIndex, blockText,
                                highlightModeRemovableMarkups = removableMarkups),
                            isFirstVerseContent = verseItems.isEmpty())
                        verseItems.add(currItem)
                        out.clear()
                    }
                    val nextItem = processBlockQuote(bibleVersionIndex, chapterNumber,
                        part, rawVerse.verseNumber, verseItems.size, prependText, bookHighlighter,
                            chapterHasHighlights)
                    prependText = null
                    verseItems.add(nextItem)
                }
                is NoteRef -> {
                    if (prependText != null) {
                        out.addInitText(prependText)
                        prependText = null
                    }
                    processNoteRef(bibleVersionIndex, chapterNumber, part, out)
                }
                else -> {
                    if (prependText != null) {
                        out.addInitText(prependText)
                        prependText = null
                    }
                    part as FancyContent
                    processFancyContent(part, out)
                }
            }
        }

        // to deal with omitted NIV verses such as matt 17:21,
        // ensure verse items is not empty.
        if (!out.isEmpty() || verseItems.isEmpty()) {
            if (prependText != null) {
                out.addInitText(prependText)
                prependText = null
            }
            val blockText = bookHighlighter.processBlockText(rawVerse.verseNumber,
                verseItems.size, out)
            val removableMarkups = bookHighlighter.getHighlightModeEditableMarkups(out)
            val currItem = BookDisplayItem(BookDisplayItemViewType.VERSE,
                chapterNumber, rawVerse.verseNumber,
                BookDisplayItemContent(bibleVersionIndex, blockText,
                    highlightModeRemovableMarkups = removableMarkups),
                isFirstVerseContent = verseItems.isEmpty())
            verseItems.add(currItem)
        }

        return verseItems
    }

    private fun processBlockQuote(
        bibleVersionIndex: Int,
        chapterNumber: Int,
        rawQuote: BlockQuote,
        verseNumber: Int,
        verseBlockIndex: Int,
        prependText: String?,
        bookHighlighter: BookHighlighter,
        chapterHasHighlights: Boolean
    ): BookDisplayItem {
        val out = VerseHighlighter(chapterHasHighlights)
        prependText?.let { out.addInitText(it) }
        for (part in rawQuote.parts) {
            when (part) {
                is WordsOfJesus -> {
                    out.addInitMarkup("<font color='$wjColor'>")
                    for (wjPart in part.parts) {
                        when (wjPart) {
                            is NoteRef -> {
                                processNoteRef(bibleVersionIndex, chapterNumber, wjPart, out)
                            }
                            else -> {
                                wjPart as FancyContent
                                processFancyContent(wjPart, out)
                            }
                        }
                    }
                    out.addInitMarkup("</font>")
                }
                is NoteRef -> {
                    processNoteRef(bibleVersionIndex, chapterNumber, part, out)
                }
                else -> {
                    part as FancyContent
                    processFancyContent(part, out)
                }
            }
        }
        val blockText = bookHighlighter.processBlockText(verseNumber,
            verseBlockIndex, out)
        val removableMarkups = bookHighlighter.getHighlightModeEditableMarkups(out)
        return BookDisplayItem(BookDisplayItemViewType.VERSE, chapterNumber, verseNumber,
            BookDisplayItemContent(bibleVersionIndex, blockText, rawQuote.kind,
                highlightModeRemovableMarkups = removableMarkups),
            isFirstVerseContent = verseBlockIndex == 0)
    }

    private fun processFancyContent(rawContent: FancyContent, out: Any) {
        if (out is VerseHighlighter) {
            when (rawContent.kind) {
                FancyContentKind.EM, FancyContentKind.SELAH -> {
                    out.addInitMarkup("<em>")
                    out.addInitText(rawContent.content)
                    out.addInitMarkup("</em>")
                }
                FancyContentKind.STRONG_EM -> {
                    out.addInitMarkup("<strong>")
                    out.addInitText(rawContent.content)
                    out.addInitMarkup("</strong>")
                }
                FancyContentKind.PICTOGRAM -> {
                    out.addInitMarkup("<big>")
                    out.addInitText(rawContent.content)
                    out.addInitMarkup("</big>")
                }
                else -> {
                    out.addInitText(rawContent.content)
                }
            }
        }
        else {
            out as java.lang.StringBuilder
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
    }

    private fun processNoteRef(
        bibleVersionIndex: Int,
        chapterNumber: Int,
        rawNoteRef: NoteRef,
        out: Any
    ) {
        val lowerA = 'a'.toInt()
        val charRef = (lowerA + rawNoteRef.noteNumber -1).toChar()
        var text = "<sup><a href='${bibleVersions[bibleVersionIndex]}-$chapterNumber-${rawNoteRef.noteNumber}'>" +
            "$charRef</a></sup>"
        if (out is VerseHighlighter) {
            out.addInitMarkup(VerseHighlighter.Markup(text, removeDuringHighlighting = true))
        }
        else {
            out as java.lang.StringBuilder
            out.append(text)
        }
    }

    private fun processNote(
        bibleVersionIndex: Int,
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
        return BookDisplayItem(viewType, chapterNumber, 0,
            BookDisplayItemContent(bibleVersionIndex, out.toString()))
    }

    private fun compressFootNotes(footNotes: List<BookDisplayItem>): List<BookDisplayItem> {
        var buf = StringBuilder()
        var combined: BookDisplayItem? = null
        val compressed = mutableListOf<BookDisplayItem>()
        for (f in footNotes) {
            if (f.viewType == BookDisplayItemViewType.FOOTNOTE) {
                compressed.add(f)
                if (combined != null) {
                    combined.fullContent.text = buf.toString()
                    combined = null
                    buf.clear()
                }
            }
            else {
                if (combined == null) {
                    compressed.add(f)
                    combined = f
                }
                buf.append(f.fullContent.text).append(" ")
            }
        }
        if (combined != null) {
            combined.fullContent.text = buf.toString()
        }
        return compressed
    }
}