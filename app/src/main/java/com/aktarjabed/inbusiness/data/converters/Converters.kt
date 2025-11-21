package com.aktarjabed.inbusiness.data.converters

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(",")

    @TypeConverter
<<<<<<< HEAD
    fun toStringList(value: String?): List<String>? =
=======
    fun toStringList(value: String?): List<String>? =
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
        value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
}