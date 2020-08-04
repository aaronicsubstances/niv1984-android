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
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.models.ScrollPosPref
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.view_adapters.BookLoadAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class HighlightModeHelper(private val fragment: BookLoadFragment,
                          savedInstanceState: Bundle?) {

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
        if (!fragment.viewModel.isLastLoadResultValid(bookContentAdapter.bibleVersions,
                bookContentAdapter.bibleVersionIndexInUI, bookContentAdapter.displayMultipleSideBySide,
                bookContentAdapter.isNightMode)) {
            AppUtils.showShortToast(fragment.context, fragment.getString(
                R.string.message_highlight_mode_prohibited))
            return
        }

        val latestSysBookmark = fragment.viewModel.currLoc
        switchToChapterFocusView(latestSysBookmark)

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

    private fun switchToChapterFocusView(sysBookmark: ScrollPosPref) {
        val bibleVersionIndex = sysBookmark.particularBibleVersionIndex ?: 0
        val bibleVersionCode = fragment.bookContentAdapter.bibleVersions[bibleVersionIndex]
        val bibleVersion = AppConstants.bibleVersions.getValue(bibleVersionCode)
        val chapterTitle = bibleVersion.getChapterTitle(fragment.bookNumber, sysBookmark.chapterNumber)
        selectedChapterTitle.text = chapterTitle

        selectedChapterContent.text = fetchChapterContent(sysBookmark.chapterNumber,
            bibleVersionIndex)

        // delay goToVerse so htmlTextView gets the chance to redraw
        val initialVerseNumber = Math.max(1, sysBookmark.verseNumber)
        selectedChapterContentScroller.post {
            htmlViewManager.goToVerse(initialVerseNumber, selectedChapterContent,
                selectedChapterContentScroller)
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
                if (lastVerseNum != item.verseNumber) {
                    if (lastVerseNum > 0) {
                        if (contentByParts == null) {
                            chapterContent.append("<br>")
                        }
                        chapterContent.append("<br></p></${htmlViewManager.vPrefix}$lastVerseNum>")
                    }
                    chapterContent.append("<${htmlViewManager.vPrefix}${item.verseNumber}><p>")
                    lastVerseNum = item.verseNumber
                }
                contentByParts?.let {
                    for (partIdx in it.indices) {
                        val processed = processTextForHighlightMode(it[partIdx])
                        chapterContent.append(processed).append("<br>")
                    }
                }
                fullContent?.let {
                    val processed = processTextForHighlightMode(it)
                    chapterContent.append(processed)
                }
            }
            i++
        }
        if (lastVerseNum > 0) {
            chapterContent.append("</p></${htmlViewManager.vPrefix}$lastVerseNum>")
        }

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
        /*AppUtils.showShortToast(fragment.context, "selection ($selStart, $selEnd) " +
                "maps to verses $vStart-$vEnd")*/
    }

    fun removeHighlightRange() {

    }
}