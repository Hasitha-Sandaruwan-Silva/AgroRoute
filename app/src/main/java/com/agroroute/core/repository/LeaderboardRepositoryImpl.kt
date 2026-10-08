package com.agroroute.core.repository

import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import com.agroroute.core.util.Result
import com.agroroute.core.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [LeaderboardRepository] using local Room data for performance.
 */
@Singleton
class LeaderboardRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val firestoreService: FirestoreService,
    private val authRepository: AuthRepository
) : LeaderboardRepository {

    override fun getFarmerLeaderboard(): Flow<Result<List<UserProfile>>> =
        userProfileDao.getUsersByRoleSortedByPoints(Constants.ROLE_FARMER)
            .map<List<UserProfile>, Result<List<UserProfile>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load farmer leaderboard", e)) }

    override fun getSupplierLeaderboard(): Flow<Result<List<UserProfile>>> =
        userProfileDao.getUsersByRoleSortedByPoints(Constants.ROLE_SUPPLIER)
            .map<List<UserProfile>, Result<List<UserProfile>>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load supplier leaderboard", e)) }

    override suspend fun getFarmerRank(userId: String): Result<Int?> = safeCall {
        val leaderboard = getFarmerLeaderboard().first()
        when (leaderboard) {
            is Result.Success -> {
                val rank = leaderboard.data.indexOfFirst { it.userId == userId }
                if (rank >= 0) rank + 1 else null
            }
            is Result.Error -> throw Exception(leaderboard.message ?: "Failed to compute farmer rank")
            is Result.Loading -> null
        }
    }

    override suspend fun getSupplierRank(userId: String): Result<Int?> = safeCall {
        val leaderboard = getSupplierLeaderboard().first()
        when (leaderboard) {
            is Result.Success -> {
                val rank = leaderboard.data.indexOfFirst { it.userId == userId }
                if (rank >= 0) rank + 1 else null
            }
            is Result.Error -> throw Exception(leaderboard.message ?: "Failed to compute supplier rank")
            is Result.Loading -> null
        }
    }

    override suspend fun syncLeaderboard(): Result<Unit> = safeCall {
        val userId = authRepository.currentUserId ?: error("Sign in to sync leaderboard")
        // Sync all users to get updated points
        val allUsers = firestoreService.queryOnceFromServer(
            Constants.COLLECTION_USERS,
            null,
            null,
            UserProfile::class.java
        )
        allUsers.forEach { userProfileDao.upsertUserProfile(it) }
    }
}
