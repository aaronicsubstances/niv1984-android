package com.aaronicsubstances.niv1984.scrollexperiment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aaronicsubstances.endlesspaginglib.*
import com.aaronicsubstances.niv1984.models.BookReadItem

class ExPosViewModel: ViewModel(), EndlessListViewModel<BookReadItem>,
        EndlessListDataSource<BookReadItem> {

    val endlessListRepo: EndlessListRepositoryForPositionalDS<BookReadItem>
    private val _loadLiveData: MutableLiveData<List<BookReadItem>>
    val loadLiveData: LiveData<List<BookReadItem>>
        get() = _loadLiveData

    init {
        endlessListRepo = EndlessListRepositoryForPositionalDS(5000, true)
        val endlessListConfig = EndlessListRepositoryConfig.Builder().setLoadSize(50).build()
        endlessListRepo.init(endlessListConfig, this)

        _loadLiveData = MutableLiveData()

    }

    override fun onCleared() {
        super.onCleared()
        endlessListRepo?.dispose()
    }

    fun load() {
        endlessListRepo.loadInitialAsync(0)
    }

    // ENDLESS LIST VIEW MODEL implementation

    override fun getListItemClass() = BookReadItem::class.java

    override fun getDataSource() = this

    override fun onListPageLoaded(
        error: Throwable?,
        data: List<BookReadItem>?,
        dataValid: Boolean
    ) {
        _loadLiveData.value = null
        assert(dataValid);
        if (error != null) {
            throw error;
        }
    }

    override fun onCurrentListInvalidated() = throw NotImplementedError()

    override fun createListItemIndicatingFurtherLoading(
        inAfterPosition: Boolean
    ) = throw NotImplementedError()

    override fun createListItemIndicatingError(
        error: Throwable,
        inAfterPosition: Boolean
    ) = throw NotImplementedError()

    // DATASOURCE implementation
    override fun fetchInitialDataAsync(
        config: EndlessListRepositoryConfig?,
        initialKey: Any?,
        dsCallback: EndlessListDataSource.Callback<BookReadItem>?
    ) {
        throw NotImplementedError()
    }

    override fun fetchDataAsync(
        config: EndlessListRepositoryConfig?,
        exclusiveBoundaryKey: Any?,
        useInAfterPosition: Boolean,
        dsCallback: EndlessListDataSource.Callback<BookReadItem>?
    ) {
        throw NotImplementedError()
    }

    override fun fetchPositionalDataAsync(
        config: EndlessListRepositoryConfig,
        inclusiveStartIndex: Int,
        exclusiveEndIndex: Int,
        pageNumber: Int,
        dsCallback: EndlessListDataSource.Callback<BookReadItem>
    ) {
        val list = mutableListOf<BookReadItem>();
        (inclusiveStartIndex until exclusiveEndIndex).forEach {
            list.add(BookReadItem(BookReadItem.Key(0, 0, ""),
                BookReadItem.ViewType.VERSE, 0, "${it + 1}. Sweet (page ${pageNumber + 1})"))
        }
        dsCallback.postDataLoadResult(EndlessListLoadResult(list))
    }
}