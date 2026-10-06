package com.agroroute.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a registered user (farmer or supplier) — persisted locally and in Firestore.
 */
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.FARMER,
    val profileImageUrl: String? = null,
    val location: String = "",
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
