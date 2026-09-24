package com.agroroute.core.repository

import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.AuthService
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import com.agroroute.core.util.Result
import com.agroroute.core.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [AuthRepository] coordinating Firebase Auth, Firestore user documents, and local Room cache.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val firestoreService: FirestoreService,
    private val userProfileDao: UserProfileDao
) : AuthRepository {

    override val isLoggedIn: Boolean get() = authService.isLoggedIn
    override val currentUserId: String? get() = authService.currentUserId

    override fun getCurrentUserProfile(): Flow<Result<UserProfile?>> =
        userProfileDao.getCurrentUser()
            .map<UserProfile?, Result<UserProfile?>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load profile", e)) }

    override suspend fun login(email: String, password: String): Result<UserProfile> = safeCall {
        val firebaseUser = authService.signIn(email, password)
        // Fetch profile from Firestore and cache locally
        val profile = firestoreService.getOnce<UserProfile>(
            Constants.COLLECTION_USERS, firebaseUser.uid
        ) ?: error("User profile not found in Firestore for uid: ${firebaseUser.uid}")
        userProfileDao.upsertUserProfile(profile)
        profile
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<UserProfile> = safeCall {
        val firebaseUser = authService.register(email, password)
        val profile = UserProfile(
            userId      = firebaseUser.uid,
            displayName = displayName,
            email       = email,
            role        = role
        )
        // Persist to Firestore and local cache simultaneously
        firestoreService.set(Constants.COLLECTION_USERS, firebaseUser.uid, profile)
        userProfileDao.upsertUserProfile(profile)
        profile
    }

    override suspend fun logout(): Result<Unit> = safeCall {
        authService.signOut()
        userProfileDao.deleteAll()
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> = safeCall {
        firestoreService.set(Constants.COLLECTION_USERS, profile.userId, profile)
        userProfileDao.upsertUserProfile(profile)
    }
}
