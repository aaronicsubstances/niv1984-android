package com.aaronicsubstances.niv1984.persistence

import androidx.room.TypeConverter
import java.sql.Timestamp
import java.util.*

object UuidTypeConverter {

    @JvmStatic
    @TypeConverter
    fun toUUID(value: String): UUID {
        return UUID.fromString(value)
    }

    @JvmStatic
    @TypeConverter
    fun toString(value: UUID): String {
        return value.toString()
    }
}

object TimestampTypeConverter {

    @JvmStatic
    @TypeConverter
    fun toTimestamp(value: Long): Timestamp {
        return Timestamp(value)
    }

    @JvmStatic
    @TypeConverter
    fun toLong(value: Timestamp): Long {
        return value.time
    }
}

object DateTypeConverter {

    @JvmStatic
    @TypeConverter
    fun toDate(value: Long): Date {
        return Date(value)
    }

    @JvmStatic
    @TypeConverter
    fun toLong(value: Date): Long {
        return value.time
    }
}
