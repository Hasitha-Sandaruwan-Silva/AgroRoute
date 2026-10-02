package com.agroroute.core.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around [FirebaseAuth] that exposes suspend-based sign-in, registration, and sign-out.
 */
@Singleton
class AuthService @Inject constructor(
    private val auth: FirebaseAuth
) {

    /** The currently signed-in Firebase user, or null if no session exists. */
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** The UID of the signed-in user, or null. */
    val currentUserId: String? get() = auth.currentUser?.uid

    /** Returns true if a user is currently authenticated. */
    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun authStateChanges(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Signs in with email and password. Returns the [FirebaseUser] on success.
     * Throws [FirebaseAuthException] on failure — wrap with [safeCall] in repository.
     */
    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: error("Sign-in succeeded but user is null")
    }

    /** Signs in using a Google ID token without retaining the token. */
    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: error("Google sign-in succeeded but user is null")
    }

    /**
     * Creates a new account with email and password. Returns the new [FirebaseUser].
     * Throws on failure — wrap with [safeCall] in repository.
     */
    suspend fun register(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user ?: error("Registration succeeded but user is null")
    }

    /** Signs out the current user. */
    fun signOut() = auth.signOut()

    /**
     * Sends a password-reset email to [email].
     */
    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }
}
