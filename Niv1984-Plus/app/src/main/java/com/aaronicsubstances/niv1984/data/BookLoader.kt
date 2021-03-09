package com.aaronicsubstances.niv1984.data

import android.content.Context
import android.text.TextUtils
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.*
import com.aaronicsubstances.niv1984.utils.BookParser.BlockQuote
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
        val DUMMY_CONTENT = BookDisplayItemContent(-1, "")

        fun createNoteRefHtml(
                chapterNumber: Int,
                noteRefNumber: Int,
                createLink: Boolean
        ): Pair<String, String> {
            val lowerA = 'a'.toInt()
            val charRef = (lowerA + noteRefNumber - 1).toChar()
            var supEl = charRef.toString()
            val link = "ft-$chapterNumber-$noteRefNumber"
            if (createLink) {
                supEl = "[<a href='$link'> $charRef </a>]"
            }
            supEl = "<sup>$supEl</sup>"
            return Pair(supEl, link)
        }
    }

    suspend fun load(): BookDisplay {
        return withContext(Dispatchers.IO) {

            val book = if (bibleVersionIndexInUI == null) {
                val merger = MultipleVersionsMerger(bookNumber, bibleVersions,
                    displayMultipleSideBySide)
                val totalChapterCount = AppConstants.BIBLE_BOOK_CHAPTER_COUNT[bookNumber - 1]
                val combinedDisplayItems = mutableListOf<BookDisplayItem>()
                val combinedChapterIndices = mutableListOf<Int>()
                if (displayMultipleSideBySide) {
                    val chapterIndices = mutableListOf<Int>()
                    val displayItems = loadBibleVersionBook(0, chapterIndices)

                    val chapterIndices2 = mutableListOf<Int>()
                    val displayItems2 = loadBibleVersionBook(1, chapterIndices2)

                    (0 until totalChapterCount).forEach {
                        combinedChapterIndices.add(combinedDisplayItems.size)
                        val cIdx1 = chapterIndices[it]
                        val cIdx2 = chapterIndices2[it]
                        var cEndIdx1 = displayItems.size
                        var cEndIdx2 = displayItems2.size
                        if (it < totalChapterCount - 1) {
                            cEndIdx1 = chapterIndices[it + 1]
                            cEndIdx2 = chapterIndices2[it + 1]
                        }
                        merger.mergeVersionsInTwoColumns(it + 1, combinedDisplayItems, displayItems, cIdx1, cEndIdx1,
                            displayItems2, cIdx2, cEndIdx2)
                    }
                }
                else {
                    val allChapterIndices = mutableListOf<MutableList<Int>>()
                    val allDisplayItems = mutableListOf<List<BookDisplayItem>>()
                    bibleVersions.indices.forEach {
                        val chapterIndices = mutableListOf<Int>()
                        val displayItems = loadBibleVersionBook(it, chapterIndices)
                        allChapterIndices.add(chapterIndices)
                        allDisplayItems.add(displayItems)
                    }
                    (0 until totalChapterCount).forEach { cnMinusOne ->
                        combinedChapterIndices.add(combinedDisplayItems.size)
                        val allBeginChapterIndices = mutableListOf<Int>();
                        val allEndChapterIndices = mutableListOf<Int>();
                        allDisplayItems.indices.forEach {
                            val chapterIndices = allChapterIndices[it]
                            val cIdx = chapterIndices[cnMinusOne]
                            var cEndIdx = allDisplayItems[it].size
                            if (cnMinusOne < totalChapterCount - 1) {
                                cEndIdx = chapterIndices[cnMinusOne + 1]
                            }
                            allBeginChapterIndices.add(cIdx)
                            allEndChapterIndices.add(cEndIdx)
                        }
                        merger.mergeVersionsInSingleColumn(cnMinusOne + 1, combinedDisplayItems,
                            allDisplayItems, allBeginChapterIndices, allEndChapterIndices)
                    }
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
                val displayItems = loadBibleVersionBook(bibleVersionIndexInUI, chapterIndices)
                BookDisplay(bookNumber, bibleVersions, bibleVersionIndexInUI, displayItems, chapterIndices,
                    displayMultipleSideBySide, isNightMode)
            }
            book
        }
    }

    private suspend fun loadBibleVersionBook(bibleVersionIndex: Int,
                                             chapterIndices: MutableList<Int>): List<BookDisplayItem> {
        val bookContents = processChapters(bibleVersionIndex, chapterIndices)
        // reassign bible version indices
        bookContents.forEach { it.fullContent.bibleVersionIndex = bibleVersionIndex }
        return bookContents
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
                            rawChapter.chapterNumber,
                            part)
                        displayItems.add(item)
                    }
                    is Verse -> {
                        val items = processVerse(
                            rawChapter.chapterNumber,
                            part, bookHighlighter, chapterHasHighlights)
                        displayItems.addAll(items)
                    }
                    else -> {
                        part as Note
                        val footNoteItem = processNote(rawChapter.chapterNumber, part)
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
                    processNoteRef(chapterNumber, part, out)
                }
                else -> {
                    part as FancyContent
                    processFancyContent(part, out)
                }
            }
        }
        return BookDisplayItem(viewType, chapterNumber, 0,
            BookDisplayItemContent(0, out.toString()))
    }

    private fun processVerse(
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
                                processNoteRef(chapterNumber, wjPart, out)
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
                            BookDisplayItemContent(0, blockText,
                                highlightModeRemovableMarkups = removableMarkups),
                            isFirstVerseContent = verseItems.isEmpty())
                        verseItems.add(currItem)
                        out.clear()
                    }
                    val nextItem = processBlockQuote(chapterNumber,
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
                    processNoteRef(chapterNumber, part, out)
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
                BookDisplayItemContent(0, blockText,
                    highlightModeRemovableMarkups = removableMarkups),
                isFirstVerseContent = verseItems.isEmpty())
            verseItems.add(currItem)
        }

        return verseItems
    }

    private fun processBlockQuote(
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
                                processNoteRef(chapterNumber, wjPart, out)
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
                    processNoteRef(chapterNumber, part, out)
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
            BookDisplayItemContent(0, blockText, rawQuote.kind,
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
        chapterNumber: Int,
        rawNoteRef: NoteRef,
        out: Any
    ) {
        var text = createNoteRefHtml(chapterNumber, rawNoteRef.noteNumber, true).first
        if (out is VerseHighlighter) {
            out.addInitMarkup(VerseHighlighter.Markup(text, removeDuringHighlighting = true))
        }
        else {
            out as java.lang.StringBuilder
            out.append(text)
        }
    }

    private fun processNote(
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
        var footNoteId: String? = null
        if (viewType == BookDisplayItemViewType.FOOTNOTE) {
            val noteRefHtml = createNoteRefHtml(chapterNumber, rawNote.noteNumber, false)
            out.append(noteRefHtml.first)
            footNoteId = noteRefHtml.second
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
            BookDisplayItemContent(0, out.toString(),
            footNoteId = footNoteId))
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