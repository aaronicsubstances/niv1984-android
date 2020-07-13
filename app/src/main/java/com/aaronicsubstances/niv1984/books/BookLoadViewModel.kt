package com.aaronicsubstances.niv1984.books

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.BookLoader
import com.aaronicsubstances.niv1984.models.BookDisplay
import com.aaronicsubstances.niv1984.models.BookDisplayItem
import kotlinx.coroutines.launch

class BookLoadViewModel(application: Application): AndroidViewModel(application) {

    private val _loadLiveData: MutableLiveData<List<BookDisplayItem>> = MutableLiveData()
    val loadLiveData: LiveData<List<BookDisplayItem>>
        get() = _loadLiveData

    private var lastLoadResult: BookDisplay? = null

    private val _scrollRecords = mutableMapOf<String, Int>()

    var lastScrollItemPos: Int?
        get() {
            val key = lastLoadResult?.bibleVersions?.joinToString("-")
            return _scrollRecords[key]
        }
        set(value) {
            val key = lastLoadResult!!.bibleVersions.joinToString("-")
            _scrollRecords[key] = value!!
        }

    fun loadBook(bookNumber: Int, bibleVersions: List<String>) {
        if (lastLoadResult?.bibleVersions == bibleVersions) {
            //live data will do this automatically when it is subscribed to.
        }
        else {
            val context = (getApplication() as MyApplication).applicationContext
            loadScrollRecords(context, bibleVersions)
            viewModelScope.launch {
                val bookLoader = BookLoader(context, bookNumber, bibleVersions)
                val model = bookLoader.load()
                lastLoadResult = model
                _loadLiveData.value = model.displayItems
            }
        }
    }

    private fun loadScrollRecords(context: Context, bibleVersions: List<String>) {

    }

    override fun onCleared() {
        super.onCleared()
        saveScrollRecords((getApplication() as MyApplication).applicationContext)
    }

    private fun saveScrollRecords(context: Context) {

    }
}