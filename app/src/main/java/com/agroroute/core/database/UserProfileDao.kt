package com.agroroute.core.database

import androidx.room.*
import com.agroroute.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * DAO for reading and writing the locally cached [UserProfile].
 */
@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getCurrentUser(): Flow<UserProfile?>

    @Upsert
    suspend fun upsertUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteUserProfile(userId: String)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAll()
}
