package com.aaronicsubstances.niv1984.repositories

import android.content.Context
import android.text.TextUtils
import com.aaronicsubstances.endlesspaginglib.*
import com.aaronicsubstances.niv1984.models.BookReadItem
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class BookReadDataSource(private val context: Context,
                         private val coroutineScope: CoroutineScope,
                         private val bookNumber: Int,
                         val bibleVersions: List<String>): EndlessListDataSource<BookReadItem> {

    private var footNoteIndex = 0
    private val wjColor = "red"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookReadDataSource

        if (bibleVersions != other.bibleVersions) return false

        return true
    }

    override fun hashCode(): Int {
        return bibleVersions.hashCode()
    }

    override fun fetchInitialDataAsync(
        asyncResultId: Any?,
        config: EndlessListRepositoryConfig,
        initialKey: Any?,
        dsCallback: EndlessListDataSource.Callback<BookReadItem>
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val readItemKey = initialKey as BookReadItem.Key?
            val readItems = if (readItemKey == null) {
                loadReadItemsAfter(null, config.initialLoadSize)
            }
            else {
                loadReadItemsAfter(readItemKey, config.initialLoadSize)
            }
            val fetchResult = EndlessListLoadResult(readItems)
            dsCallback.postDataLoadResult(fetchResult)
        }
    }

    override fun fetchDataAsync(
        asyncResultId: Any?,
        config: EndlessListRepositoryConfig,
        exclusiveBoundaryKey: Any?,
        useInAfterPosition: Boolean,
        dsCallback: EndlessListDataSource.Callback<BookReadItem>
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val readItemKey = exclusiveBoundaryKey as BookReadItem.Key?
            val readItems = if (readItemKey == null) {
                if (useInAfterPosition) {
                    loadReadItemsAfter(null, config.loadSize)
                }
                else {
                    // loadBefore requires a key so return empty if
                    // key is not provided.
                    listOf()
                }
            }
            else {
                if (useInAfterPosition) {
                    loadReadItemsAfter(readItemKey, config.loadSize)
                }
                else {
                    loadReadItemsBefore(readItemKey, config.loadSize)
                }
            }
            val fetchResult = EndlessListLoadResult(readItems)
            dsCallback.postDataLoadResult(fetchResult)
        }
    }

    private fun loadReadItemsAfter(exclusiveKey: BookReadItem.Key?, loadSize: Int): List<BookReadItem> {
        val totalChapterCount = AppConstants.BIBLE_BOOK_CHAPTER_COUNT[bookNumber-1]
        val totalReadItems = LinkedList<BookReadItem>()
        var chapterNumber = exclusiveKey?.chapterNumber ?: 1
        // don't take loadSize too seriously, and treat it as a minimum requirement only.
        while (chapterNumber <= totalChapterCount && totalReadItems.size < loadSize) {
            val chapterReadItems = loadChapter(chapterNumber)
            if (exclusiveKey != null && chapterNumber == exclusiveKey.chapterNumber) {
                // start adding items after content index of exclusive key.
                var startIndex = 0
                while (startIndex < chapterReadItems.size) {
                    if (chapterReadItems[startIndex].key.contentIndex == exclusiveKey.contentIndex) {
                        break
                    }
                    startIndex++
                }
                assert (startIndex <= chapterReadItems.size) {
                    "$exclusiveKey not found"
                }
                totalReadItems.addAll(chapterReadItems.subList(startIndex + 1,
                    chapterReadItems.size))
            }
            else {
                totalReadItems.addAll(chapterReadItems)
            }

            chapterNumber++
        }
        return totalReadItems
    }

    private fun loadReadItemsBefore(exclusiveKey: BookReadItem.Key, loadSize: Int): List<BookReadItem> {
        val totalReadItems = LinkedList<BookReadItem>()
        var chapterNumber = exclusiveKey.chapterNumber
        // don't take loadSize too seriously, and treat it as a minimum requirement only.
        while (chapterNumber > 0 && totalReadItems.size < loadSize) {
            val chapterReadItems = loadChapter(chapterNumber)
            if (chapterNumber == exclusiveKey.chapterNumber) {
                // start adding items before content index of exclusive key.
                var endIndex = chapterReadItems.size - 1
                while (endIndex >= 0) {
                    if (chapterReadItems[endIndex].key.contentIndex == exclusiveKey.contentIndex) {
                        break
                    }
                    endIndex--
                }
                assert (endIndex >= 0) {
                    "$exclusiveKey not found"
                }
                totalReadItems.addAll(0, chapterReadItems.subList(0, endIndex))
            }
            else {
                totalReadItems.addAll(0, chapterReadItems)
            }

            chapterNumber--
        }
        return totalReadItems
    }

    private fun loadChapter(chapterNumber: Int): List<BookReadItem> {
        // reset
        footNoteIndex = 0

        val readItems = if (bibleVersions.size > 1) {
            loadMultipleBibleVersions(chapterNumber)
        }
        else {
            loadSingleBibleVersion(chapterNumber)
        }

        // set content index
        readItems.indices.forEach {
            readItems[it].key = BookReadItem.Key(chapterNumber, it, bibleVersions[0])
        }
        return readItems
    }

    private fun loadSingleBibleVersion(chapterNumber: Int): List<BookReadItem> {
        val readItems = mutableListOf<BookReadItem>()
        loadPartBibleVersion(chapterNumber, 0, readItems)
        return readItems
    }

    private fun loadMultipleBibleVersions(chapterNumber: Int): List<BookReadItem> {
        val readItems1 = mutableListOf<BookReadItem>()
        val readItems2 = mutableListOf<BookReadItem>()
        val footNoteCount1 = loadPartBibleVersion(chapterNumber, 0,
            readItems1)
        val footNoteCount2 = loadPartBibleVersion(chapterNumber, 1,
            readItems2)

        // merge readItems.
        val readItems = mutableListOf<BookReadItem>()

        var idx1 = 0
        var idx2 = 0;

        // exempt footnotes at first.
        while (idx1 < readItems1.size - footNoteCount1 &&
            idx2 < readItems2.size - footNoteCount2) {
            val item1 = readItems1[idx1]
            val item2 = readItems2[idx2]
            // only write out an item from second list if verse number is
            // greater.
            if (item2.verseNumber > item1.verseNumber) {
                readItems.add(item2)
                idx2++
            }
            else {
                readItems.add(item1)
                idx1++
            }
        }
        while (idx1 < readItems1.size - footNoteCount1) {
            readItems.add(readItems1[idx1++])
        }
        while (idx2 < readItems2.size - footNoteCount2) {
            readItems.add(readItems2[idx2++])
        }

        // now add footnotes
        while (idx1 < readItems1.size) {
            readItems.add(readItems1[idx1++])
        }
        while (idx2 < readItems2.size) {
            readItems.add(readItems2[idx2++])
        }

        return readItems
    }

    private fun loadPartBibleVersion(chapterNumber: Int, bibleVersionIndex: Int,
                                     readItems: MutableList<BookReadItem>): Int {
        if (bibleVersionIndex == 0) {
            readItems.add(createTitle(chapterNumber))
            readItems.add(BookReadItem(
                BookReadItem.Key(chapterNumber, 0,
                    bibleVersions[0]), BookReadItem.ViewType.VERSE, 1, "Test"))
            return 0
        }
        val chapterContents = loadChapterFromAssets(chapterNumber, bibleVersions[bibleVersionIndex])
        val footNoteCount = processChapterContents(chapterNumber,
            chapterContents, bibleVersionIndex, readItems)
        return footNoteCount
    }

    private fun createTitle(chapterNumber: Int): BookReadItem {
        val firstBibleVersionCode = bibleVersions[0]
        val titleText = AppConstants.bibleVersions.getValue(
            firstBibleVersionCode).getChapterTitle(chapterNumber)
        return BookReadItem(BookReadItem.Key(chapterNumber, 0,
            firstBibleVersionCode), BookReadItem.ViewType.TITLE, 0, titleText)
    }

    private fun createDivider(chapterNumber: Int, bibleVersionIndex: Int): BookReadItem {
        return BookReadItem(BookReadItem.Key(chapterNumber, 0,
            bibleVersions[bibleVersionIndex]), BookReadItem.ViewType.DIVIDER, 0, "")
    }

    private fun loadChapterFromAssets(chapterNumber: Int, bibleVersion: String): List<Any> {
        val assetPath = String.format(
            "%s/%02d/%03d.xml", bibleVersion,
            bookNumber, chapterNumber
        )
        var chapterContents = mutableListOf<Any>()
        context.assets.open(assetPath).use {
            BookChapterParser().parse(it, chapterContents)
        }
        return chapterContents
    }

    private fun processChapterContents(
        chapterNumber: Int,
        parseResults: List<Any>,
        bibleVersionIndex: Int,
        readItems: MutableList<BookReadItem>
    ): Int {
        val footNotes = mutableListOf<BookReadItem>()
        for (part in parseResults) {
            when (part) {
                is BookChapterParser.ChapterFragment -> {
                    processChapterFragment(
                        chapterNumber,
                        part,
                        footNotes,
                        bibleVersionIndex,
                        readItems
                    )
                }
                is BookChapterParser.Verse -> {
                    processVerse(chapterNumber, part, footNotes, bibleVersionIndex, readItems)
                }
            }
        }

        // add dividers to surround footnotes.
        if (bibleVersionIndex == 0) {
            footNotes.add(0, createDivider(chapterNumber, bibleVersionIndex))
        }
        footNotes.add(createDivider(chapterNumber, bibleVersionIndex))
        readItems.addAll(footNotes)
        return footNotes.size
    }

    private fun processChapterFragment(
        chapterNumber: Int,
        parseResult: BookChapterParser.ChapterFragment,
        footNotes: MutableList<BookReadItem>,
        bibleVersionIndex: Int,
        readItems: MutableList<BookReadItem>
    ) {
        val out = StringBuilder()
        for (part in parseResult.parts) {
            if (bibleVersions.size > 1) {
                // skip chapter fragments in main text and only process notes when
                // multiple bible versions are being shown.
                if (part is BookChapterParser.Note) {
                    processNote(chapterNumber, 0, part, footNotes,
                        bibleVersionIndex, null)
                }
            }
            else {
                processFancyContentOrNote(chapterNumber, 0, part, footNotes,
                    bibleVersionIndex, out)
            }
        }

        val viewType = when (parseResult.kind) {
            BookChapterParser.ChapterFragmentKind.HEADING -> {
                BookReadItem.ViewType.HEADER
            }
            else -> {
                BookReadItem.ViewType.CHAPTER_FRAGMENT
            }
        }
        val html = AppUtils.parseHtml(out.toString())
        val chapterFragment = BookReadItem(BookReadItem.Key(chapterNumber, 0,
            bibleVersions[bibleVersionIndex]), viewType, 0, html)
        readItems.add(chapterFragment)
    }

    private fun processVerse(
        chapterNumber: Int,
        parseResult: BookChapterParser.Verse,
        footNotes: MutableList<BookReadItem>,
        bibleVersionIndex: Int,
        readItems: MutableList<BookReadItem>
    ) {
        val selectedBibleVersion = AppConstants.bibleVersions.getValue(
            bibleVersions[bibleVersionIndex])
        val out = StringBuilder()

        if (bibleVersions.size > 1) {
            out.append("<strong>(${selectedBibleVersion.abbreviation}) </strong>")
        }
        out.append("${parseResult.verseNumber}. ")
        for (part in parseResult.parts) {
            when (part) {
                is BookChapterParser.WordsOfJesus -> {
                    out.append("<font color='$wjColor'>")
                    for (wjPart in part.parts) {
                        processFancyContentOrNote(chapterNumber, parseResult.verseNumber,
                            wjPart, footNotes, bibleVersionIndex, out)
                    }
                    out.append("<font>")
                }
                else -> {
                    processFancyContentOrNote(chapterNumber, parseResult.verseNumber,
                        part, footNotes, bibleVersionIndex, out)
                }
            }
        }
        val html = AppUtils.parseHtml(out.toString())
        val verseItem = BookReadItem(BookReadItem.Key(chapterNumber, 0,
            selectedBibleVersion.code), BookReadItem.ViewType.VERSE, parseResult.verseNumber,
            html)
        readItems.add(verseItem)
    }

    private fun processFancyContentOrNote(
        chapterNumber: Int,
        verseNumber: Int,
        parseResult: Any,
        footNotes: MutableList<BookReadItem>,
        bibleVersionIndex: Int,
        out: StringBuilder
    ) {
        when (parseResult) {
            is BookChapterParser.FancyContent -> {
                val escapedContent = TextUtils.htmlEncode(parseResult.content)
                when (parseResult.kind) {
                    BookChapterParser.FancyContentKind.EM, BookChapterParser.FancyContentKind.SELAH -> {
                        out.append("<em>$escapedContent</em>")
                    }
                    BookChapterParser.FancyContentKind.STRONG_EM -> {
                        out.append("<strong>$escapedContent</strong>")
                    }
                    else -> {
                        out.append(escapedContent)
                    }
                }
            }
            is BookChapterParser.Note -> {
                processNote(chapterNumber, verseNumber, parseResult, footNotes,
                    bibleVersionIndex, out)
            }
        }
    }

    private fun processNote(
        chapterNumber: Int,
        verseNumber: Int,
        parseResult: BookChapterParser.Note,
        footNotes: MutableList<BookReadItem>,
        bibleVersionIndex: Int,
        mainText: StringBuilder?
    ) {
        val noteText = StringBuilder()
        var viewType = BookReadItem.ViewType.CROSS_REFERENCES
        if (parseResult.kind == BookChapterParser.NoteKind.DEFAULT) {
            var noteIdentifier = ('a'.toInt() + footNoteIndex++).toChar().toString()
            noteIdentifier = "<sup><b>[<a href='#$noteIdentifier'>$noteIdentifier</a>]</b></sup>"
            noteText.append(noteIdentifier)
            mainText?.append(noteIdentifier)
            viewType = BookReadItem.ViewType.FOOTNOTE
        }
        for (part in parseResult.parts) {
            val escapedContent = TextUtils.htmlEncode(part.body)
            when (part.kind) {
                BookChapterParser.NoteContentKind.EM, BookChapterParser.NoteContentKind.REF_VERSE_START -> {
                    noteText.append("<em>$escapedContent</em>")
                }
                BookChapterParser.NoteContentKind.STRONG_EM -> {
                    noteText.append("<strong>$escapedContent</strong>")
                }
                BookChapterParser.NoteContentKind.REF_VERSE -> {
                    noteText.append("<em><a link='#$escapedContent'>$escapedContent</a></em>")
                }
                else -> {
                    noteText.append(escapedContent)
                }
            }
        }

        val html = AppUtils.parseHtml(noteText.toString())
        val footNote = BookReadItem(
            BookReadItem.Key(chapterNumber, 0,
                bibleVersions[bibleVersionIndex]), viewType, verseNumber, html)
        footNotes.add(footNote)
    }
}