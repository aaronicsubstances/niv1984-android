package com.aaronicsubstances.niv1984.data

import android.content.Context
import androidx.core.util.Consumer
import com.aaronicsubstances.largelistpaging.BoundedDataSource
import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.models.ScrollPosPref
import com.aaronicsubstances.niv1984.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

class BookmarkDataSource(
    private val context: Context,
    private val coroutineScope: CoroutineScope
): BoundedDataSource<BookmarkAdapterItem?> {

    override fun loadInitialData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        pageNumbers: MutableList<Int>,
        inclusiveStartIndex: Int,
        exclusiveEndIndex: Int,
        loadCallback: Consumer<BoundedDataSource.LoadResult<BookmarkAdapterItem?>>
    ) {
        _loadData(config, pageNumbers, loadCallback)
    }

    override fun loadData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        pageNumbers: MutableList<Int>,
        inclusiveStartIndex: Int,
        exclusiveEndIndex: Int,
        isScrollInForwardDirection: Boolean,
        loadCallback: Consumer<BoundedDataSource.LoadResult<BookmarkAdapterItem?>>
    ) {
        _loadData(config, pageNumbers, loadCallback)
    }

    private fun _loadData(
        config: LargeListPagingConfig,
        pageNumbers: MutableList<Int>,
        loadCallback: Consumer<BoundedDataSource.LoadResult<BookmarkAdapterItem?>>
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val results = db.userBookmarkDao().getPageSortedByDate(
                pageNumbers[0] * config.loadSize,
                pageNumbers.size * config.loadSize)
            val resultMap = mutableMapOf<Int, List<BookmarkAdapterItem?>>()
            var i = 0
            while (i < results.size) {
                resultMap[pageNumbers[0] + resultMap.size] = results.subList(i, min(i + config.loadSize,
                    results.size)).map {
                    val scrollPosPref = AppUtils.deserializeFromJson(it.serializedData,
                        ScrollPosPref::class.java)
                    BookmarkAdapterItem(it.title, scrollPosPref, it.dateUpdated)
                }
                i += config.loadSize
            }
            loadCallback.accept(BoundedDataSource.LoadResult(resultMap))
        }
    }

}