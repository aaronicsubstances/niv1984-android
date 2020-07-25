package com.aaronicsubstances.niv1984.ui.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.largelistpaging.DefaultPaginationEventListener
import com.aaronicsubstances.largelistpaging.UnboundedDataPaginator
import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.SearchResult
import javax.inject.Inject

class SearchViewModel(application: Application): AndroidViewModel(application) {

    val paginator: UnboundedDataPaginator<SearchResult>

    @Inject
    internal lateinit var sharedPrefManager: SharedPrefManager

    @Inject
    internal lateinit var pagingConfig: LargeListPagingConfig

    @Inject
    internal lateinit var context: Context

    init {
        (application as MyApplication).appComponent.inject(this)

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
               startBookNumber: Int, inclEndBookNumber: Int,
               includeFootnotes: Boolean, treatSearchAsContains: Boolean,
               treatQueryAsAlternatives: Boolean) {
        if (_searchResultLiveData.value != null) {
            return
        }
        val dataSource = SearchResultDataSource(context, viewModelScope, query, bibleVersions,
            startBookNumber, inclEndBookNumber, includeFootnotes, treatSearchAsContains,
            treatQueryAsAlternatives)
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