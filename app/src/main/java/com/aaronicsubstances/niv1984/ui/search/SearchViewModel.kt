package com.aaronicsubstances.niv1984.ui.search

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
import com.aaronicsubstances.niv1984.data.SearchResultDataSource
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.SearchResult
import javax.inject.Inject

class SearchViewModel(application: Application): AndroidViewModel(application) {

    val paginator: UnboundedDataPaginator<SearchResult>

    @Inject
    internal lateinit var sharedPrefManager: SharedPrefManager

    @Inject
    internal lateinit var context: Context

    init {
        (application as MyApplication).appComponent.inject(this)

        // reduce search start up time by reducing initial load size such that
        // only one DB trip is made initially.
        // Also make max load size larger.
        val pagingConfig = LargeListPagingConfig.Builder()
            .setInitialLoadSize(SearchResultDataSource.NEW_BATCH_SIZE/ 2)
            .setLoadSize(SearchResultDataSource.NEW_BATCH_SIZE / 2)
            .setMaxLoadSize(SearchResultDataSource.NEW_BATCH_SIZE * 5)
            .build()
        paginator = UnboundedDataPaginator(pagingConfig)
        paginator.addEventListener(SearchResultHelper())
    }

    override fun onCleared() {
        super.onCleared()
        paginator.dispose()
    }

    private val _searchResultLiveData = MutableLiveData<List<SearchResult>>()
    val searchResultLiveData: LiveData<List<SearchResult>>
        get() = _searchResultLiveData

    fun search(query: String, bibleVersions: List<String>,
               startBookNumber: Int, inclEndBookNumber: Int) {
        if (_searchResultLiveData.value != null) {
            return
        }
        val dataSource = SearchResultDataSource(context, viewModelScope, query, bibleVersions,
            sharedPrefManager.getPreferredBibleVersions(),
            startBookNumber, inclEndBookNumber)
        paginator.loadInitialAsync(dataSource, null)
    }

    inner class SearchResultHelper : DefaultPaginationEventListener<SearchResult>() {

        override fun onDataLoaded(
            reqId: Int,
            data: MutableList<SearchResult>,
            dataValid: Boolean,
            isScrollInForwardDirection: Boolean
        ) {
            _searchResultLiveData.value = data
        }
    }
}