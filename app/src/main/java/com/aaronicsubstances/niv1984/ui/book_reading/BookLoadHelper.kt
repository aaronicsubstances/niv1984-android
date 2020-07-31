package com.aaronicsubstances.niv1984.ui.book_reading

import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.ui.view_adapters.BookLoadAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants

class BookLoadHelper(private val fragment: BookLoadFragment) {
    private val chapterFocusView: View
    private val selectedChapterTitle: TextView
    private val selectedChapterContent: TextView
    private val bookReadView: RecyclerView
    private val bookContentAdapter: BookLoadAdapter

    init {
        fragment.requireView().let {
            chapterFocusView = it.findViewById(R.id.chapterFocusView)
            selectedChapterTitle = it.findViewById(R.id.selectedChapterTitle)
            selectedChapterContent = it.findViewById(R.id.selectedChapterText)
            bookReadView = it.findViewById(R.id.bookReadView)
        }
        bookContentAdapter = fragment.bookContentAdapter

        val onItemLongClickListenerFactory = object: LargeListEventListenerFactory() {
            override fun <T> create(
                viewHolder: RecyclerView.ViewHolder,
                listenerCls: Class<T>, eventContextData: Any?
            ): T {
                assert(listenerCls == View.OnLongClickListener::class.java)
                return View.OnLongClickListener {
                    switchToChapterFocusView(getItem(viewHolder, bookContentAdapter),
                        eventContextData as Int)
                    true
                } as T
            }
        }
        bookContentAdapter.onItemLongClickListenerFactory = onItemLongClickListenerFactory
        fragment.requireActivity().onBackPressedDispatcher.addCallback(fragment,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!backPressIntercepted()){
                        isEnabled = false
                        fragment.requireActivity().onBackPressed()
                    }
                }
        })
    }

    private fun backPressIntercepted(): Boolean {
        if (bookReadView.visibility == View.VISIBLE) {
            return false
        }
        bookReadView.visibility = View.VISIBLE
        chapterFocusView.visibility = View.GONE
        return true
    }

    internal fun switchToChapterFocusView(item: BookDisplayItem, bibleVersionIndex: Int) {
        val bibleVersionCode = fragment.bookContentAdapter.bibleVersions[bibleVersionIndex]
        val chapterTitle = AppConstants.bibleVersions.getValue(bibleVersionCode).getChapterTitle(
            fragment.bookNumber, item.chapterNumber)
        selectedChapterTitle.text = chapterTitle
        val titleItem = BookDisplayItem(BookDisplayItemViewType.TITLE, item.chapterNumber,
            0, item.fullContent)
        bookContentAdapter.initDefault(titleItem, selectedChapterTitle)
        item.chapterNumber
        bookReadView.visibility = View.GONE
        chapterFocusView.visibility = View.VISIBLE
    }
}