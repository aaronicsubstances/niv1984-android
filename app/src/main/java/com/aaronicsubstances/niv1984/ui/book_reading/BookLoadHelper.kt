package com.aaronicsubstances.niv1984.ui.book_reading

import android.text.Spanned
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemContent
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.ui.view_adapters.BookLoadAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils

class BookLoadHelper(private val fragment: BookLoadFragment) {
    private val defaultView: View
    private val chapterFocusView: View
    private val selectedBookTitle: TextView
    private val selectedChapterTitle: TextView
    private val selectedChapterContent: HtmlTextView
    private val selectedChapterContentScroller: HtmlScrollView
    private val bookContentAdapter: BookLoadAdapter

    private val htmlViewManager: HtmlViewManager
    private val backPressListener: OnBackPressedCallback

    //private val contextMenuActionCallback: ActionMode.Callback

    init {
        fragment.requireView().let {
            defaultView = it.findViewById(R.id.highlightOff)
            chapterFocusView = it.findViewById(R.id.highlightOn)
            selectedBookTitle = it.findViewById(R.id.bookDescriptionHighlighted)
            selectedChapterTitle = it.findViewById(R.id.chapterTitle)
            selectedChapterContent = it.findViewById(R.id.selectedChapterText)
            selectedChapterContentScroller = it.findViewById(R.id.selectedChapterScroller)
        }
        bookContentAdapter = fragment.bookContentAdapter
        htmlViewManager = HtmlViewManager(fragment.requireContext())

        selectedChapterContentScroller.viewDataChangeListener = htmlViewManager
        selectedChapterContent.viewDataChangeListener = htmlViewManager

        val onItemLongClickListenerFactory = object: LargeListEventListenerFactory() {
            override fun <T> create(
                viewHolder: RecyclerView.ViewHolder,
                listenerCls: Class<T>, eventContextData: Any?
            ): T {
                assert(listenerCls == View.OnLongClickListener::class.java)
                return View.OnLongClickListener {
                    val item = getItem(viewHolder, bookContentAdapter)
                    val bibleVersionIndex = eventContextData as Int
                    switchToChapterFocusView(item, bibleVersionIndex)
                    true
                } as T
            }
        }
        bookContentAdapter.onItemLongClickListenerFactory = onItemLongClickListenerFactory

        backPressListener = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!backPressIntercepted()) {
                    isEnabled = false
                    fragment.requireActivity().onBackPressed()
                }
            }
        }
        fragment.requireActivity().onBackPressedDispatcher.addCallback(fragment.viewLifecycleOwner,
            backPressListener)

        selectedChapterContent.customSelectionActionModeCallback = object: ActionMode.Callback {
            private var mode: ActionMode? = null
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                this.mode = mode
                mode.menuInflater.inflate(R.menu.book_load_floating, menu)
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
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                this.mode = null
            }

            fun finishActionMode() {
                mode?.finish()
                cancelChapterFocusView()
            }
        }
    }

    fun cancelChapterFocusView() {
        defaultView.visibility = View.VISIBLE
        chapterFocusView.visibility = View.INVISIBLE
        selectedChapterContent.text = ""
    }

    private fun backPressIntercepted(): Boolean {
        if (chapterFocusView.visibility != View.VISIBLE) {
            return false
        }
        cancelChapterFocusView()
        return true
    }

    private fun switchToChapterFocusView(item: BookDisplayItem, bibleVersionIndex: Int) {
        val titleItem = BookDisplayItem(BookDisplayItemViewType.TITLE, item.chapterNumber,
            0, item.fullContent)
        bookContentAdapter.initDefault(titleItem, selectedChapterTitle)
        bookContentAdapter.initDefault(item, selectedChapterContent)

        val bibleVersionCode = fragment.bookContentAdapter.bibleVersions[bibleVersionIndex]
        val bibleVersion = AppConstants.bibleVersions.getValue(bibleVersionCode)
        selectedBookTitle.text = fragment.getString(R.string.highlight_mode_title,
            bibleVersion.bookNames[fragment.bookNumber - 1])
        val chapterTitle = bibleVersion.getChapterTitle(
            fragment.bookNumber, item.chapterNumber)
        selectedChapterTitle.text = "(${bibleVersion.abbreviation}) $chapterTitle"

        selectedChapterContent.text = fetchChapterContent(item.chapterNumber, bibleVersionIndex)

        defaultView.visibility = View.INVISIBLE
        chapterFocusView.visibility = View.VISIBLE

        // delay goToVerse so htmlTextView gets the chance to redraw
        selectedChapterContentScroller.post {
            htmlViewManager.goToVerse(item.verseNumber, selectedChapterContent,
                selectedChapterContentScroller)
        }

        backPressListener.isEnabled = true
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
                if (bookContentAdapter.multipleDisplay) {
                    if (bookContentAdapter.displayMultipleSideBySide) {
                        contentByParts = if (bibleVersionIndex == 0) {
                            item.firstPartialContent
                        } else {
                            item.secondPartialContent
                        }
                    }
                }
                if (contentByParts == null && item.fullContent.bibleVersionIndex == bibleVersionIndex) {
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
                    for (part in it) {
                        chapterContent.append(part.text).append("<br>")
                    }
                }
                fullContent?.let { chapterContent.append(it.text) }
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