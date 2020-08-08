package com.aaronicsubstances.niv1984.ui.book_reading

import android.app.Activity
import android.content.Intent
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.MainActivity
import com.aaronicsubstances.niv1984.ui.dialogs.BookSelectionDialog
import com.aaronicsubstances.niv1984.ui.dialogs.ChapterSelectionDialog
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice

class BookOrChapterSwitchHandler(private val fragment: BookLoadFragment) {
    companion object {
        const val INTENT_EXTRA_BOOK_SELECTION = "selectedBookNumber"
        const val INTENT_EXTRA_CHAPTER_SELECTION = "selectedChapterNumber"
        private const val REQUEST_CODE_CHAPTER_SELECTED = 1
        private const val REQUEST_CODE_BOOK_SELECTED = 2
    }

    fun startChapterSelection(selectedChapterNumber: Int) {
        val title = fragment.getEffectiveTitle()
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
        /*if (fragment.bookNumber > 0) {
            val bookList = AppConstants.bibleVersions.getValue(fragment.bibleVersions[0]).bookNames
            MaterialDialog(fragment.requireActivity()).show {
                title(R.string.dialog_title_change_book)
                listItemsSingleChoice(
                    items = bookList,
                    initialSelection = fragment.bookNumber -1
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