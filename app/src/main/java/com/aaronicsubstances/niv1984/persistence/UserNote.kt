package com.aaronicsubstances.niv1984.persistence

import androidx.room.*
import java.sql.Timestamp

@Entity(tableName = "user_note")
class UserNote(

    @PrimaryKey(autoGenerate = true)
    var id: Int,

    var title: String,

    var content: String,

    @ColumnInfo(name = "date_created")
    var dateCreated: Timestamp,

    @ColumnInfo(name = "date_updated")
    var dateUpdated: Timestamp
)

@Dao
interface UserNoteDao {
    @Query("SELECT * from user_note ORDER BY date_updated DESC")
    suspend fun getNotes(): List<UserNote>
}