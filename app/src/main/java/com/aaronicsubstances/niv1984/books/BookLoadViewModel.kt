package com.aaronicsubstances.niv1984.books

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
import com.aaronicsubstances.niv1984.persistence.SharedPrefManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookLoadViewModel(application: Application): AndroidViewModel(application) {

    private val _loadLiveData: MutableLiveData<Pair<BookDisplay, BookLoadAftermath>> = MutableLiveData()
    var lastLoadResult: BookDisplay? = null
        private set

    private var lastJob: Job? = null

    private var systemBookmarks = ScrollPosPref(0, 0, 0, 0,
            listOf(), BookDisplayItemViewType.CHAPTER_FRAGMENT)

    @Inject
    internal lateinit var sharedPrefManager: SharedPrefManager

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    override fun onCleared() {
        super.onCleared()
        saveSystemBookmarks()
    }

    private fun loadSystemBookmarks(bookNumber: Int) {
        if (systemBookmarks.bookNumber > 0) {
            return
        }
        systemBookmarks = sharedPrefManager.loadPrefItem(
            SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS +
                    bookNumber, ScrollPosPref::class.java) ?: ScrollPosPref(
                bookNumber, 1, 0, 0,
                listOf(), BookDisplayItemViewType.TITLE)
    }

    private fun saveSystemBookmarks() {
        if (systemBookmarks.bookNumber > 0) {
            sharedPrefManager.savePrefItem(
                SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS + systemBookmarks.bookNumber,
                systemBookmarks
            )
        }
    }

    val loadLiveData: LiveData<Pair<BookDisplay, BookLoadAftermath>>
        get() = _loadLiveData

    fun loadBook(bookNumber: Int, bibleVersions: List<String>, displayMultipleSideBySide: Boolean,
                 isNightMode: Boolean) {
        if (lastLoadResult != null) {
            val temp = lastLoadResult!!
            if (temp.bibleVersions == bibleVersions &&
                    temp.displayMultipleSideBySide == displayMultipleSideBySide &&
                    temp.isNightMode == isNightMode) {
                //live data will republish lastLoadResult automatically when it is subscribed to.
                _loadLiveData.value = Pair(temp, BookLoadAftermath(-1,
                    systemBookmarks.chapterNumber))
                return
            }
        }

        val context = (getApplication() as MyApplication).applicationContext
        lastJob?.cancel()
        lastJob = viewModelScope.launch {
            val bookLoader = BookLoader(context, bookNumber, bibleVersions,
                displayMultipleSideBySide, isNightMode)
            val model = bookLoader.load()

            loadSystemBookmarks(bookNumber)

            // update system bookmarks in response to version switch, except
            // if loaded system bookmarks has same version as current request.
            // (which can only happen on the very first request).
            if (bibleVersions != systemBookmarks.particularBibleVersions ||
                    displayMultipleSideBySide != lastLoadResult?.displayMultipleSideBySide) {
                updateSystemBookmarksInternally(model)
            }

            val bookLoadAftermath = BookLoadAftermath(systemBookmarks.particularViewItemPos,
                systemBookmarks.chapterNumber)

            lastLoadResult = model
            _loadLiveData.value = Pair(model, bookLoadAftermath)
        }
    }

    private fun updateSystemBookmarksInternally(model: BookDisplay) {
        systemBookmarks.particularBibleVersions = model.bibleVersions
        var pos = model.chapterIndices[systemBookmarks.chapterNumber - 1]
        while (pos < model.displayItems.size) {
            val displayItem = model.displayItems[pos]
            if (displayItem.viewType == systemBookmarks.equivalentViewItemType) {
                if (displayItem.viewType != BookDisplayItemViewType.VERSE ||
                        displayItem.verseNumber == systemBookmarks.verseNumber) {
                    break
                }
            }
            pos++
        }
        systemBookmarks.particularViewItemPos = pos
    }

    fun updateSystemBookmarks(chapterNumber: Int, verseNumber: Int,
                              equivalentViewType: BookDisplayItemViewType,
                              particularPos: Int) {
        systemBookmarks.apply {
            this.equivalentViewItemType = equivalentViewType
            this.particularViewItemPos = particularPos
            this.chapterNumber = chapterNumber
            this.verseNumber = verseNumber
        }
    }
}

data class BookLoadAftermath(val particularPos: Int,
                             val chapterNumber: Int)