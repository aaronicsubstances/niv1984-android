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
        """SELECT COUNT(*) FROM UserBookmark"""
    )
    suspend fun getTotalCount(): Int

    @Insert
    suspend fun insert(entity: UserBookmark)

    @Update(entity = UserBookmark::class)
    suspend fun update(updateData: UserBookmarkUpdate)

    @Delete
    suspend fun delete(entity: UserBookmark)
}