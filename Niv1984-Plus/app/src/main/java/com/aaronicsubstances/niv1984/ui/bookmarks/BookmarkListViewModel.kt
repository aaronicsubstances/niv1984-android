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
import com.aaronicsubstances.niv1984.data.BookmarkDataSource
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import javax.inject.Inject

class BookmarkListViewModel(application: Application): AndroidViewModel(application) {

    private val _bookmarkLiveData = MutableLiveData<List<BookmarkAdapterItem>>()
    val bookmarkLiveData: LiveData<List<BookmarkAdapterItem>>
        get() = _bookmarkLiveData

    val paginator: UnboundedDataPaginator<BookmarkAdapterItem>

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
        val ds = BookmarkDataSource(context, viewModelScope)
        paginator.loadInitialAsync(ds, null)
    }

    fun reloadBookmarks() {
        _bookmarkLiveData.value = null
        loadBookmarks()
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
