package com.aaronicsubstances.niv1984.books

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aaronicsubstances.endlesspaginglib.*
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.models.BookReadItem
import com.aaronicsubstances.niv1984.repositories.BookReadDataSource
import javax.inject.Inject

class BookTextViewModel(application: Application) :
    AndroidViewModel(application), EndlessListViewModel<BookReadItem> {

    @Inject
    internal lateinit var endlessListConfig: EndlessListRepositoryConfig

    val endlessListRepo: EndlessListRepository<BookReadItem>
    private val _loadLiveData: MutableLiveData<List<BookReadItem>>
    val loadLiveData: LiveData<List<BookReadItem>>
        get() = _loadLiveData

    private var currentListDataSource: BookReadDataSource? = null
    private var lastLoadResult: List<BookReadItem>? = null

    init {
        (application as MyApplication).appComponent.inject(this)

        endlessListRepo = EndlessListRepository()
        endlessListRepo.init(endlessListConfig, this)

        _loadLiveData = MutableLiveData()
    }

    fun loadBook(bookNumber: Int, bibleVersions: List<String>,
                 initialKey: BookReadItem.Key?) {
        if (lastLoadResult != null && currentListDataSource != null &&
                bibleVersions == currentListDataSource?.bibleVersions) {
            //onListPageLoaded(null, lastLoadResult, true)
            //live data will do this automatically when it is subscribed to.
        }
        else {
            currentListDataSource = BookReadDataSource((getApplication() as MyApplication).applicationContext,
                viewModelScope, bookNumber, bibleVersions)
            endlessListRepo.loadInitialAsync(initialKey)
        }
    }

    override fun onCleared() {
        super.onCleared()
        endlessListRepo?.dispose()
    }

    override fun getListItemClass() = BookReadItem::class.java

    override fun getDataSource() = currentListDataSource

    override fun onListPageLoaded(
        error: Throwable?,
        data: List<BookReadItem>?,
        dataValid: Boolean
    ) {
        if (data != null) {
            _loadLiveData.value = data
        }
        assert(dataValid);
        if (error != null) {
            throw error;
        }

        lastLoadResult = data
    }

    override fun onCurrentListInvalidated() = throw NotImplementedError()

    override fun createListItemIndicatingFurtherLoading(
        inAfterPosition: Boolean
    ) = throw NotImplementedError()

    override fun createListItemIndicatingError(
        error: Throwable,
        inAfterPosition: Boolean
    ) = throw NotImplementedError()
}
