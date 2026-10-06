package com.agroroute.core.repository

import com.agroroute.core.model.UserProfile
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for leaderboard operations.
 */
interface LeaderboardRepository {

    /** Get farmers ranked by points (descending). */
    fun getFarmerLeaderboard(): Flow<Result<List<UserProfile>>>

    /** Get suppliers ranked by points (descending). */
    fun getSupplierLeaderboard(): Flow<Result<List<UserProfile>>>

    /** Get user's current rank among farmers. */
    suspend fun getFarmerRank(userId: String): Result<Int?>

    /** Get user's current rank among suppliers. */
    suspend fun getSupplierRank(userId: String): Result<Int?>

    /** Sync leaderboard data from Firestore to Room. */
    suspend fun syncLeaderboard(): Result<Unit>
}
