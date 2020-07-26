package com.aaronicsubstances.niv1984.models

import androidx.room.*
import com.aaronicsubstances.largelistpaging.BatchedDataSourceEntity

@Entity
data class BatchedDataSourceEntityImpl(
    @PrimaryKey
    var rank: Int,
    var lastUpdateTimestamp: Long,
    var category: String,
    var batchVersion: String,
    var batchNumber: Int,
    var itemKey: String,
    var serializedItem: String
): BatchedDataSourceEntity {

    override fun fetchRank() = rank
    override fun fetchItemKey() = itemKey
    override fun fetchSerializedItem() = serializedItem
}

@Dao
interface BatchedDataSourceEntityDao {

    @Insert
    suspend fun insert(vararg entity: BatchedDataSourceEntityImpl)

    @Query("""SELECT * FROM BatchedDataSourceEntityImpl WHERE category = :category
        AND batchVersion = :batchVersion AND rank BETWEEN :startRank AND :endRank
        ORDER BY rank
    """)
    suspend fun getBatch(category: String, batchVersion: String, startRank: Int,
                 endRank: Int): List<BatchedDataSourceEntityImpl>

    @Query("DELETE FROM BatchedDataSourceEntityImpl WHERE category = :category")
    suspend fun deleteCategory(category: String)

    @Query("""DELETE FROM BatchedDataSourceEntityImpl WHERE category = :category
        AND batchVersion = :batchVersion AND batchNumber = :batchNumber
    """)
    suspend fun deleteBatch(category: String, batchVersion: String, batchNumber: Int)

    @Query("""SELECT COUNT(DISTINCT batchNumber) FROM BatchedDataSourceEntityImpl
        WHERE category = :category AND batchVersion = :batchVersion
    """)
    suspend fun getDistinctBatchCount(category: String, batchVersion: String): Int

    @Query("""SELECT MIN(batchNumber) FROM BatchedDataSourceEntityImpl
        WHERE category = :category AND batchVersion = :batchVersion
    """)
    suspend fun getMinBatchNumber(category: String, batchVersion: String): Int


    @Query("""SELECT MAX(batchNumber) FROM BatchedDataSourceEntityImpl
        WHERE category = :category AND batchVersion = :batchVersion
    """)
    suspend fun getMaxBatchNumber(category: String, batchVersion: String): Int

    @Query("""SELECT COUNT(*) FROM BatchedDataSourceEntityImpl 
        WHERE category = :category AND batchVersion = :batchVersion
            AND itemKey IN (:itemKeys)
    """)
    suspend fun getItemCount(category: String, batchVersion: String, itemKeys: List<String>): Int

    @Query("""SELECT itemKey FROM BatchedDataSourceEntityImpl
        WHERE category = :category AND batchVersion = :batchVersion
            AND batchNumber = :batchNumber
            ORDER BY rank
            LIMIT 1
    """)
    suspend fun getLastItemKeyOfBatch(category: String, batchVersion: String, batchNumber: Int): String
}