package com.agroroute.core.repository

import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.remote.AuthService
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Constants
import com.agroroute.core.util.Result
import com.agroroute.core.util.safeCall
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val userProfileDao: UserProfileDao,
    private val notificationService: NotificationService
) : AuthRepository {

    override val isLoggedIn: Boolean get() = authService.isLoggedIn
    override val currentUserId: String? get() = authService.currentUserId

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentUserProfile(): Flow<Result<UserProfile?>> =
        authService.authStateChanges()
            .flatMapLatest { userId ->
                if (userId.isNullOrBlank()) flowOf(null) else userProfileDao.getUserProfile(userId)
            }
            .map<UserProfile?, Result<UserProfile?>> { Result.Success(it) }
            .catch { e -> emit(Result.Error(e.message ?: "Failed to load profile", e)) }

    override suspend fun restoreSession(): Result<UserProfile?> {
        val userId = authService.currentUserId ?: return Result.Success(null)
        val cached = userProfileDao.getUserProfile(userId).first()
        return try {
            val remote = firestoreService.getOnce<UserProfile>(Constants.COLLECTION_USERS, userId)
                ?: return Result.Error("No account profile is associated with this sign-in")
            if (remote.userId != userId) return Result.Error("Account profile does not match the signed-in user")
            userProfileDao.upsertUserProfile(remote)
            Result.Success(remote)
        } catch (error: Exception) {
            if (cached != null && error.isNetworkFailure()) Result.Success(cached)
            else Result.Error(error.toUserMessage(), error)
        }
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> =
        mapFirebaseFailure(safeCall {
            val firebaseUser = authService.signIn(email, password)
            val restored = restoreSession().getOrThrow()
            restored ?: error("No account profile is associated with this sign-in (${firebaseUser.uid})")
        })

    override suspend fun signInWithGoogle(idToken: String): Result<GoogleSignInOutcome> =
        mapGoogleSignInFailure(safeCall {
            val firebaseUser = authService.signInWithGoogle(idToken)
            val snapshot = firestoreService.getDocumentOnce(Constants.COLLECTION_USERS, firebaseUser.uid)
            val savedRole = snapshot.getString("role")?.let { roleName ->
                runCatching { UserRole.valueOf(roleName) }.getOrNull()
            }
            val profile = if (snapshot.exists() && savedRole != null) {
                val savedProfile = snapshot.toObject(UserProfile::class.java)
                    ?.copy(userId = firebaseUser.uid, role = savedRole)
                    ?: error("Could not read the saved account profile")
                userProfileDao.upsertUserProfile(savedProfile)
                savedProfile
            } else {
                null
            }
            googleSignInOutcome(profile)
        })

    override suspend fun completeGoogleSignIn(role: UserRole): Result<UserProfile> =
        mapFirebaseFailure(safeCall {
            val firebaseUser = authService.currentUser ?: error("Sign in with Google to continue")
            val snapshot = firestoreService.getDocumentOnce(Constants.COLLECTION_USERS, firebaseUser.uid)
            val savedRole = snapshot.getString("role")?.let { roleName ->
                runCatching { UserRole.valueOf(roleName) }.getOrNull()
            }
            val profile = if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java)
                    ?: error("Could not read the saved account profile")
                existing.copy(userId = firebaseUser.uid, role = savedRole ?: role)
            } else {
                UserProfile(
                    userId = firebaseUser.uid,
                    displayName = firebaseUser.displayName.orEmpty(),
                    email = firebaseUser.email.orEmpty(),
                    role = role,
                    profileImageUrl = firebaseUser.photoUrl?.toString()
                )
            }
            firestoreService.set(Constants.COLLECTION_USERS, firebaseUser.uid, profile)
            userProfileDao.upsertUserProfile(profile)
            profile
        })

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<UserProfile> = mapFirebaseFailure(safeCall {
        val firebaseUser = authService.register(email, password)
        val profile = UserProfile(
            userId = firebaseUser.uid,
            displayName = displayName,
            email = email,
            role = role
        )
        firestoreService.set(Constants.COLLECTION_USERS, firebaseUser.uid, profile)
        userProfileDao.upsertUserProfile(profile)
        profile
    })

    override suspend fun logout(): Result<Unit> = mapFirebaseFailure(safeCall {
        val userId = authService.currentUserId
        if (userId != null) notificationService.unregisterDeviceToken(userId)
        authService.signOut()
        if (userId != null) userProfileDao.deleteUserProfile(userId)
    })

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> = mapFirebaseFailure(safeCall {
        val userId = authService.currentUserId ?: error("Sign in to update your profile")
        check(profile.userId == userId) { "You can only update your own profile" }
        val existing = firestoreService.getOnce<UserProfile>(Constants.COLLECTION_USERS, userId)
            ?: error("User profile not found")
        check(profile.role == existing.role) { "Account role cannot be changed" }
        firestoreService.set(Constants.COLLECTION_USERS, profile.userId, profile)
        userProfileDao.upsertUserProfile(profile)
    })

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = mapFirebaseFailure(safeCall {
        authService.sendPasswordResetEmail(email)
    })

    private fun <T> mapFirebaseFailure(result: Result<T>): Result<T> = when (result) {
        is Result.Error -> Result.Error(result.cause?.toUserMessage() ?: result.message, result.cause)
        else -> result
    }

    private fun <T> mapGoogleSignInFailure(result: Result<T>): Result<T> = when (result) {
        is Result.Error -> Result.Error(
            result.cause?.toGoogleSignInMessage() ?: result.message,
            result.cause
        )
        else -> result
    }

    private fun Throwable.toGoogleSignInMessage(): String = when ((this as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_CREDENTIAL" -> "Google sign-in could not be verified. Please try again."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            "An account already exists with a different sign-in method. Sign in with that method first."
        else -> toUserMessage()
    }

    private fun Throwable.isNetworkFailure(): Boolean =
        this is IOException ||
            (this is FirebaseFirestoreException && code in setOf(
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
            ))

    private fun Throwable.toUserMessage(): String = when ((this as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
        "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
            "Email or password is incorrect."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists for this email."
        "ERROR_WEAK_PASSWORD" -> "Choose a stronger password with at least 6 characters."
        "ERROR_USER_DISABLED" -> "This account has been disabled. Contact support."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a little and try again."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Email and password sign-in is not enabled for this Firebase project."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Network unavailable. Check your connection and try again."
        else -> when {
            isNetworkFailure() -> "Network unavailable. Check your connection and try again."
            this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "This account is not permitted to access that data."
            else -> message ?: "Firebase request failed. Please try again."
        }
    }
}
