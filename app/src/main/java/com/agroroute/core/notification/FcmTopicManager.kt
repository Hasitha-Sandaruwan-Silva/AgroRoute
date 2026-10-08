package com.agroroute.core.notification

import android.util.Log
import com.agroroute.core.model.UserRole
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscribes the device to per-user / per-role FCM topics.
 *
 * Topics avoid writing FCM tokens into Firestore (rules do not allow a token field).
 * Server-side senders (Cloud Functions / Console) can target `user_{uid}` or `role_farmer` /
 * `role_supplier` without client rule changes.
 */
@Singleton
class FcmTopicManager @Inject constructor() {

    private var subscribedUserTopic: String? = null
    private var subscribedRoleTopic: String? = null

    suspend fun syncForUser(userId: String, role: UserRole) {
        val userTopic = userTopic(userId)
        val roleTopic = roleTopic(role)
        if (subscribedUserTopic != userTopic) {
            subscribedUserTopic?.let { unsubscribeQuietly(it) }
            subscribeQuietly(userTopic)
            subscribedUserTopic = userTopic
        }
        if (subscribedRoleTopic != roleTopic) {
            subscribedRoleTopic?.let { unsubscribeQuietly(it) }
            subscribeQuietly(roleTopic)
            subscribedRoleTopic = roleTopic
        }
    }

    suspend fun clear() {
        subscribedUserTopic?.let { unsubscribeQuietly(it) }
        subscribedRoleTopic?.let { unsubscribeQuietly(it) }
        subscribedUserTopic = null
        subscribedRoleTopic = null
    }

    suspend fun currentTokenOrNull(): String? = try {
        FirebaseMessaging.getInstance().token.await()
    } catch (error: Exception) {
        Log.w(TAG, "Unable to fetch FCM token", error)
        null
    }

    private suspend fun subscribeQuietly(topic: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to $topic")
        } catch (error: Exception) {
            Log.w(TAG, "Failed to subscribe to $topic", error)
        }
    }

    private suspend fun unsubscribeQuietly(topic: String) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
        } catch (error: Exception) {
            Log.w(TAG, "Failed to unsubscribe from $topic", error)
        }
    }

    companion object {
        private const val TAG = "FcmTopicManager"

        fun userTopic(userId: String): String = "user_${sanitize(userId)}"
        fun roleTopic(role: UserRole): String = when (role) {
            UserRole.FARMER -> "role_farmer"
            UserRole.SUPPLIER -> "role_supplier"
        }

        private fun sanitize(value: String): String =
            value.replace(Regex("[^A-Za-z0-9-_.~%]"), "_")
    }
}
