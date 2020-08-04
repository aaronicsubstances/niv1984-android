package com.aaronicsubstances.niv1984.ui.book_reading

import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.data.SourceCodeTransformer
import com.aaronicsubstances.niv1984.models.*
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.view_adapters.BookLoadAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class HighlightModeHelper(private val fragment: BookLoadFragment,
                          savedInstanceState: Bundle?) {
    private val TAG = javaClass.name

    companion object {
        private const val STATE_KEY_IN_HIGHLIGHT_MODE = "HighlightModeHelper.inHighlightMode"
    }

    private val defaultView: View
    private val chapterFocusView: View
    private val selectedChapterTitle: TextView
    private val selectedChapterContent: HtmlTextView
    private val selectedChapterContentScroller: HtmlScrollView
    private val bookContentAdapter: BookLoadAdapter

    private val htmlViewManager: HtmlViewManager

    var inHighlightMode: Boolean

    init {
        fragment.requireView().let {
            defaultView = it.findViewById(R.id.bookReadView)
            chapterFocusView = it.findViewById(R.id.selectedChapterScroller)
            selectedChapterTitle = it.findViewById(R.id.selectedChapterTitle)
            selectedChapterContent = it.findViewById(R.id.selectedChapterText)
            selectedChapterContentScroller = it.findViewById(R.id.selectedChapterScroller)
        }

        // use INVISIBLE instead of GONE so that HtmlScrollView height is determined correctly before
        // highlight requests
        chapterFocusView.visibility = View.INVISIBLE

        bookContentAdapter = fragment.bookContentAdapter
        htmlViewManager = HtmlViewManager(fragment.requireContext())

        selectedChapterContentScroller.viewDataChangeListener = htmlViewManager
        selectedChapterContent.viewDataChangeListener = htmlViewManager

        selectedChapterContent.customSelectionActionModeCallback = object: ActionMode.Callback {
            private var mode: ActionMode? = null
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                this.mode = mode
                mode.menuInflater.inflate(R.menu.fragment_book_load_floating, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.action_add_highlight -> {
                        addHighlightRange()
                        finishActionMode()
                        return true
                    }
                    R.id.action_remove_highlight -> {
                        removeHighlightRange()
                        finishActionMode()
                        return true
                    }
                }
                // else let default items be processed.
                // currently this means that completing a default action such as Copy
                // does not automatically exit highlight mode. That is fine for now.
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                this.mode = null
            }

            fun finishActionMode() {
                mode?.finish()
                exitHighlightMode()
            }
        }

        // enter copy mode immediately if restored state indicates so
        inHighlightMode = savedInstanceState?.getBoolean(STATE_KEY_IN_HIGHLIGHT_MODE, false) ?: false
        if (inHighlightMode) {
            enterHighlightMode()
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_IN_HIGHLIGHT_MODE, inHighlightMode)
    }

    fun handleBackPress(): Boolean {
        return exitHighlightMode()
    }

    fun onPrefZoomLevelChanged() {
        if (inHighlightMode) {
            updateTextSizes()
        }
    }

    fun onChapterChanged() {
        if (inHighlightMode) {
            Selection.removeSelection(selectedChapterContent.text as Spannable)
            enterHighlightMode()
        }
    }

    fun enterHighlightMode() {
        val currentLoadResult = fragment.viewModel.getValidLastLoadResult(
            bookContentAdapter.bibleVersions, bookContentAdapter.bibleVersionIndexInUI,
            bookContentAdapter.displayMultipleSideBySide, bookContentAdapter.isNightMode)
        if (currentLoadResult == null) {
            AppUtils.showShortToast(fragment.context, fragment.getString(
                R.string.message_highlight_mode_prohibited))
            return
        }

        val latestSysBookmark = fragment.viewModel.currLoc
        switchToChapterFocusView(currentLoadResult, latestSysBookmark)

        defaultView.visibility = View.INVISIBLE
        chapterFocusView.visibility = View.VISIBLE
        updateTextSizes()

        inHighlightMode = true
        (fragment.activity as MainActivity).invalidateOptionsMenu()
        fragment.resetOverlayPanel()
    }

    fun exitHighlightMode(): Boolean {
        if (!inHighlightMode) {
            return false
        }
        defaultView.visibility = View.VISIBLE
        chapterFocusView.visibility = View.INVISIBLE
        selectedChapterContentScroller.setOnScrollChangeListener(null)
        selectedChapterContent.text = "" // should clear any selection
        inHighlightMode = false
        (fragment.activity as MainActivity).invalidateOptionsMenu()
        fragment.resetOverlayPanel()
        return true
    }

    private fun updateTextSizes() {
        val dummyTitleItem = BookDisplayItem(BookDisplayItemViewType.TITLE, 0,
            0, BookDisplayItemContent(0, "")
        )
        val dummyVerseItem = BookDisplayItem(BookDisplayItemViewType.VERSE, 0,
            0, BookDisplayItemContent(0, ""))
        bookContentAdapter.initDefault(dummyTitleItem, selectedChapterTitle)
        bookContentAdapter.initDefault(dummyVerseItem, selectedChapterContent)
    }

    private fun switchToChapterFocusView(currentLoadResult: BookDisplay, sysBookmark: ScrollPosPref) {
        val bibleVersionIndex = sysBookmark.particularBibleVersionIndex ?: 0
        val bibleVersionCode = fragment.bookContentAdapter.bibleVersions[bibleVersionIndex]
        val bibleVersion = AppConstants.bibleVersions.getValue(bibleVersionCode)
        val chapterTitle = bibleVersion.getChapterTitle(fragment.bookNumber, sysBookmark.chapterNumber)
        selectedChapterTitle.text = chapterTitle

        selectedChapterContent.text = fetchChapterContent(sysBookmark.chapterNumber,
            bibleVersionIndex)

        // delay goToVerse so htmlTextView gets the chance to redraw
        val initialVerseNumber = if (sysBookmark.verseNumber > 0) sysBookmark.verseNumber
        else {
            if (sysBookmark.equivalentViewItemType == BookDisplayItemViewType.TITLE) 0
            else htmlViewManager.lastVerseNumberSeen
        }
        selectedChapterContentScroller.post {
            htmlViewManager.goToVerse(
                initialVerseNumber, selectedChapterContentScroller, selectedChapterContent
            )

            selectedChapterContentScroller.setOnScrollChangeListener(object :
                View.OnScrollChangeListener {
                override fun onScrollChange(
                    v: View?,
                    scrollX: Int,
                    scrollY: Int,
                    oldScrollX: Int,
                    oldScrollY: Int
                ) {
                    var vNum = htmlViewManager.getVerseNumber(selectedChapterContent, scrollY)
                    //android.util.Log.d(TAG, "Scroll pos $scrollY points to verse $vNum")

                    var equivalentViewType = BookDisplayItemViewType.VERSE
                    if (vNum < 1) {
                        vNum = 0
                        equivalentViewType = BookDisplayItemViewType.TITLE
                    }
                    // debounce
                    if (vNum != fragment.viewModel.currLocVerseNumber) {
                        fragment.viewModel.updateSystemBookmarks(
                            sysBookmark.chapterNumber,
                            vNum, equivalentViewType, -1
                        )
                        val particularPos =
                            fragment.viewModel.updateParticularPos(currentLoadResult)
                        fragment.scrollBook(particularPos)
                    }
                }
            })
        }
    }

    private fun fetchChapterContent(chapterNumber: Int, bibleVersionIndex: Int): Spanned {
        val chapterIndex = fragment.viewModel.lastLoadResult!!.chapterIndices[chapterNumber - 1]
        val titleItem = bookContentAdapter.currentList[chapterIndex]
        assert(titleItem.chapterNumber == chapterNumber)
        assert(titleItem.viewType == BookDisplayItemViewType.TITLE)
        var i = chapterIndex + 1
        val chapterContent = StringBuilder()
        var lastVerseNum = 0
        var lastVerseBlockIndex = 0
        while (i < bookContentAdapter.currentList.size) {
            val item = bookContentAdapter.currentList[i]
            if (item.chapterNumber != chapterNumber) {
                break
            }
            var contentByParts: List<BookDisplayItemContent>? = null
            var fullContent: BookDisplayItemContent? = null
            if (item.viewType == BookDisplayItemViewType.VERSE) {
                if (bookContentAdapter.multipleDisplay && bookContentAdapter.displayMultipleSideBySide) {
                    contentByParts = if (bibleVersionIndex == 0) {
                        item.firstPartialContent
                    } else {
                        item.secondPartialContent
                    }
                }
                else if (item.fullContent.bibleVersionIndex == bibleVersionIndex) {
                    fullContent = item.fullContent
                }
            }
            if (contentByParts != null || fullContent != null) {
                val vTag = "${htmlViewManager.vPrefix}$lastVerseNum"
                if (lastVerseNum != item.verseNumber) {
                    if (lastVerseNum > 0) {
                        chapterContent.append("<br></$vTag>")
                    }
                    chapterContent.append("<$vTag>")
                    lastVerseNum = item.verseNumber
                    lastVerseBlockIndex = 0
                }
                contentByParts?.let {
                    for (partIdx in it.indices) {
                        val part = it[partIdx]
                        val processed = processTextForHighlightMode(part)
                        val bTag = "${htmlViewManager.bPrefix}$partIdx"
                        chapterContent.append("<$bTag>")
                        chapterContent.append(processed)
                        chapterContent.append("</$bTag>")
                        chapterContent.append("<br>")
                    }
                }
                fullContent?.let {
                    val processed = processTextForHighlightMode(it)
                    val bTag = "${htmlViewManager.bPrefix}$lastVerseBlockIndex"
                    lastVerseBlockIndex++
                    chapterContent.append("<$bTag>")
                    chapterContent.append(processed)
                    chapterContent.append("</$bTag>")
                    chapterContent.append("<br>")
                }
            }
            i++
        }
        assert(lastVerseNum > 0)
        chapterContent.append("</${htmlViewManager.vPrefix}$lastVerseNum>")

        htmlViewManager.reset()
        //wrap in body to prevent tag mechanism from treating first verse tag as a sort of wrapper
        val spanned = AppUtils.parseHtml("<body>$chapterContent</body>", htmlViewManager)
        return spanned
    }

    private fun processTextForHighlightMode(item: BookDisplayItemContent): String {
        val removableMarkups = item.highlightModeRemovableMarkups
        if (removableMarkups == null || removableMarkups.isEmpty()) {
            return item.text
        }
        val transformer = SourceCodeTransformer(item.text)
        for (m in removableMarkups) {
            transformer.addTransform("", m.pos, m.pos + m.tag.length)
        }
        return transformer.transformedText.toString()
    }

    fun addHighlightRange() {
        val selStart = selectedChapterContent.selectionStart
        val selEnd = selectedChapterContent.selectionEnd
        val vStart = htmlViewManager.getVerseNumber(selStart)
        val vEnd = htmlViewManager.getVerseNumber(selEnd)
        AppUtils.showShortToast(fragment.context, "selection ($selStart, $selEnd) " +
                "maps to verses $vStart-$vEnd")
    }

    fun determineRanges(): List<IntArray> {
        val selStart = selectedChapterContent.selectionStart
        val selEnd = selectedChapterContent.selectionEnd
        val ranges = mutableListOf<IntArray>()
        for (blockEntry in htmlViewManager.verseBlockPosMap) {
            val blockStart = blockEntry[2]
            val blockEnd = blockEntry[3]

            // explore 4 possibilities: totally outside, totally inside, overlaps on the left,
            // or overlaps on the right.
            if (selStart > blockEnd || selEnd < blockStart) {
                // totally outside
            }
            else if (selStart >= blockStart && selEnd <= blockEnd) {
                // totally inside.
                ranges.add(blockEntry)
            }
            else {
                if (selStart < blockStart) {
                    // overlaps on the left
                    ranges.add(intArrayOf(blockEntry[0], blockEntry[1], blockStart, selEnd))
                }
                if (selEnd > blockEnd) {
                    // overlaps on the right
                    ranges.add(intArrayOf(blockEntry[0], blockEntry[1], selStart, blockEnd))
                }
            }
        }
        return ranges
    }

    fun removeHighlightRange() {

    }
}