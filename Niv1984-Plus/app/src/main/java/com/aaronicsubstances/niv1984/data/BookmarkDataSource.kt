package com.aaronicsubstances.niv1984.data

import android.content.Context
import androidx.core.util.Consumer
import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import com.aaronicsubstances.largelistpaging.UnboundedDataSource
import com.aaronicsubstances.niv1984.models.BookmarkAdapterItem
import com.aaronicsubstances.niv1984.models.ScrollPosPref
import com.aaronicsubstances.niv1984.models.UserBookmark
import com.aaronicsubstances.niv1984.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Timestamp

class BookmarkDataSource(
    private val context: Context,
    private val coroutineScope: CoroutineScope
): UnboundedDataSource<BookmarkAdapterItem> {

    private fun _loadData(
        boundaryId: Timestamp?, isScrollInForwardDirection: Boolean, loadSize: Int,
        loadCallback: Consumer<UnboundedDataSource.LoadResult<BookmarkAdapterItem>>
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val rawResults: List<UserBookmark> =
                if (boundaryId == null) {
                    db.userBookmarkDao().getInitialSortedByDate(loadSize)
                }
                else if (isScrollInForwardDirection) {
                    db.userBookmarkDao().getNextAfterSortedByDate(boundaryId, loadSize)
                }
                else {
                    db.userBookmarkDao().getPreviousBeforeSortedByDate(boundaryId, loadSize)
                        .sortedByDescending { it.dateUpdated }
                }
            val results = mutableListOf<BookmarkAdapterItem>()
            for (rawResult in rawResults) {
                val scrollPosPref = AppUtils.deserializeFromJson(rawResult.serializedData,
                    ScrollPosPref::class.java)
                results.add(BookmarkAdapterItem(rawResult.title, scrollPosPref,
                    rawResult.dateUpdated))
            }
            loadCallback.accept(UnboundedDataSource.LoadResult(results))
        }
    }

    override fun loadInitialData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        initialKey: Any?,
        loadCallback: Consumer<UnboundedDataSource.LoadResult<BookmarkAdapterItem>>
    ) {
        var initialId: Timestamp? = null
        if (initialKey != null) {
            initialId = initialKey as Timestamp
        }
        _loadData(initialId, true, config.initialLoadSize, loadCallback)
    }

    override fun loadData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        boundaryKey: Any,
        isScrollInForwardDirection: Boolean,
        loadCallback: Consumer<UnboundedDataSource.LoadResult<BookmarkAdapterItem>>
    ) {
        _loadData(boundaryKey as Timestamp, isScrollInForwardDirection, config.loadSize, loadCallback)
    }
}