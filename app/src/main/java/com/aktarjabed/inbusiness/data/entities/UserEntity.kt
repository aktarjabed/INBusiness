package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String?,
    val phone: String?,
    val displayName: String?,
    val photoUrl: String?,
    val provider: String,
    val isEmailVerified: Boolean,
    val createdAt: Long,
    val lastLoginAt: Long
)