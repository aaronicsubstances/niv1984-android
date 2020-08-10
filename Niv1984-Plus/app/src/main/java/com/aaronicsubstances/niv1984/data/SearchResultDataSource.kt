package com.aaronicsubstances.niv1984.data

import android.content.Context
import androidx.core.util.Consumer
import com.aaronicsubstances.largelistpaging.AbstractBatchedDataSourceK
import com.aaronicsubstances.largelistpaging.LargeListPagingConfig
import com.aaronicsubstances.largelistpaging.UnboundedDataSource
import com.aaronicsubstances.niv1984.models.BatchedDataSourceEntityDao
import com.aaronicsubstances.niv1984.models.BatchedDataSourceEntity
import com.aaronicsubstances.niv1984.models.BibleIndexRecordDao
import com.aaronicsubstances.niv1984.models.SearchResult
import com.aaronicsubstances.niv1984.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchResultDataSource(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val query: String,
    private val selectedBibleVersions: List<String>,
    private val preferredBibleVersions: List<String>,
    private val startBookNumber: Int,
    private val inclEndBookNumber: Int
): AbstractBatchedDataSourceK<SearchResult>(NEW_BATCH_SIZE, Int.MAX_VALUE),
    UnboundedDataSource<SearchResult> {

    companion object {
        private const val CAT_SEARCH = "search"
        const val NEW_BATCH_SIZE = 200

        internal fun splitUserQuery(rawUserQuery: String): List<String> {
            // Replace all chars which are neither letters nor digits with space.
            val processed = StringBuilder()
            for (i in rawUserQuery.indices) {
                val c = rawUserQuery[i]
                if (Character.isLetterOrDigit(c)) {
                    processed.append(c)
                } else {
                    processed.append(" ")
                }
            }
            val terms = processed.toString().split(" ").filter { it.isNotEmpty() }
            return terms
        }

        internal fun transformUserQuery(
            terms: List<String>, treatAsExact: Boolean, omittedTermCount: Int
        ): String {
            if (treatAsExact) {
                if (terms.isEmpty()) {
                    return ""
                }
                // use phrase query
                return "\"" + terms.joinToString(" ") + "\""
            } else {
                val nearQueryLen = terms.size - omittedTermCount
                if (nearQueryLen < 1) {
                    return ""
                }
                val nearQueries = mutableListOf<String>()
                for (i in 0..omittedTermCount) {
                    // quote terms as phrases or else lowercase them to prevent clash with keywords
                    val nearQuery = terms.subList(i, i + nearQueryLen)
                        .joinToString(" NEAR/3 ") { "\"$it\"" }
                    nearQueries.add(nearQuery)
                }
                return nearQueries.joinToString(" OR ")
            }
        }
    }

    data class HelperAsyncContext(
        val dao: BatchedDataSourceEntityDao,
        val ftsDao: BibleIndexRecordDao
    )

    private var lastInitialLoadRequestId: String = ""
    private var queryTerms = listOf<String>()
    private var transformedQuery: String = ""
    private var stage = 0

    override fun loadInitialData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        initialKey: Any?,
        loadCallback: Consumer<UnboundedDataSource.LoadResult<SearchResult>>
    ) {
        // reset state
        stage = 0
        lastInitialLoadRequestId = "$loadRequestId"
        queryTerms = splitUserQuery(query)
        transformedQuery = transformUserQuery(queryTerms, true, 0)

        coroutineScope.launch(Dispatchers.IO) {
            val db = SearchDatabase.getDatabase(context)
            val asyncContext =
                HelperAsyncContext(db.batchedDataSourceDao(), db.bibleIndexRecordDao())
            val result = loadBatch(
                true, CAT_SEARCH, lastInitialLoadRequestId, null,
                true, config.initialLoadSize, asyncContext
            )
            loadCallback.accept(result)
        }
    }

    override fun loadData(
        loadRequestId: Int,
        config: LargeListPagingConfig,
        boundaryKey: Any,
        isScrollInForwardDirection: Boolean,
        loadCallback: Consumer<UnboundedDataSource.LoadResult<SearchResult>>
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val db = SearchDatabase.getDatabase(context)
            val asyncContext =
                HelperAsyncContext(db.batchedDataSourceDao(), db.bibleIndexRecordDao())
            val result = loadBatch(
                false, CAT_SEARCH, lastInitialLoadRequestId, boundaryKey as Int,
                isScrollInForwardDirection, config.loadSize, asyncContext
            )
            loadCallback.accept(result)
        }
    }

    override suspend fun fetchNewBatch(
        asyncContext: Any?,
        batchNumber: Int,
        batchSize: Int
    ): List<SearchResult> {
        asyncContext as HelperAsyncContext

        val acrossStageResults = mutableListOf<SearchResult>()
        val exclusions = mutableListOf<Int>()
        let {
            val result = asyncContext.ftsDao.search(
                transformedQuery,
                selectedBibleVersions,
                preferredBibleVersions[0], preferredBibleVersions[1],
                startBookNumber,
                inclEndBookNumber,
                exclusions,
                CAT_SEARCH,
                lastInitialLoadRequestId,
                batchNumber,
                batchSize
            )
            if (result.size == batchSize || stage == queryTerms.size) {
                return result
            }
            acrossStageResults.addAll(result)
            exclusions.addAll(result.map { it.docId })
        }
        while (acrossStageResults.size < batchSize && stage < queryTerms.size) {
            // time to advance stage.
            stage++
            transformedQuery = transformUserQuery(queryTerms, false, stage - 1)
            val rem = asyncContext.ftsDao.search(
                transformedQuery, selectedBibleVersions,
                preferredBibleVersions[0], preferredBibleVersions[1],
                startBookNumber, inclEndBookNumber,
                exclusions, CAT_SEARCH, lastInitialLoadRequestId, batchNumber,
                batchSize - acrossStageResults.size)
            exclusions.addAll(rem.map { it.docId })
            acrossStageResults.addAll(rem)
        }
        return acrossStageResults
    }

    override suspend fun daoInsert(asyncContext: Any?, wrappers: List<Any>) {
        asyncContext as HelperAsyncContext
        val expectedTypedEntities = Array(wrappers.size) {
            wrappers[it] as BatchedDataSourceEntity
        }
        asyncContext.dao.insert(*expectedTypedEntities)
    }

    override suspend fun daoGetBatch(
        asyncContext: Any?,
        category: String,
        batchVersion: String,
        startRank: Int,
        endRank: Int
    ): List<Any> {
        asyncContext as HelperAsyncContext
        return asyncContext.dao.getBatch(category, batchVersion, startRank, endRank)
    }

    override suspend fun daoDeleteCategory(asyncContext: Any?, category: String) {
        asyncContext as HelperAsyncContext
        asyncContext.dao.deleteCategory(category)
    }

    override suspend fun daoDeleteBatch(
        asyncContext: Any?,
        category: String,
        batchVersion: String,
        batchNumber: Int
    ) {
        asyncContext as HelperAsyncContext
        asyncContext.dao.deleteBatch(category, batchVersion, batchNumber)
    }

    override suspend fun daoGetDistinctBatchCount(
        asyncContext: Any?,
        category: String,
        batchVersion: String
    ): Int {
        asyncContext as HelperAsyncContext
        return asyncContext.dao.getDistinctBatchCount(category, batchVersion)
    }

    override suspend fun daoGetMinBatchNumber(
        asyncContext: Any?,
        category: String,
        batchVersion: String
    ): Int {
        asyncContext as HelperAsyncContext
        return asyncContext.dao.getMinBatchNumber(category, batchVersion)
    }

    override suspend fun daoGetMaxBatchNumber(
        asyncContext: Any?,
        category: String,
        batchVersion: String
    ): Int {
        asyncContext as HelperAsyncContext
        return asyncContext.dao.getMaxBatchNumber(category, batchVersion)
    }

    override suspend fun daoGetItemCount(
        asyncContext: Any?,
        category: String,
        batchVersion: String,
        itemKeys: List<Any>
    ): Int {
        asyncContext as HelperAsyncContext
        return asyncContext.dao.getItemCount(category, batchVersion, itemKeys.map {"$it"})
    }

    override fun createItemWrapper(
        lastUpdateTimestamp: Long,
        category: String,
        batchVersion: String,
        batchNumber: Int,
        rank: Int,
        item: SearchResult
    ): Any {
        val serializedItem = AppUtils.serializeAsJson(item)
        return BatchedDataSourceEntity(0,
            rank, lastUpdateTimestamp, category, batchVersion,
            batchNumber, item.fetchKey().toString(), serializedItem
        )
    }

    override fun unwrapItem(wrapper: Any): SearchResult {
        wrapper as BatchedDataSourceEntity
        val item = AppUtils.deserializeFromJson(wrapper.serializedItem, SearchResult::class.java)
        return item
    }
}