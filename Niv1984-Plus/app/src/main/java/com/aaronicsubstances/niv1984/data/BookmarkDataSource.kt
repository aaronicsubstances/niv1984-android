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
    private val coroutineScope: CoroutineScope,
    private val sortByAccessDate: Boolean
): UnboundedDataSource<BookmarkAdapterItem> {

    private fun _loadData(
        boundaryId: Any?, isScrollInForwardDirection: Boolean, loadSize: Int,
        loadCallback: Consumer<UnboundedDataSource.LoadResult<BookmarkAdapterItem>>
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val rawResults: List<UserBookmark> =
                if (sortByAccessDate) {
                    if (boundaryId == null) {
                        db.userBookmarkDao().getInitialSortedByDate(loadSize)
                    } else if (isScrollInForwardDirection) {
                        db.userBookmarkDao().getNextAfterSortedByDate(boundaryId as Timestamp, loadSize)
                    } else {
                        db.userBookmarkDao().getPreviousBeforeSortedByDate(boundaryId as Timestamp, loadSize)
                            .sortedByDescending { it.dateUpdated }
                    }
                }
            else {
                if (boundaryId == null) {
                    db.userBookmarkDao().getInitialSortedByTitle(loadSize)
                } else if (isScrollInForwardDirection) {
                    db.userBookmarkDao().getNextAfterSortedByTitle(boundaryId as String, loadSize)
                } else {
                    db.userBookmarkDao().getPreviousBeforeSortedByTitle(boundaryId as String, loadSize)
                        .sortedBy { it.title }
                }
            }
            val results = mutableListOf<BookmarkAdapterItem>()
            for (rawResult in rawResults) {
                val scrollPosPref = AppUtils.deserializeFromJson(rawResult.serializedData,
                    ScrollPosPref::class.java)
                results.add(BookmarkAdapterItem(rawResult.id, rawResult.title, scrollPosPref,
                    rawResult.dateUpdated, sortByAccessDate))
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
        _loadData(initialKey, true, config.initialLoadSize, loadCallback)
    }

    override fun loadData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        boundaryKey: Any,
        isScrollInForwardDirection: Boolean,
        loadCallback: Consumer<UnboundedDataSource.LoadResult<BookmarkAdapterItem>>
    ) {
        _loadData(boundaryKey, isScrollInForwardDirection, config.loadSize, loadCallback)
    }
}