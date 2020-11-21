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
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SearchResultDataSource
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.models.SearchResult
import com.aaronicsubstances.niv1984.models.SearchResultAdapterItem
import com.aaronicsubstances.niv1984.utils.AppUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    private val _searchResultLiveData = MutableLiveData<List<SearchResultAdapterItem>>()
    val searchResultLiveData: LiveData<List<SearchResultAdapterItem>>
        get() = _searchResultLiveData

    fun search(query: String, bibleVersions: List<String>,
               startBookNumber: Int, inclEndBookNumber: Int,
               treatAsAlternatives: Boolean) {
        if (_searchResultLiveData.value != null) {
            return
        }
        val dataSource = SearchResultDataSource(context, viewModelScope, query, bibleVersions,
            sharedPrefManager.getPreferredBibleVersions(),
            startBookNumber, inclEndBookNumber, treatAsAlternatives)
        paginator.loadInitialAsync(dataSource, null)
    }

    inner class SearchResultHelper : DefaultPaginationEventListener<SearchResult>() {

        override fun onDataLoadError(
            reqId: Int,
            error: Throwable?,
            isScrollInForwardDirection: Boolean
        ) {
            LoggerFactory.getLogger(javaClass).error("Search error", error)
            AppUtils.showLongToast(context, context.getString(R.string.too_many_search_items_error))
        }

        override fun onDataLoaded(
            reqId: Int,
            data: MutableList<SearchResult>,
            dataValid: Boolean,
            isScrollInForwardDirection: Boolean
        ) {
            val adapterItems = mutableListOf<SearchResultAdapterItem>()
            adapterItems.addAll(paginator.currentList.map{
                SearchResultAdapterItem(0, it)
            })
            // add loading indicator at end if more data can be loaded, to
            // differentiate between end of overall loading and waiting for data to be loaded.
            if (isScrollInForwardDirection) {
                if (!paginator.isLastPageRequested) {
                    adapterItems.add(
                        SearchResultAdapterItem(
                            AppUtils.VIEW_TYPE_LOADING,
                            SearchResult()
                        )
                    )
                }
            }
            else {
                if (!paginator.isFirstPageRequested) {
                    adapterItems.add(
                        0, SearchResultAdapterItem(
                            AppUtils.VIEW_TYPE_LOADING,
                            SearchResult()
                        )
                    )
                }
            }
            _searchResultLiveData.value = adapterItems
        }
    }
}