package com.aaronicsubstances.largelistpaging

import android.os.SystemClock

abstract class AbstractBatchedDataSourceK<T : ExtendedLargeListItem>(
    newBatchSize: Int,
    maxBatchCount: Int
) {
    private val newBatchSize = if (newBatchSize < 1) 20 else newBatchSize
    private val maxBatchCount = if (maxBatchCount < 1) 10 else maxBatchCount

    suspend fun loadBatch(
        isInitialLoad: Boolean, category: String, batchVersion: String,
        exclusiveBoundaryRank: Int?, isScrollInForwardDirection: Boolean, loadSize: Int,
        asyncContext: Any?
    ): UnboundedDataSource.LoadResult<T> {
        val rankBounds = AbstractBatchedDataSourceJ.calculateRankBounds(
            isInitialLoad, exclusiveBoundaryRank, isScrollInForwardDirection,
            loadSize
        )

        val startRank = rankBounds[0]
        val endRank = rankBounds[1]
        if (endRank < 0) {
            return UnboundedDataSource.LoadResult(listOf())
        }
        if (isInitialLoad) {
            // wipe out all batches in category across all versions.
            daoDeleteCategory(asyncContext, category)
        }
        val batch = daoGetBatch(
            asyncContext,
            category, batchVersion, startRank, endRank
        )
        return if (batch.isNotEmpty()) {
            completeBatchLoading(
                asyncContext, category, batchVersion, startRank, endRank,
                batch, true
            )
        } else {
            startNewBatchLoading(
                asyncContext, category, batchVersion,
                startRank, endRank, isScrollInForwardDirection
            )
        }
    }

    private suspend fun completeBatchLoading(
        asyncContext: Any?, category: String, batchVersion: String, startRank: Int, endRank: Int,
        batch: List<Any>?, listValid: Boolean
    ): UnboundedDataSource.LoadResult<T> {
        var batch = batch
        if (batch == null) {
            batch = daoGetBatch(asyncContext, category, batchVersion, startRank, endRank)
        }
        val successResult = mutableListOf<T>()
        for (wrapper in batch) {
            val item = unwrapItem(wrapper)
            successResult.add(item)
        }
        return UnboundedDataSource.LoadResult(successResult, null, listValid)
    }

    private suspend fun startNewBatchLoading(
        asyncContext: Any?, category: String, batchVersion: String,
        startRank: Int, endRank: Int, isScrollInForwardDirection: Boolean
    ): UnboundedDataSource.LoadResult<T> {
        // determine batch numbers for rank bounds.
        val startBatchNumber = AbstractBatchedDataSourceJ.calculateBatchNumber(startRank, newBatchSize)
        val endBatchNumber = AbstractBatchedDataSourceJ.calculateBatchNumber(endRank, newBatchSize)

        var successResultValid = true
        for (batchNumber in startBatchNumber..endBatchNumber) {
            val batch = fetchNewBatch(asyncContext, batchNumber, newBatchSize)

            // Assign ranks and update times, and save to database.
            val wrappers = mutableListOf<Any>()
            val itemKeys = mutableListOf<Any>()
            val startBatchRank = (batchNumber - 1) * newBatchSize
            for (i in batch.indices) {
                val rank = startBatchRank + i
                val item = batch[i]
                item.storeRank(rank)
                item.storeLastUpdateTimestamp(createCurrentTimeStamp())

                val wrapper = createItemWrapper(
                    item.fetchLastUpdateTimestamp(), category, batchVersion, batchNumber,
                    rank, item)
                wrappers.add(wrapper)
                itemKeys.add(item.fetchKey())
            }

            // Wipe out previous data.
            daoDeleteBatch(asyncContext, category, batchVersion, batchNumber)

            if (itemKeys.isNotEmpty()) {
                // Check for invalidity before saving.
                val duplicateCount = daoGetItemCount(asyncContext, category, batchVersion, itemKeys)
                if (duplicateCount > 0) {
                    successResultValid = false
                }
                daoInsert(asyncContext, wrappers)
            }

            // if not enough data in batch when scrolling backwards,
            // then data is invalid, so don't proceed further.
            if (batch.size < newBatchSize) {
                if (!isScrollInForwardDirection) {
                    successResultValid = false
                }
                break
            }
        }

        // before returning, truncate database of batches if too many.
        val batchCount = daoGetDistinctBatchCount(asyncContext, category, batchVersion)
        if (batchCount > maxBatchCount) {
            val batchNumberToDrop = if (isScrollInForwardDirection) {
                daoGetMinBatchNumber(asyncContext, category, batchVersion)
            } else {
                daoGetMaxBatchNumber(asyncContext, category, batchVersion)
            }
            daoDeleteBatch(asyncContext, category, batchVersion, batchNumberToDrop)
        }

        return completeBatchLoading(
            asyncContext,
            category,
            batchVersion,
            startRank,
            endRank,
            null,
            successResultValid
        )
    }

    protected open fun createCurrentTimeStamp(): Long {
        return SystemClock.uptimeMillis()
    }

    protected abstract fun createItemWrapper(
        lastUpdateTimestamp: Long, category: String, batchVersion: String,
        batchNumber: Int, rank: Int, item: T): Any

    protected abstract fun unwrapItem(wrapper: Any): T

    protected abstract suspend fun fetchNewBatch(
        asyncContext: Any?,
        batchNumber: Int,
        batchSize: Int
    ): List<T>

    protected abstract suspend fun daoInsert(asyncContext: Any?, wrappers: List<Any>)

    protected abstract suspend fun daoGetBatch(
        asyncContext: Any?, category: String, batchVersion: String, startRank: Int, endRank: Int
    ): List<Any>

    protected abstract suspend fun daoDeleteCategory(asyncContext: Any?, category: String)

    protected abstract suspend fun daoDeleteBatch(
        asyncContext: Any?, category: String,
        batchVersion: String, batchNumber: Int
    )

    protected abstract suspend fun daoGetDistinctBatchCount(
        asyncContext: Any?,
        category: String,
        batchVersion: String
    ): Int

    protected abstract suspend fun daoGetMinBatchNumber(
        asyncContext: Any?,
        category: String,
        batchVersion: String
    ): Int

    protected abstract suspend fun daoGetMaxBatchNumber(
        asyncContext: Any?,
        category: String,
        batchVersion: String
    ): Int

    protected abstract suspend fun daoGetItemCount(
        asyncContext: Any?, category: String,
        batchVersion: String, itemKeys: List<Any>
    ): Int
}
