package com.aaronicsubstances.niv1984.ui.book_reading

import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.view.*
import android.widget.TextView
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.data.BookHighlighter
import com.aaronicsubstances.niv1984.data.SourceCodeTransformer
import com.aaronicsubstances.niv1984.data.VerseHighlighter
import com.aaronicsubstances.niv1984.models.*
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.view_adapters.BookLoadAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class HighlightModeHelper(private val fragment: BookLoadFragment,
                          savedInstanceState: Bundle?): ViewTreeObserver.OnScrollChangedListener {

    private val TAG = javaClass.name

    companion object {
        private const val STATE_KEY_IN_HIGHLIGHT_MODE = "HighlightModeHelper.inHighlightMode"
        private const val STATE_KEY_SPECIFIC_BIBLE_VERSION_INDEX = "HighlightModeHelper.specificBibleVersionIndex"
    }

    private val defaultView: View
    private val chapterFocusView: View
    private val selectedChapterTitle: TextView
    private val selectedChapterContent: HtmlTextView
    private val selectedChapterContentScroller: HtmlScrollView
    private val bookContentAdapter: BookLoadAdapter

    private val htmlViewManager: HtmlViewManager

    var inHighlightMode: Boolean = false
    var specificBibleVersionIndex: Int = 0
        private set

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
        if (savedInstanceState?.getBoolean(STATE_KEY_IN_HIGHLIGHT_MODE, false) == true) {
            val savedBibleVersionIndex = savedInstanceState.getInt(
                STATE_KEY_SPECIFIC_BIBLE_VERSION_INDEX, 0);
            enterHighlightMode(savedBibleVersionIndex)
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_IN_HIGHLIGHT_MODE, inHighlightMode)
        outState.putInt(STATE_KEY_SPECIFIC_BIBLE_VERSION_INDEX, specificBibleVersionIndex)
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
            selectedChapterContentScroller.viewTreeObserver.removeOnScrollChangedListener(this)
            enterHighlightMode(specificBibleVersionIndex)
        }
    }

    fun canEnterHighlightMode(): Boolean {
        if (!fragment.viewModel.isLastLoadResultValid()) {
            AppUtils.showShortToast(fragment.context, fragment.getString(
                R.string.message_book_loading_unfinished))
            return false
        }
        return true
    }

    fun enterHighlightMode(specificBibleVersionIndex: Int) {
        if (!canEnterHighlightMode()) {
            return
        }

        this.specificBibleVersionIndex = specificBibleVersionIndex
        val latestSysBookmark = fragment.viewModel.currLoc
        switchToChapterFocusView(fragment.viewModel.lastLoadResult!!, latestSysBookmark)

        defaultView.visibility = View.INVISIBLE
        chapterFocusView.visibility = View.VISIBLE
        updateTextSizes()

        inHighlightMode = true
        (fragment.activity as MainActivity).invalidateOptionsMenu()
        fragment.syncViewWithDataContext()
    }

    fun exitHighlightMode(): Boolean {
        if (!inHighlightMode) {
            return false
        }
        defaultView.visibility = View.VISIBLE
        chapterFocusView.visibility = View.INVISIBLE
        selectedChapterContentScroller.viewTreeObserver.removeOnScrollChangedListener(this)
        selectedChapterContent.text = "" // should clear any selection
        inHighlightMode = false
        (fragment.activity as MainActivity).invalidateOptionsMenu()
        fragment.syncViewWithDataContext()
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
        val bibleVersionCode = currentLoadResult.bibleVersions[specificBibleVersionIndex]
        val bibleVersion = AppConstants.bibleVersions.getValue(bibleVersionCode)
        val chapterTitle = bibleVersion.getChapterTitle(currentLoadResult.bookNumber,
            sysBookmark.chapterNumber)
        selectedChapterTitle.text = chapterTitle

        selectedChapterContent.text = fetchChapterContent(currentLoadResult,
            sysBookmark.chapterNumber)

        // delay goToVerse so htmlTextView gets the chance to redraw
        val initialVerseNumber = if (sysBookmark.verseNumber > 0) sysBookmark.verseNumber
        else {
            if (sysBookmark.equivalentViewItemType == BookDisplayItemViewType.TITLE) 0
            else htmlViewManager.lastVerseNumberSeen
        }
        selectedChapterContentScroller.post {
            htmlViewManager.initiateVerseSelection(initialVerseNumber, selectedChapterContent)
            htmlViewManager.scrollToVerse(
                initialVerseNumber, selectedChapterContentScroller, selectedChapterContent
            )

            updateDefaultViewScrollPos(initialVerseNumber, sysBookmark.chapterNumber, false)

            selectedChapterContentScroller.viewTreeObserver.addOnScrollChangedListener(this)
        }
    }

    override fun onScrollChanged() {
        val scrollY = selectedChapterContentScroller.scrollY
        val vNum = htmlViewManager.getVerseNumber(selectedChapterContent, scrollY)
        //android.util.Log.d(TAG, "Scroll pos $scrollY points to verse $vNum")

        if (vNum != -1) {
            val latestSysBookmarkChapterNumber = fragment.viewModel.currLocChapterNumber
            updateDefaultViewScrollPos(vNum, latestSysBookmarkChapterNumber, true)
        }
    }

    private fun updateDefaultViewScrollPos(vNum: Int, bookmarkChapterNumber: Int,
                                           debounce: Boolean) {
        if (!debounce || vNum != fragment.viewModel.currLocVerseNumber) {
            fragment.viewModel.updateSystemBookmarks(bookmarkChapterNumber, vNum)
            if (fragment.viewModel.currLocViewItemPos != -1) {
                fragment.scrollBook(fragment.viewModel.currLocViewItemPos)
            }
        }
    }

    private fun fetchChapterContent(currentLoadResult: BookDisplay, chapterNumber: Int): Spanned {
        val chapterIndex = currentLoadResult.chapterIndices[chapterNumber - 1]
        val titleItem = currentLoadResult.displayItems[chapterIndex]
        AppUtils.assert(titleItem.chapterNumber == chapterNumber)
        AppUtils.assert(titleItem.viewType == BookDisplayItemViewType.TITLE)
        var i = chapterIndex + 1
        val chapterContent = StringBuilder()
        var lastVerseNum = 0
        var lastVerseBlockIndex = 0
        while (i < currentLoadResult.displayItems.size) {
            val item = currentLoadResult.displayItems[i]
            if (item.chapterNumber != chapterNumber) {
                break
            }
            var contentByParts: List<BookDisplayItemContent>? = null
            var fullContent: BookDisplayItemContent? = null
            if (item.viewType == BookDisplayItemViewType.VERSE) {
                if (currentLoadResult.bibleVersionIndexInUI == null && currentLoadResult.displayMultipleSideBySide) {
                    contentByParts = if (specificBibleVersionIndex == 0) {
                        item.firstPartialContent
                    } else {
                        item.secondPartialContent
                    }
                }
                else if (item.fullContent.bibleVersionIndex == specificBibleVersionIndex) {
                    fullContent = item.fullContent
                }
            }
            if (contentByParts != null || fullContent != null) {
                if (lastVerseNum != item.verseNumber) {
                    if (lastVerseNum > 0) {
                        val vTag = "${htmlViewManager.vPrefix}$lastVerseNum"
                        chapterContent.append("<br></$vTag>")
                    }
                    lastVerseNum = item.verseNumber
                    lastVerseBlockIndex = 0
                    val vTag = "${htmlViewManager.vPrefix}$lastVerseNum"
                    chapterContent.append("<$vTag>")
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
        AppUtils.assert(lastVerseNum > 0)
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
            if (m.removeDuringHighlighting) {
                transformer.addTransform("", m.pos, m.pos + m.tag.length)
            }
            else {
                val mId = m.id as String
                AppUtils.assert(mId.startsWith(BookHighlighter.MARKUP_ID_HIGHLIGHT))
                val idSuffix = mId.substring(BookHighlighter.MARKUP_ID_HIGHLIGHT.length)
                AppUtils.assert(idSuffix.length == 9) {
                    "Unexpected highlight markup suffix: $idSuffix (from $m)"
                }
                var tempTag = "<"
                if (idSuffix.startsWith("-")) {
                    tempTag += "/"
                }
                tempTag += "${htmlViewManager.highlightTagPrefix}${idSuffix.substring(1)}>"
                transformer.addTransform(tempTag, m.pos, m.pos + m.tag.length)
            }
        }
        return transformer.transformedText.toString()
    }

    fun addHighlightRange() {
        val selStart = selectedChapterContent.selectionStart
        val selEnd = selectedChapterContent.selectionEnd
        val changes = VerseHighlighter.determineBlockRangesAffectedBySelection(selStart, selEnd,
                htmlViewManager.verseBlockRanges)
        if (changes.isEmpty()) {
            AppUtils.showShortToast(fragment.context,
                    fragment.getString(R.string.message_no_highlightable_content))
        }
        else {
            fragment.viewModel.updateHighlights(specificBibleVersionIndex, changes, false)
        }
    }

    fun removeHighlightRange() {
        val selStart = selectedChapterContent.selectionStart
        val selEnd = selectedChapterContent.selectionEnd
        val changes = VerseHighlighter.determineBlockRangesAffectedBySelection(selStart, selEnd,
                htmlViewManager.verseBlockRanges)
        if (changes.isNotEmpty()) {
            fragment.viewModel.updateHighlights(specificBibleVersionIndex, changes, true)
        }
    }
}