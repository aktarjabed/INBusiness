package com.aktarjabed.inbusiness.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aktarjabed.inbusiness.domain.model.User

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
) {
    companion object {
        fun fromDomain(user: User): UserEntity {
            return UserEntity(
                uid = user.id,
                email = user.email,
                phone = user.phoneNumber,
                displayName = user.displayName,
                photoUrl = user.photoUrl,
                provider = user.provider.name,
                isEmailVerified = user.isEmailVerified,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
        }
    }

    fun toDomain(): User {
        return User(
            id = uid,
            email = email,
            phoneNumber = phone,
            displayName = displayName,
            photoUrl = photoUrl,
            provider = when (provider) {
                "GOOGLE" -> User.Provider.GOOGLE
                "MICROSOFT" -> User.Provider.MICROSOFT
                "PHONE" -> User.Provider.PHONE
                "EMAIL" -> User.Provider.EMAIL
                else -> User.Provider.EMAIL
            },
            isEmailVerified = isEmailVerified
        )
    }
}