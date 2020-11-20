package com.aaronicsubstances.niv1984.ui.book_reading

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.dialogs.BookSelectionDialog
import com.aaronicsubstances.niv1984.ui.dialogs.ChapterSelectionDialog
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice

class BookOrChapterSwitchHandler(private val fragment: BookLoadFragment) {

    companion object {
        const val INTENT_EXTRA_BOOK_SELECTION = "selectedBookNumber"
        const val INTENT_EXTRA_CHAPTER_SELECTION = "selectedChapterNumber"
        private const val REQUEST_CODE_CHAPTER_SELECTED = 1
        private const val REQUEST_CODE_BOOK_SELECTED = 2
    }

    init {
        val root = fragment.requireView()
        val prevChapButton = root.findViewById<ImageButton>(R.id.prevChapButton)
        val nextChapButton = root.findViewById<ImageButton>(R.id.nextChapButton)
        val prevBookPresentButton = root.findViewById<ImageButton>(R.id.prevBookPresent)
        val nextBookPresentButton = root.findViewById<ImageButton>(R.id.nextBookPresent)
        val prevBookAbsentButton = root.findViewById<ImageButton>(R.id.prevBookAbsent)
        val nextBookAbsentButton = root.findViewById<ImageButton>(R.id.nextBookAbsent)

        val prevBookPresent = fragment.bookNumber > 1
        val nextBookPresent = fragment.bookNumber < AppConstants.BIBLE_BOOK_COUNT
        prevBookPresentButton.visibility = if (prevBookPresent) View.VISIBLE else View.GONE
        prevBookAbsentButton.visibility = if (!prevBookPresent) View.VISIBLE else View.GONE
        nextBookPresentButton.visibility = if (nextBookPresent) View.VISIBLE else View.GONE
        nextBookAbsentButton.visibility = if (!nextBookPresent) View.VISIBLE else View.GONE

        prevBookPresentButton.setOnClickListener {
            fragment.goToBook(fragment.bookNumber - 1, 0, 0)
        }
        nextBookPresentButton.setOnClickListener {
            fragment.goToBook(fragment.bookNumber + 1, 0, 0)
        }

        prevBookAbsentButton.setOnClickListener {
            AppUtils.showShortToast(fragment.context, fragment.getString(
                R.string.message_already_at_first_book))
        }
        nextBookAbsentButton.setOnClickListener {
            AppUtils.showShortToast(fragment.context, fragment.getString(
                R.string.message_already_at_last_book))
        }

        prevChapButton.setOnClickListener {
            val prevChapter = fragment.viewModel.currLocChapterNumber - 1
            if (prevChapter < 1) {
                AppUtils.showShortToast(fragment.context, fragment.getString(
                    R.string.message_already_at_first_chapter))
            }
            else {
                fragment.goToChapter(prevChapter)
            }
        }
        nextChapButton.setOnClickListener {
            val nextChapter = fragment.viewModel.currLocChapterNumber + 1
            if (nextChapter > AppConstants.BIBLE_BOOK_CHAPTER_COUNT[fragment.bookNumber - 1]) {
                AppUtils.showShortToast(fragment.context, fragment.getString(
                    R.string.message_already_at_last_chapter))
            }
            else {
                fragment.goToChapter(nextChapter)
            }
        }
    }

    fun startChapterSelection(selectedChapterNumber: Int) {
        val title = fragment.getEffectiveBookTitle()
        val dialog = ChapterSelectionDialog.newInstance(title,
            fragment.bookNumber, selectedChapterNumber)
        dialog.setTargetFragment(fragment, REQUEST_CODE_CHAPTER_SELECTED)
        val fm = fragment.requireActivity().supportFragmentManager
        val ft = fm.beginTransaction()
        val prev = fm.findFragmentByTag(MainActivity.FRAG_TAG_CHAPTER_SEL)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        dialog.show(ft, MainActivity.FRAG_TAG_CHAPTER_SEL)
    }

    fun startBookSelection() {
        // Could not use this because it doesn't automatically scroll and expose
        // selected book.
        /*if (fragment.bookNumber > 0) {
            val bookList = AppConstants.bibleVersions.getValue(fragment.bibleVersions[0]).bookNames
            MaterialDialog(fragment.requireActivity()).show {
                title(R.string.dialog_title_change_book)
                listItemsSingleChoice(
                    items = bookList,
                    initialSelection = fragment.bookNumber - 1
                ) { dialog, index, text ->
                    // Invoked when the user taps an item
                    if (index + 1 != fragment.bookNumber) {
                        fragment.goToBook(index + 1, 0, 0)
                    }
                }
            }
            return
        }*/
        val dialog = BookSelectionDialog.newInstance(fragment.bibleVersions[0],
            fragment.bookNumber)
        dialog.setTargetFragment(fragment, REQUEST_CODE_BOOK_SELECTED)
        val fm = fragment.requireActivity().supportFragmentManager
        val ft = fm.beginTransaction()
        val prev = fm.findFragmentByTag(MainActivity.FRAG_TAG_BOOK_SEL)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        dialog.show(ft, MainActivity.FRAG_TAG_BOOK_SEL)
    }

    fun onActivityResultFromFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_CODE_BOOK_SELECTED -> {
                val selectedBookNumber = data!!.getIntExtra(INTENT_EXTRA_BOOK_SELECTION, 1)
                fragment.goToBook(selectedBookNumber, 0, 0)
            }
            REQUEST_CODE_CHAPTER_SELECTED -> {
                val selectedChapterNumber = data!!.getIntExtra(INTENT_EXTRA_CHAPTER_SELECTION, 1)
                fragment.goToChapter(selectedChapterNumber)
            }
        }
    }
}