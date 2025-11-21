package com.aktarjabed.inbusiness.domain.model

data class User(
    val id: String,
    val email: String?,
    val phoneNumber: String?,
    val displayName: String?,
    val photoUrl: String?,
    val provider: Provider,
    val isEmailVerified: Boolean = false
) {
    enum class Provider {
        GOOGLE,
        MICROSOFT,
        PHONE,
        EMAIL
    }
}