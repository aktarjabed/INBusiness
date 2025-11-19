package com.aktarjabed.inbusiness.data.database

import androidx.room.TypeConverter
import java.time.Instant

class TypeConverters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}