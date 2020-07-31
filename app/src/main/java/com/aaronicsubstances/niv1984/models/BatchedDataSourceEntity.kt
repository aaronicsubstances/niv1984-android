package com.aaronicsubstances.niv1984.models

import androidx.room.*

@Entity
data class BatchedDataSourceEntity(
    @PrimaryKey
    var rank: Int,
    var lastUpdateTimestamp: Long,
    var category: String,
    var batchVersion: String,
    var batchNumber: Int,
    var itemKey: String,
    var serializedItem: String
)

@Dao
interface BatchedDataSourceEntityDao {

    @Insert
    suspend fun insert(vararg entity: BatchedDataSourceEntity)

    @Query(
        """SELECT * FROM BatchedDataSourceEntity WHERE category = :category
        AND batchVersion = :batchVersion AND rank BETWEEN :startRank AND :endRank
        ORDER BY rank
    """
    )
    suspend fun getBatch(category: String, batchVersion: String, startRank: Int,
                 endRank: Int): List<BatchedDataSourceEntity>

    @Query("DELETE FROM BatchedDataSourceEntity WHERE category = :category")
    suspend fun deleteCategory(category: String)

    @Query(
        """DELETE FROM BatchedDataSourceEntity WHERE category = :category
        AND batchVersion = :batchVersion AND batchNumber = :batchNumber
    """
    )
    suspend fun deleteBatch(category: String, batchVersion: String, batchNumber: Int)

    @Query(
        """SELECT COUNT(DISTINCT batchNumber) FROM BatchedDataSourceEntity
        WHERE category = :category AND batchVersion = :batchVersion
    """
    )
    suspend fun getDistinctBatchCount(category: String, batchVersion: String): Int

    @Query(
        """SELECT MIN(batchNumber) FROM BatchedDataSourceEntity
        WHERE category = :category AND batchVersion = :batchVersion
    """
    )
    suspend fun getMinBatchNumber(category: String, batchVersion: String): Int


    @Query(
        """SELECT MAX(batchNumber) FROM BatchedDataSourceEntity
        WHERE category = :category AND batchVersion = :batchVersion
    """
    )
    suspend fun getMaxBatchNumber(category: String, batchVersion: String): Int

    @Query(
        """SELECT COUNT(*) FROM BatchedDataSourceEntity 
        WHERE category = :category AND batchVersion = :batchVersion
            AND itemKey IN (:itemKeys)
    """
    )
    suspend fun getItemCount(category: String, batchVersion: String, itemKeys: List<String>): Int
}