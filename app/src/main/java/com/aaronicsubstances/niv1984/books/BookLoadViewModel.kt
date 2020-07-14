package com.aaronicsubstances.niv1984.books

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.BookLoader
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import com.aaronicsubstances.niv1984.models.BookDisplayItemViewType
import com.aaronicsubstances.niv1984.models.ScrollPosPref
import com.aaronicsubstances.niv1984.persistence.SharedPrefManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookLoadViewModel(application: Application): AndroidViewModel(application) {

    private val _loadLiveData: MutableLiveData<BookDisplay> = MutableLiveData()
    private var lastLoadResult: BookDisplay? = null
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

    private fun loadSystemBookmarks(bookNumber: Int, bibleVersions: List<String>): Boolean {
        if (systemBookmarks.bookNumber > 0) {
            systemBookmarks.particularBibleVersions = bibleVersions
            return true
        }
        var temp = sharedPrefManager.loadPrefItem(
            SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS +
                    bookNumber, ScrollPosPref::class.java
        )

        if (temp?.particularBibleVersions == bibleVersions) {
            // fine
        }
        else {
            // invalidate bookmark if on the very first load attempt, it is
            // having a different particular set of bible versions
            temp = ScrollPosPref(
                bookNumber, 1, 0, 0,
                bibleVersions, BookDisplayItemViewType.TITLE
            )
        }
        systemBookmarks = temp
        return false
    }

    private fun saveSystemBookmarks() {
        if (systemBookmarks.bookNumber > 0) {
            sharedPrefManager.savePrefItem(
                SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS + systemBookmarks.bookNumber,
                systemBookmarks
            )
        }
    }

    val loadLiveData: LiveData<BookDisplay>
        get() = _loadLiveData

    var bookLoadAftermath: BookLoadAftermath? = null
        private set

    fun loadBook(bookNumber: Int, bibleVersions: List<String>, displayMultipleSideBySide: Boolean) {
        bookLoadAftermath = null
        if (lastLoadResult?.bibleVersions == bibleVersions) {
            //live data will do this automatically when it is subscribed to.
        }
        else {
            val context = (getApplication() as MyApplication).applicationContext
            lastJob?.cancel()
            lastJob = viewModelScope.launch {
                val bookLoader = BookLoader(context, bookNumber, bibleVersions,
                    displayMultipleSideBySide = displayMultipleSideBySide)
                val model = bookLoader.load()

                // only request scroll to a particular position if
                // 1. view model is loading system bookmarks for the first time.
                // 2. particular bible versions stored in system bookmarks is still
                //    equal to the one being requested now. May be different if
                //    settings of preferred bible versions changes.
                val systemBookmarksAlreadyLoaded = loadSystemBookmarks(bookNumber, bibleVersions)

                // update system bookmarks in response to version switch
                if (systemBookmarksAlreadyLoaded) {
                    updateParticularPos(model)
                }

                bookLoadAftermath = BookLoadAftermath(systemBookmarks.particularViewItemPos)

                lastLoadResult = model
                _loadLiveData.value = model
            }
        }
    }

    private fun updateParticularPos(model: BookDisplay) {
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

data class BookLoadAftermath(
    val particularPos: Int)