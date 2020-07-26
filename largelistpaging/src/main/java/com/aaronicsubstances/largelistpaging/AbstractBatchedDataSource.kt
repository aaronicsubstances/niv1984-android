package com.aaronicsubstances.largelistpaging

import android.os.Parcelable
import android.os.SystemClock
import android.util.Base64

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.ArrayList

abstract class AbstractBatchedDataSource<T : ExtendedLargeListItem>(
    private val itemClass: Class<T>,
    newBatchSize: Int,
    maxBatchCount: Int
) {
    private var newBatchSize: Int = 0
    private var maxBatchCount: Int = 0

    init {
        this.newBatchSize = newBatchSize
        this.maxBatchCount = maxBatchCount

        if (newBatchSize < 1) {
            this.newBatchSize = 20
        }
        if (maxBatchCount < 1) {
            this.maxBatchCount = 10
        }
    }

    suspend fun loadBatchAsync(
        isInitialLoad: Boolean, category: String, batchVersion: String,
        exclusiveBoundaryRank: Int?, isScrollInForwardDirection: Boolean, loadSize: Int,
        asyncContext: Any?
    ): UnboundedDataSource.LoadResult<T> {
        val rankBounds = calculateRankBounds(
            isInitialLoad, exclusiveBoundaryRank, isScrollInForwardDirection,
            loadSize
        )

        val startRank = rankBounds[0]
        val endRank = rankBounds[1]
        if (endRank < 0) {
            return UnboundedDataSource.LoadResult(ArrayList())
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
        batch: List<BatchedDataSourceEntity>?, listValid: Boolean
    ): UnboundedDataSource.LoadResult<T> {
        var batch = batch
        if (batch == null) {
            batch = daoGetBatch(asyncContext, category, batchVersion, startRank, endRank)
        }
        val successResult = ArrayList<T>()
        for (entity in batch) {
            val item = deserializeLargeListItem(entity.fetchSerializedItem())
            successResult.add(item)
        }
        return UnboundedDataSource.LoadResult(successResult, null, listValid)
    }

    private suspend fun startNewBatchLoading(
        asyncContext: Any?, category: String, batchVersion: String,
        startRank: Int, endRank: Int, isScrollInForwardDirection: Boolean
    ): UnboundedDataSource.LoadResult<T> {
        // determine batch numbers for rank bounds.
        val startBatchNumber = calculateBatchNumber(startRank, newBatchSize)
        val endBatchNumber = calculateBatchNumber(endRank, newBatchSize)

        var successResultValid = true
        for (batchNumber in startBatchNumber..endBatchNumber) {
            val batch = fetchNewBatch(asyncContext, batchNumber, newBatchSize)

            // Assign ranks and update times, and save to database.
            val entities = ArrayList<BatchedDataSourceEntity>()
            val startBatchRank = (batchNumber - 1) * newBatchSize
            for (i in batch.indices) {
                val rank = startBatchRank + i
                val item = batch[i]
                item.storeRank(rank)
                item.storeLastUpdateTimestamp(createCurrentTimeStamp())

                val serializedItem = serializeLargeListItem(item)
                val dbRecord = createBatchedDataSourceEntity(
                    item.fetchLastUpdateTimestamp(), category, batchVersion, batchNumber,
                    rank, item.fetchKey(), serializedItem
                )
                entities.add(dbRecord)
            }

            // Wipe out previous data.
            daoDeleteBatch(asyncContext, category, batchVersion, batchNumber)

            if (!entities.isEmpty()) {
                // Check for invalidity before saving.
                val itemKeys = ArrayList<String>()
                for (item in entities) {
                    itemKeys.add(item.fetchItemKey())
                }
                val duplicateCount = daoGetItemCount(asyncContext, category, batchVersion, itemKeys)
                if (duplicateCount > 0) {
                    successResultValid = false
                }
                daoInsert(asyncContext, entities)
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
            val batchNumberToDrop: Int
            if (isScrollInForwardDirection) {
                batchNumberToDrop = daoGetMinBatchNumber(asyncContext, category, batchVersion)
            } else {
                batchNumberToDrop = daoGetMaxBatchNumber(asyncContext, category, batchVersion)
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

    protected open fun serializeLargeListItem(obj: T): String {
        if (obj is Serializable) {
            val bOut = ByteArrayOutputStream()
            ObjectOutputStream(bOut).use {
                it.writeObject(obj)
            }
            return Base64.encodeToString(bOut.toByteArray(), Base64.DEFAULT)
        }
        throw UnsupportedOperationException(
            createSerializationExceptionMessage(
                javaClass,
                itemClass
            )
        )
    }

    protected open fun deserializeLargeListItem(value: String): T {
        if (Serializable::class.java.isAssignableFrom(itemClass)) {
            val decoded = Base64.decode(value, Base64.DEFAULT)
            val bIn = ByteArrayInputStream(decoded)
            return ObjectInputStream(bIn).use {
                it.readObject() as T
            }
        }
        throw UnsupportedOperationException(
            createSerializationExceptionMessage(
                javaClass,
                itemClass
            )
        )
    }

    protected abstract fun createBatchedDataSourceEntity(
        lastUpdateTimestamp: Long, category: String, batchVersion: String,
        batchNumber: Int, rank: Int, itemKey: Any, serializedItem: String
    ): BatchedDataSourceEntity

    protected abstract suspend fun fetchNewBatch(
        asyncContext: Any?,
        batchNumber: Int,
        batchSize: Int
    ): List<T>

    protected abstract suspend fun daoInsert(asyncContext: Any?, entities: List<BatchedDataSourceEntity>)

    protected abstract suspend fun daoGetBatch(
        asyncContext: Any?, category: String, batchVersion: String, startRank: Int, endRank: Int
    ): List<BatchedDataSourceEntity>

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
        batchVersion: String, itemKeys: List<String>
    ): Int

    companion object {

        internal fun calculateRankBounds(
            isInitialLoad: Boolean, exclusiveBoundaryRank: Int?,
            isScrollInForwardDirection: Boolean, loadSize: Int
        ): IntArray {
            var startRank: Int
            val endRank: Int
            if (isInitialLoad) {
                startRank = 0
                endRank = loadSize - 1
            } else if (isScrollInForwardDirection) {
                if (exclusiveBoundaryRank != null) {
                    startRank = exclusiveBoundaryRank + 1
                } else {
                    startRank = 0
                }
                endRank = startRank + loadSize - 1
            } else {
                if (exclusiveBoundaryRank != null) {
                    endRank = exclusiveBoundaryRank - 1
                } else {
                    endRank = 0
                }
                startRank = endRank - loadSize + 1
            }
            // check bounds of boundary rank numbers.
            if (startRank < 0) {
                startRank = 0
            }
            return intArrayOf(startRank, endRank)
        }

        internal fun calculateBatchNumber(rank: Int, batchSize: Int): Int {
            return rank / batchSize + 1
        }

        internal fun createSerializationExceptionMessage(
            thisClass: Class<*>,
            itemClass: Class<*>
        ): String {
            return String.format(
                "%1\$s does not implement %2\$s hence default serialization cannot be used with its instances. " +
                        "Either make %1\$s implement %2\$s, or override (de)serializeLargeListItem() methods in " +
                        "%3\$s with custom implementations (e.g. based on %4\$s or JSON).",
                itemClass, Serializable::class.java, thisClass, Parcelable::class.java
            )
        }
    }
}
