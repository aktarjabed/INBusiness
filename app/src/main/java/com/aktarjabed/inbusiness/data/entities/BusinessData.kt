package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
<<<<<<< HEAD
import java.time.LocalDateTime

@Entity(tableName = "business_data")
data class BusinessData(
    @PrimaryKey val id: String,
    val userId: String,
    val businessName: String,
    val gstin: String?,
    val address: String,
    val phone: String?,
    val email: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
=======

@Entity(tableName = "business_data")
data class BusinessData(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val gstin: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val phoneNumber: String = "",
    val email: String = ""
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
)