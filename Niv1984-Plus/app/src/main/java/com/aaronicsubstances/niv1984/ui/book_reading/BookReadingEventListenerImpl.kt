package com.aaronicsubstances.niv1984.ui.book_reading

import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.afollestad.materialdialogs.MaterialDialog

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
        }
        val footNoteItem = temp.displayItems[idx]
        AppUtils.assert(footNoteItem.viewType == BookDisplayItemViewType.FOOTNOTE)

        // since adapter already uses html to cache item, we have no choice but to
        // parse html each time over here
        val dialogMessage = run {
            val lowerA = 'a'.toInt()
            val charRef = (lowerA + noteRefNumber - 1).toChar()
            val prefix = "<sup>$charRef</sup>"
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
}