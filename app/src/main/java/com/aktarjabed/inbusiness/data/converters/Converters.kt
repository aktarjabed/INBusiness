package com.aktarjabed.inbusiness.data.converters

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Room type converters for the persisted model.
 *
 * Only converters that are actually used by an entity are kept here. Earlier revisions
 * also shipped a comma-joined `List<String>` converter; no entity used it and the
 * encoding was ambiguous for values containing commas, so it was removed rather than
 * left as a trap for future columns.
 */
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
