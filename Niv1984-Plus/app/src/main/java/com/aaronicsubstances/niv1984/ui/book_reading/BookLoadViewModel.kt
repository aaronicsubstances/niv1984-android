package com.aaronicsubstances.niv1984.ui.book_reading

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.AppDatabase
import com.aaronicsubstances.niv1984.data.BookHighlighter
import com.aaronicsubstances.niv1984.data.BookLoader
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.*
import com.aaronicsubstances.niv1984.utils.AppUtils
import com.aaronicsubstances.niv1984.utils.LiveDataEvent
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

class BookLoadViewModel(application: Application): AndroidViewModel(application) {

    private val _loadLiveData: MutableLiveData<BookDisplay> = MutableLiveData()
    val loadLiveData: LiveData<BookDisplay>
        get() = _loadLiveData

    var lastLoadResult: BookDisplay? = null
        private set

    private val _loadProgressLiveData: MutableLiveData<LiveDataEvent<Boolean>> = MutableLiveData()
    val loadProgressLiveData: LiveData<LiveDataEvent<Boolean>>
        get() = _loadProgressLiveData

    private val _newBookmarkLiveData: MutableLiveData<LiveDataEvent<UserBookmark>> = MutableLiveData()
    val newBookmarkLiveData: LiveData<LiveDataEvent<UserBookmark>>
        get() = _newBookmarkLiveData

    var loadResultValidationCallback: ((BookDisplay?) -> Boolean)? = null

    private var systemBookmark = ScrollPosPref(0, 0, 0, 0,
            listOf(), null, BookDisplayItemViewType.TITLE,
        false, false)

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

    fun isLastLoadResultValid(): Boolean {
        return loadResultValidationCallback?.invoke(lastLoadResult) ?: false
    }

    val currLoc: ScrollPosPref
        get() = ScrollPosPref(systemBookmark.bookNumber, systemBookmark.chapterNumber,
            systemBookmark.verseNumber, systemBookmark.particularViewItemPos,
            systemBookmark.particularBibleVersions, systemBookmark.particularBibleVersionIndex,
            systemBookmark.equivalentViewItemType, systemBookmark.displayMultipleSideBySide,
            systemBookmark.atEndOfChapter)

    val currLocChapterNumber: Int
        get() = systemBookmark.chapterNumber

    val currLocVerseNumber: Int
        get() = systemBookmark.verseNumber

    val currLocViewItemPos: Int
        get() = systemBookmark.particularViewItemPos

    private fun loadSystemBookmarks(bookNumber: Int) {
        if (systemBookmark.bookNumber > 0) {
            return
        }
        systemBookmark = sharedPrefManager.loadPrefItem(
            SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS +
                    bookNumber, ScrollPosPref::class.java) ?: ScrollPosPref(
                bookNumber, 1, 0, 0,
                listOf(), 0, BookDisplayItemViewType.TITLE, false,
            false)
    }

    fun saveSystemBookmarks() {
        if (systemBookmark.bookNumber > 0) {
            sharedPrefManager.savePrefItem(
                SharedPrefManager.PREF_KEY_SYSTEM_BOOKMARKS + systemBookmark.bookNumber,
                systemBookmark
            )
        }
    }

    fun loadBook(bookNumber: Int, bibleVersions: List<String>, bibleVersionIndex: Int?,
                 displayMultipleSideBySide: Boolean, isNightMode: Boolean) {
        if (isLastLoadResultValid()) {
            // reuse lastLoadResult. if first time subscribe after config change,
            // live data mechanism will republish result.
            return
        }

        viewModelScope.launch {
            //val startTime = SystemClock.uptimeMillis()
            val bookLoader = BookLoader(context, bookNumber, bibleVersions, bibleVersionIndex,
                displayMultipleSideBySide, isNightMode)
            val model = bookLoader.load()
            //val timeTaken = SystemClock.uptimeMillis() - startTime
            //AppUtils.showShortToast(context, "Book loaded in ${timeTaken / 1000.0} secs")

            // don't proceed further if model is no longer needed due to UI change request.
            if (loadResultValidationCallback?.invoke(model) != true) {
                return@launch
            }

            loadSystemBookmarks(bookNumber)

            // update system bookmarks in response to version switch, except
            // if loaded system bookmarks has same version as current request.
            // (which is the case with a repeat book load request).
            var isParticularPosValid = systemBookmark.particularViewItemPos >= 0 &&
                    bibleVersions == systemBookmark.particularBibleVersions &&
                bibleVersionIndex == systemBookmark.particularBibleVersionIndex &&
                    (bibleVersionIndex != null ||
                            displayMultipleSideBySide == systemBookmark.displayMultipleSideBySide)
            if (!isParticularPosValid) {
                updateSystemBookmarkInternally(model)
            }

            lastLoadResult = model
            _loadLiveData.value = model
        }
    }

