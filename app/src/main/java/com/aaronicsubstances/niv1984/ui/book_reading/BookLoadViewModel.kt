package com.aaronicsubstances.niv1984.ui.book_reading

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.BookLoader
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.models.ScrollPosPref
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookLoadViewModel(application: Application): AndroidViewModel(application) {

    private val _loadLiveData: MutableLiveData<Pair<BookDisplay, BookLoadAftermath>> = MutableLiveData()
    var lastLoadResult: BookDisplay? = null
        private set

    private var lastJob: Job? = null

    private var systemBookmark = ScrollPosPref(0, 0, 0, 0,
            listOf(), null, BookDisplayItemViewType.CHAPTER_FRAGMENT, false)

    @Inject
    internal lateinit var sharedPrefManager: SharedPrefManager

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    override fun onCleared() {
        super.onCleared()
        saveSystemBookmarks()
    }

    fun initCurrLoc(bookNumber: Int, chapterNumber: Int, verseNumber: Int,
                    isSearchResultTriggered: Boolean) {
        if (systemBookmark.bookNumber > 0) {
            return
        }
        systemBookmark.apply {
            this.bookNumber = bookNumber
            this.chapterNumber = chapterNumber
            this.verseNumber = verseNumber
            this.equivalentViewItemType = BookDisplayItemViewType.VERSE
            if (verseNumber < 1) {
                this.equivalentViewItemType = if (isSearchResultTriggered) {
                    BookDisplayItemViewType.DIVIDER
                } else { BookDisplayItemViewType.TITLE }
            }
        }
    }

    fun initCurrLoc(bookmark: ScrollPosPref) {
        systemBookmark = bookmark
    }

    fun isLastLoadResultValid(bibleVersions: List<String>, bibleVersionIndex: Int?,
                              displayMultipleSideBySide: Boolean, isNightMode: Boolean): Boolean {
        val temp = lastLoadResult
        if (temp != null) {
            return temp.bibleVersions == bibleVersions &&
                    temp.bibleVersionIndexInUI == bibleVersionIndex &&
                    (temp.bibleVersionIndexInUI != null ||
                            temp.displayMultipleSideBySide == displayMultipleSideBySide) &&
                    temp.isNightMode == isNightMode
        }
        return false
    }

    val currLoc: ScrollPosPref
        get() = ScrollPosPref(systemBookmark.bookNumber, systemBookmark.chapterNumber,
            systemBookmark.verseNumber, systemBookmark.particularViewItemPos,
            systemBookmark.particularBibleVersions, systemBookmark.particularBibleVersionIndex,
            systemBookmark.equivalentViewItemType, systemBookmark.displayMultipleSideBySide)

    private fun loadSystemBookmarks(bookNumber: Int) {
        if (systemBookmark.bookNumber > 0) {
            return
        }
        systemBookmark = sharedPrefManager.loadPrefItem(
            SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS +
                    bookNumber, ScrollPosPref::class.java) ?: ScrollPosPref(
                bookNumber, 1, 0, 0,
                listOf(), 0, BookDisplayItemViewType.TITLE, false)
    }

    private fun saveSystemBookmarks() {
        if (systemBookmark.bookNumber > 0) {
            sharedPrefManager.savePrefItem(
                SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS + systemBookmark.bookNumber,
                systemBookmark
            )
        }
    }

    val loadLiveData: LiveData<Pair<BookDisplay, BookLoadAftermath>>
        get() = _loadLiveData

    fun loadBook(bookNumber: Int, bibleVersions: List<String>, bibleVersionIndex: Int?,
                 displayMultipleSideBySide: Boolean, isNightMode: Boolean) {
        if (isLastLoadResultValid(bibleVersions, bibleVersionIndex, displayMultipleSideBySide,
                isNightMode)) {
            // reuse lastLoadResult.
            _loadLiveData.value = Pair(lastLoadResult!!, BookLoadAftermath(-1,
                systemBookmark.chapterNumber))
            return
        }

        val context = (getApplication() as MyApplication).applicationContext
        lastJob?.cancel()
        lastJob = viewModelScope.launch {
            val bookLoader = BookLoader(context, bookNumber, bibleVersions, bibleVersionIndex,
                displayMultipleSideBySide, isNightMode)
            val model = bookLoader.load()

            loadSystemBookmarks(bookNumber)

            // update system bookmarks in response to version switch, except
            // if loaded system bookmarks has same version as current request.
            // (which can only happen on the very first request).
            var isParticularPosValid = bibleVersions == systemBookmark.particularBibleVersions &&
                bibleVersionIndex == systemBookmark.particularBibleVersionIndex &&
                    (bibleVersionIndex != null ||
                            displayMultipleSideBySide == systemBookmark.displayMultipleSideBySide)
            if (!isParticularPosValid) {
                updateSystemBookmarkInternally(model)
            }

            val bookLoadAftermath = BookLoadAftermath(systemBookmark.particularViewItemPos,
                systemBookmark.chapterNumber)

            lastLoadResult = model
            _loadLiveData.value = Pair(model, bookLoadAftermath)
        }
    }

    private fun updateSystemBookmarkInternally(model: BookDisplay) {
        systemBookmark.particularBibleVersions = model.bibleVersions
        systemBookmark.displayMultipleSideBySide = model.displayMultipleSideBySide
        systemBookmark.particularBibleVersionIndex = model.bibleVersionIndexInUI
        var pos = model.chapterIndices[systemBookmark.chapterNumber - 1]
        systemBookmark.particularViewItemPos = pos // set by default just in case none is found.
        while (pos < model.displayItems.size) {
            val displayItem = model.displayItems[pos]
            if (displayItem.viewType == systemBookmark.equivalentViewItemType) {
                if (displayItem.viewType != BookDisplayItemViewType.VERSE ||
                        displayItem.verseNumber == systemBookmark.verseNumber) {
                    systemBookmark.particularViewItemPos = pos
                    break
                }
            }
            pos++
        }
    }

    fun updateSystemBookmarks(chapterNumber: Int, verseNumber: Int,
                              equivalentViewType: BookDisplayItemViewType,
                              particularPos: Int) {
        systemBookmark.apply {
            this.equivalentViewItemType = equivalentViewType
            this.particularViewItemPos = particularPos
            this.chapterNumber = chapterNumber
            this.verseNumber = verseNumber
        }
    }
}

data class BookLoadAftermath(val particularPos: Int,
                             val chapterNumber: Int)