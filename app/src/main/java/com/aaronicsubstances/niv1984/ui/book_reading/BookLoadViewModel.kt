package com.aaronicsubstances.niv1984.ui.book_reading

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.BookHighlighter
import com.aaronicsubstances.niv1984.data.BookLoader
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.models.ScrollPosPref
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.VerseBlockHighlightRange
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
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

    @Inject
    internal lateinit var context: Context

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

    fun getValidLastLoadResult(bibleVersions: List<String>, bibleVersionIndex: Int?,
                               displayMultipleSideBySide: Boolean, isNightMode: Boolean): BookDisplay? {
        val temp = lastLoadResult
        if (temp != null) {
            if (temp.bibleVersions == bibleVersions &&
                    temp.bibleVersionIndexInUI == bibleVersionIndex &&
                    (temp.bibleVersionIndexInUI != null ||
                            temp.displayMultipleSideBySide == displayMultipleSideBySide) &&
                    temp.isNightMode == isNightMode) {
                return temp
            }
        }
        return null
    }

    val currLoc: ScrollPosPref
        get() = ScrollPosPref(systemBookmark.bookNumber, systemBookmark.chapterNumber,
            systemBookmark.verseNumber, systemBookmark.particularViewItemPos,
            systemBookmark.particularBibleVersions, systemBookmark.particularBibleVersionIndex,
            systemBookmark.equivalentViewItemType, systemBookmark.displayMultipleSideBySide)

    val currLocChapterNumber: Int
        get() = systemBookmark.chapterNumber

    val currLocVerseNumber: Int
        get() = systemBookmark.verseNumber

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
        val temp = getValidLastLoadResult(bibleVersions, bibleVersionIndex, displayMultipleSideBySide,
                isNightMode)
        if (temp != null) {
            // reuse lastLoadResult.
            _loadLiveData.value = Pair(temp, BookLoadAftermath(-1,
                systemBookmark.chapterNumber))
            return
        }

        lastJob?.cancel()
        lastJob = viewModelScope.launch {
            val bookLoader = BookLoader(context, bookNumber, bibleVersions, bibleVersionIndex,
                displayMultipleSideBySide, isNightMode)
            val model = bookLoader.load()

            loadSystemBookmarks(bookNumber)

            // update system bookmarks in response to version switch, except
            // if loaded system bookmarks has same version as current request.
            // (which can only happen on the very first request).
            var isParticularPosValid = systemBookmark.particularViewItemPos >= 0 &&
                    bibleVersions == systemBookmark.particularBibleVersions &&
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
        updateParticularPos(model)
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

    fun updateParticularPos(model: BookDisplay): Int {
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
        return systemBookmark.particularViewItemPos
    }

    fun updateHighlights(changes: List<VerseBlockHighlightRange>, removeHighlight: Boolean) {
        viewModelScope.launch {
            val temp = lastLoadResult!!
            val bookHighlighter = BookHighlighter(context, temp.bookNumber,
                temp.bibleVersions[temp.bibleVersionIndexInUI ?: 0])
            bookHighlighter.save(systemBookmark.chapterNumber, changes, removeHighlight)
            if (getValidLastLoadResult(temp.bibleVersions, temp.bibleVersionIndexInUI,
                            temp.displayMultipleSideBySide, temp.isNightMode) != null) {
                lastLoadResult = null
                loadBook(temp.bookNumber, temp.bibleVersions,
                        temp.bibleVersionIndexInUI, temp.displayMultipleSideBySide,
                        temp.isNightMode)
            }
        }
    }
}

data class BookLoadAftermath(val particularPos: Int,
                             val chapterNumber: Int)