    private fun updateSystemBookmarkInternally(model: BookDisplay) {
        systemBookmark.particularBibleVersions = model.bibleVersions
        systemBookmark.displayMultipleSideBySide = model.displayMultipleSideBySide
        systemBookmark.particularBibleVersionIndex = model.bibleVersionIndexInUI
        updateParticularPos(model)
    }

    fun updateSystemBookmarks(equivalentDisplayItem: BookDisplayItem, displayItemPos: Int) {
        systemBookmark.apply {
            this.equivalentViewItemType = equivalentDisplayItem.viewType
            this.chapterNumber = equivalentDisplayItem.chapterNumber
            this.verseNumber = equivalentDisplayItem.verseNumber
            this.particularViewItemPos = displayItemPos
            this.atEndOfChapter = false
            if (equivalentDisplayItem.viewType == BookDisplayItemViewType.DIVIDER &&
                    !equivalentDisplayItem.fullContent.isFirstDivider) {
                if (particularBibleVersionIndex != null) {
                    this.atEndOfChapter = true
                }
                else {
                    // check that we are seeing second of two dividers in multiple display mode.
                    if (equivalentDisplayItem.fullContent.bibleVersionIndex == 1) {
                        this.atEndOfChapter = true
                    }
                }
            }
        }
    }

    fun updateSystemBookmarks(chapterNumber: Int, verseNumber: Int) {
        val model = lastLoadResult ?: return
        systemBookmark.apply {
            this.equivalentViewItemType = if (verseNumber > 0) BookDisplayItemViewType.VERSE
                else BookDisplayItemViewType.TITLE
            this.chapterNumber = chapterNumber
            this.verseNumber = verseNumber
            this.atEndOfChapter = false
            updateParticularPos(model)
        }
    }

    private fun updateParticularPos(model: BookDisplay) {
        var pos = if (systemBookmark.atEndOfChapter) {
            if (systemBookmark.chapterNumber == model.chapterIndices.size) {
                // get ending pos of book
                model.displayItems.size - 1
            }
            else {
                // get pos just before next chapter.
                model.chapterIndices[systemBookmark.chapterNumber] - 1
            }
        }
        else {
            // get beginning chapter pos
            model.chapterIndices[systemBookmark.chapterNumber - 1]
        }
        systemBookmark.particularViewItemPos = pos // set by default just in case none is found.
        while (pos < model.displayItems.size) {
            val displayItem = model.displayItems[pos]
            if (displayItem.chapterNumber != systemBookmark.chapterNumber) {
                break
            }
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

    fun updateHighlights(bibleVersionIndex: Int, changes: List<VerseBlockHighlightRange>,
                         removeHighlight: Boolean) {
        val temp = lastLoadResult!!
        viewModelScope.launch {
            val bookHighlighter = BookHighlighter(context, temp.bookNumber,
                temp.bibleVersions[bibleVersionIndex])
            bookHighlighter.save(systemBookmark.chapterNumber, changes, removeHighlight)
            if (loadResultValidationCallback?.invoke(temp) == true) {
                // reload book
                lastLoadResult = null
                loadBook(temp.bookNumber, temp.bibleVersions,
                        temp.bibleVersionIndexInUI, temp.displayMultipleSideBySide,
                        temp.isNightMode)
            }
        }
    }

    fun notifyUserOfOngoingLoadProgress() {
        _loadProgressLiveData.postValue(LiveDataEvent(true))
    }

    fun createUserBookmark(title: String) {
        val temp = lastLoadResult!!
        val bookmarkData = systemBookmark
        bookmarkData.particularBibleVersionIndex?.let {
            bookmarkData.particularBibleVersions = listOf(bookmarkData.particularBibleVersions[it])
            bookmarkData.particularBibleVersionIndex = 0
        }
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(context)
            val currTime = System.currentTimeMillis()
            val newEntity = UserBookmark(0, title, Timestamp(currTime),
                Timestamp(currTime), AppUtils.serializeAsJson(bookmarkData))
            db.userBookmarkDao().insert(newEntity)

            AppUtils.showShortToast(context, "Bookmark created")
            if (loadResultValidationCallback?.invoke(temp) == true) {
                _newBookmarkLiveData.value = LiveDataEvent(newEntity)
            }
        }
    }
}