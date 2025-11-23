package com.aktarjabed.inbusiness.data.local.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :userId")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("DELETE FROM users WHERE uid = :userId")
    suspend fun deleteUser(userId: String)

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE uid = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE provider = :provider")
    suspend fun getUsersByProvider(provider: String): List<UserEntity>
}