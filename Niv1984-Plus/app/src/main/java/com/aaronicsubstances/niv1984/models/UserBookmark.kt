package com.aaronicsubstances.niv1984.models

import androidx.room.*
import java.sql.Timestamp

@Entity
data class UserBookmark(

    @PrimaryKey(autoGenerate = true)
    var id: Int,

    var title: String,

    var dateCreated: Timestamp,

    var dateUpdated: Timestamp,

    var serializedData: String
)

data class UserBookmarkUpdate(
    val id: Int,

    val dateUpdated: Timestamp
)

@Dao
interface UserBookmarkDao {

    @Query(
        """SELECT * from UserBookmark
        ORDER BY dateUpdated DESC
        LIMIT :size"""
    )
    suspend fun getInitialSortedByDate(
        size: Int
    ): List<UserBookmark>

    @Query(
        """SELECT * from UserBookmark
        WHERE dateUpdated <= :maxDateUpdated
        ORDER BY dateUpdated DESC
        LIMIT :pageSize"""
    )
    suspend fun getNextAfterSortedByDate(
        maxDateUpdated: Timestamp, pageSize: Int
    ): List<UserBookmark>

    @Query(
        """SELECT * from UserBookmark
            WHERE dateUpdated >= :minDateUpdated
            ORDER BY dateUpdated ASC
            LIMIT :pageSize"""
    )
    suspend fun getPreviousBeforeSortedByDate(
        minDateUpdated: Timestamp, pageSize: Int
    ): List<UserBookmark>

    @Query(
        """SELECT * from UserBookmark
        ORDER BY title
        LIMIT :size"""
    )
    suspend fun getInitialSortedByTitle(
        size: Int
    ): List<UserBookmark>

    @Query(
        """SELECT * from UserBookmark
        WHERE title >= :startTitle
        ORDER BY title
        LIMIT :pageSize"""
    )
    suspend fun getNextAfterSortedByTitle(
        startTitle: String, pageSize: Int
    ): List<UserBookmark>

    @Query(
        """SELECT * from UserBookmark
            WHERE title <= :endTitle
            ORDER BY title DESC
            LIMIT :pageSize"""
    )
    suspend fun getPreviousBeforeSortedByTitle(
        endTitle: String, pageSize: Int
    ): List<UserBookmark>

    @Query(
        """SELECT COUNT(*) FROM UserBookmark"""
    )
    suspend fun getTotalCount(): Int

    @Insert
    suspend fun insert(entity: UserBookmark)

    @Update(entity = UserBookmark::class)
    suspend fun update(updateData: UserBookmarkUpdate)

    @Query("DELETE FROM UserBookmark WHERE id IN (:entityIds)")
    suspend fun delete(entityIds: List<Int>)
}