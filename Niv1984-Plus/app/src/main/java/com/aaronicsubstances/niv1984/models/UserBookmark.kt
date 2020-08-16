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

    val title: String,

    val dateUpdated: Timestamp
)

@Dao
interface UserBookmarkDao {

    @Query(
        """SELECT * from UserBookmark
        WHERE dateUpdated < :maxDateUpdated OR (dateUpdated = :maxDateUpdated AND id > :minId)
        ORDER BY dateUpdated DESC, id
        LIMIT :pageSize"""
    )
    suspend fun getNextPageSortedByDate(
        maxDateUpdated: Timestamp, minId: Int,
        pageSize: Int
    ): List<UserBookmark>

    @Query(
        """SELECT * from UserBookmark
        WHERE title > :beginTitle OR (title = :beginTitle AND id > :minId)
        ORDER BY title, id
        LIMIT :pageSize"""
    )
    suspend fun getNextPageSortedByTitle(
        beginTitle: String, minId: Int,
        pageSize: Int
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