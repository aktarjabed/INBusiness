package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)