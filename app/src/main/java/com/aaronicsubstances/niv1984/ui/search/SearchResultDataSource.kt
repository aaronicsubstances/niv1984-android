package com.aaronicsubstances.niv1984.ui.search

import android.content.Context
import androidx.core.util.Consumer
import com.aaronicsubstances.largelistpaging.KeyedDataSource
import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import com.aaronicsubstances.niv1984.data.AppDatabase
import com.aaronicsubstances.niv1984.models.SearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SearchResultDataSource(private val context: Context,
                             private val coroutineScope: CoroutineScope,
                             query: String,
                             private val bibleVersions: List<String>,
                             private val startBookNumber: Int,
                             private val inclEndBookNumber: Int,
                             includeFootnotes: Boolean,
                             private val treatSearchAsContains: Boolean,
                             treatQueryAsAlternatives: Boolean): KeyedDataSource<SearchResult> {
    private val minVerseNumber = if (includeFootnotes) 0 else 1
    private val transformedQuery = processQuery(query, treatSearchAsContains, treatQueryAsAlternatives)

    override fun loadInitialData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        initialKey: Any?,
        loadCallback: Consumer<KeyedDataSource.LoadResult<SearchResult>>
    ) {
        _loadData(-1, true, config.initialLoadSize, loadCallback)
    }

    override fun loadData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        boundaryKey: Any,
        isScrollInForwardDirection: Boolean,
        loadCallback: Consumer<KeyedDataSource.LoadResult<SearchResult>>
    ) {
        _loadData(boundaryKey as Int, isScrollInForwardDirection, config.loadSize, loadCallback)
    }

    private fun _loadData(
        boundaryRowId: Int,
        isScrollInForwardDirection: Boolean,
        fetchLimit: Int,
        loadCallback: Consumer<KeyedDataSource.LoadResult<SearchResult>>
    ) {

        coroutineScope.launch {
            val appDb = AppDatabase.getDatabase(context)
            val results = if (treatSearchAsContains) {
                if (isScrollInForwardDirection) {
                    appDb.bibleIndexRecordDao().searchExactForward(
                        transformedQuery,
                        bibleVersions, minVerseNumber, startBookNumber, inclEndBookNumber,
                        boundaryRowId, fetchLimit
                    )
                } else {
                    appDb.bibleIndexRecordDao().searchExactBackward(
                        transformedQuery,
                        bibleVersions, minVerseNumber, startBookNumber, inclEndBookNumber,
                        boundaryRowId, fetchLimit
                    ).reversed().toList()
                }
            } else {
                if (isScrollInForwardDirection) {
                    appDb.bibleIndexRecordDao().searchFuzzyForward(
                        transformedQuery,
                        bibleVersions, minVerseNumber, startBookNumber, inclEndBookNumber,
                        boundaryRowId, fetchLimit
                    )
                } else {
                    appDb.bibleIndexRecordDao().searchFuzzyBackward(
                        transformedQuery,
                        bibleVersions, minVerseNumber, startBookNumber, inclEndBookNumber,
                        boundaryRowId, fetchLimit
                    ).reversed().toList()
                }
            }
            loadCallback.accept(KeyedDataSource.LoadResult(results))
        }
    }

    companion object {
        internal fun processQuery(rawUserQuery: String, treatSearchAsContains: Boolean,
                                  treatQueryAsAlternatives: Boolean): String {
            var processed = normalizeContent(rawUserQuery)
            if (treatSearchAsContains) {
                // escape %, _ and \, and surround with %
                processed = processed.replace("%|_|\\\\", "\\\\$0")
                return "%$processed%"
            }
            else {
                // Remove all chars which are neither letters nor digits
                // and lowercase letters.
                var sb = StringBuilder()
                for (i in processed.indices) {
                    var c = processed[i]
                    if (Character.isLetterOrDigit(c)) {
                        sb.append(c.toLowerCase())
                    }
                    else {
                        sb.append(" ")
                    }
                }
                // split on space, convert each term to phrase query and join with OR or NEAR
                return sb.toString().split(" ")
                    .joinToString(if (treatQueryAsAlternatives) " OR " else " NEAR ") {
                        "\"$it\""
                    }
            }
        }

        internal fun normalizeContent(c: String) =
            // normalize multiple whitespace
            c.replace(Regex("\\s+"), " ").
                // normalize non-english twi alphabets
                replace(Regex("\u025B|\u0190"), "e").
                replace(Regex("\u0254|\u0186"), "o")
    }
}