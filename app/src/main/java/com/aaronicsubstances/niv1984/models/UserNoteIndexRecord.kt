package com.aaronicsubstances.niv1984.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "user_note_index_record")
@Fts4(contentEntity = UserNote::class)
data class UserNoteIndexRecord(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    var rowId: Int,

    var content: String,

    var title: String
)