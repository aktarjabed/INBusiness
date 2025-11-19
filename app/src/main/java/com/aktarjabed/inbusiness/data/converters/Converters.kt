package com.aktarjabed.inbusiness.data.converters

import androidx.room.TypeConverter
import com.aktarjabed.inbusiness.data.entities.GstType
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun fromGstType(value: GstType): String {
        return value.name
    }

    @TypeConverter
    fun toGstType(value: String): GstType {
        return try {
            GstType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            GstType.SGST_CGST // safe default
        }
    }
}