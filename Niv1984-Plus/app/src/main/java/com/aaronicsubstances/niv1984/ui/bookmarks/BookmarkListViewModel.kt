package com.aaronicsubstances.niv1984.ui.bookmarks

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.largelistpaging.DefaultPaginationEventListener
import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import com.aaronicsubstances.largelistpaging.UnboundedDataPaginator
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.AppDatabase
import com.aaronicsubstances.niv1984.data.BookmarkDataSource
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.models.UserBookmarkUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

class BookmarkListViewModel(application: Application): AndroidViewModel(application) {

    private val _bookmarkLiveData = MutableLiveData<List<BookmarkAdapterItem>>()
    val bookmarkLiveData: LiveData<List<BookmarkAdapterItem>>
        get() = _bookmarkLiveData

    val paginator: UnboundedDataPaginator<BookmarkAdapterItem>

    private var _sortByAccessDate: Boolean = false
    var sortByAccessDate: Boolean
        get() = _sortByAccessDate
        set(value) {
            if (value != sortByAccessDate) {
                _sortByAccessDate = value
                _bookmarkLiveData.value = null
            }
        }

    @Inject
    internal lateinit var context: Context
    @Inject
    internal lateinit var pagingConfig: LargeListPagingConfig

    init {
        (application.applicationContext as MyApplication).appComponent.inject(this)
        paginator = UnboundedDataPaginator(pagingConfig)
        paginator.addEventListener(BookmarkListHelper())
    }

    fun loadBookmarks() {
        if (_bookmarkLiveData.value != null) {
            return
        }
        val ds = BookmarkDataSource(context, viewModelScope, sortByAccessDate)
        paginator.loadInitialAsync(ds, null)
    }

    fun updateAccessTime(item: BookmarkAdapterItem) {
        // use a scope which will survive view model destruction.
        GlobalScope.launch(Dispatchers.IO) {
            val update = UserBookmarkUpdate(item.id, Timestamp(System.currentTimeMillis()))
            AppDatabase.getDatabase(context).userBookmarkDao().update(update)
        }
    }

    fun deleteBookmarks(activity: BookmarkListActivity, selectedIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(context).userBookmarkDao().delete(selectedIds)
            activity.runOnUiThread {
                _bookmarkLiveData.value = null
                loadBookmarks()
            }
        }
    }

    inner class BookmarkListHelper : DefaultPaginationEventListener<BookmarkAdapterItem>() {
        override fun onDataLoaded(
            reqId: Int,
            data: MutableList<BookmarkAdapterItem>?,
            dataValid: Boolean,
            isScrollInForwardDirection: Boolean
        ) {
            _bookmarkLiveData.value = data
        }
    }
}
