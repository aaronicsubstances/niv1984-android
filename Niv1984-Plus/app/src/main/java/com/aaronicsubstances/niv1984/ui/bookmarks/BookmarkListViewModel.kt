package com.aaronicsubstances.niv1984.ui.bookmarks

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.largelistpaging.BoundedDataPaginator
import com.aaronicsubstances.largelistpaging.DefaultPaginationEventListener
import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.AppDatabase
import com.aaronicsubstances.niv1984.data.BookmarkDataSource
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.utils.LiveDataEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookmarkListViewModel(application: Application): AndroidViewModel(application) {

    private val _bookmarkLiveData = MutableLiveData<List<BookmarkAdapterItem?>>()
    val bookmarkLiveData: LiveData<List<BookmarkAdapterItem?>>
        get() = _bookmarkLiveData

    private val _paginatorLiveData = MutableLiveData<LiveDataEvent<BoundedDataPaginator<BookmarkAdapterItem?>>>()
    val paginatorLiveData: LiveData<LiveDataEvent<BoundedDataPaginator<BookmarkAdapterItem?>>>
        get() = _paginatorLiveData

    private var paginator: BoundedDataPaginator<BookmarkAdapterItem?>? = null

    @Inject
    internal lateinit var context: Context
    @Inject
    internal lateinit var pagingConfig: LargeListPagingConfig

    init {
        (application.applicationContext as MyApplication).appComponent.inject(this)
    }

    fun loadBookmarks() {
        if (_bookmarkLiveData.value != null && paginator != null) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context).userBookmarkDao()
            var localPaginator = paginator
            if (localPaginator == null) {
                val totalCount = db.getTotalCount()
                localPaginator = BoundedDataPaginator(totalCount, pagingConfig)
                localPaginator.addEventListener(BookmarkListHelper())
                paginator = localPaginator
                _paginatorLiveData.postValue(LiveDataEvent(localPaginator))
            }
            val ds = BookmarkDataSource(context, viewModelScope)
            localPaginator.loadInitialAsync(ds, 0)
        }
    }

    fun reloadBookmarks() {
        paginator?.dispose()
        paginator = null
        loadBookmarks()
    }

    inner class BookmarkListHelper : DefaultPaginationEventListener<BookmarkAdapterItem?>() {
        override fun onDataLoaded(
            reqId: Int,
            data: MutableList<BookmarkAdapterItem?>?,
            dataValid: Boolean,
            isScrollInForwardDirection: Boolean
        ) {
            _bookmarkLiveData.value = data
        }
    }
}
