package com.aaronicsubstances.niv1984.ui.book_reading

import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.data.BookLoader
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice

object BookReadingEventListenerImpl {

    fun handleUrlClick(fragment: BookLoadFragment, bibleVersionIndex: Int, url: String) {
        AppUtils.assert(url.startsWith("ft-"))
        val parts = url.split('-')
        val chapterNumber = Integer.parseInt(parts[1])
        val noteRefNumber = Integer.parseInt(parts[2])
        if (!fragment.viewModel.isLastLoadResultValid()) {
            return
        }
        var temp = fragment.viewModel.lastLoadResult!!
        var idx = if (chapterNumber == temp.chapterIndices.size) {
            temp.displayItems.size - 1
        } else {
            temp.chapterIndices[chapterNumber] - 1
        }
        AppUtils.assert(temp.displayItems[idx].viewType == BookDisplayItemViewType.DIVIDER)
        idx--
        while (true) {
            val item = temp.displayItems[idx]
            if (item.fullContent.bibleVersionIndex == bibleVersionIndex &&
                    item.fullContent.footNoteId == url) {
                break
            }
            idx--
            AppUtils.assert (idx >= 0,  {
                "Error: footnote $url not found"
            })
        }
        val footNoteItem = temp.displayItems[idx]
        AppUtils.assert(footNoteItem.viewType == BookDisplayItemViewType.FOOTNOTE)

        // since adapter already uses html to cache item, we have no choice but to
        // parse html each time over here
        val dialogMessage = run {
            val prefix = BookLoader.createNoteRefHtml(chapterNumber, noteRefNumber, false).first
            AppUtils.assert(footNoteItem.fullContent.text.trim().startsWith(prefix))
            AppUtils.parseHtml(footNoteItem.fullContent.text.trim().substring(prefix.length))
        }
        val bibleVersion = AppConstants.bibleVersions.getValue(
            fragment.bibleVersions[bibleVersionIndex])
        var dialogTitle = bibleVersion.getChapterTitle(fragment.bookNumber, chapterNumber)
        dialogTitle += " footnote"
        dialogTitle += " (${bibleVersion.abbreviation})"

        MaterialDialog(fragment.requireActivity()).show {
            title(text = dialogTitle)
            message(text = dialogMessage)
        }
    }

    fun handleBookmarkCreate(fragment: BookLoadFragment) {
        if (!fragment.viewModel.isLastLoadResultValid()) {
            AppUtils.showShortToast(fragment.context, fragment.getString(
                R.string.message_book_loading_unfinished))
            return
        }
        MaterialDialog(fragment.requireActivity()).show {
            input(maxLength = 250) { dialog, text ->
                val description = text.toString().trim()
                if (description.isNotEmpty()) {
                    fragment.viewModel.createUserBookmark(description)
                }
            }
            title(R.string.action_bookmark_create)
            positiveButton(R.string.action_create)
        }
    }

    fun handleCopyModeEntryRequest(fragment: BookLoadFragment) {
        if (!fragment.viewModel.isLastLoadResultValid()) {
            AppUtils.showShortToast(fragment.context, fragment.getString(
                R.string.message_book_loading_unfinished))
            return
        }
        if (fragment.bibleVersions.size == 1 || fragment.bibleVersionIndex != null) {
            fragment.highlightHelper?.enterHighlightMode(
                fragment.bibleVersionIndex ?: 0)
            return
        }
        MaterialDialog(fragment.requireActivity()).show {
            title(R.string.dialog_title_select_bible_version)
            listItemsSingleChoice(
                items = fragment.bibleVersions.map { AppConstants.bibleVersions.getValue(it).description }
            ) { dialog, index, text ->
                // Invoked when the user taps an item
                fragment.highlightHelper?.enterHighlightMode(index)
            }
        }
    }
}