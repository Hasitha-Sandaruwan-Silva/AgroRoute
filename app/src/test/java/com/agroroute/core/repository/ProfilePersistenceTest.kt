package com.agroroute.core.repository

import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.remote.AuthService
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.util.Result
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePersistenceTest {
    @Test fun `farmer and supplier phone location and photo persist without changing trusted points`() = runTest {
        for (role in UserRole.entries) {
            val original = UserProfile(userId = "owner", role = role, points = 8)
            val auth = mockk<AuthService>()
            every { auth.currentUserId } returns "owner"
            val firestore = mockk<FirebaseFirestore>(relaxed = true)
            val snapshot = mockk<DocumentSnapshot>()
            every { firestore.collection("users").document("owner").get() } returns Tasks.forResult(snapshot)
            every { snapshot.toObject(UserProfile::class.java) } returns original
            val remote = FirestoreService(firestore)
            val dao = mockk<UserProfileDao>(relaxed = true)
            val repository = AuthRepositoryImpl(auth, remote, dao, mockk<NotificationService>(relaxed = true))
            val location = "Dummy farm\n[7.000000, 80.000000]"
            val edited = original.copy(displayName = "Dummy owner", phone = "+94000000001", location = location,
                profileImageUrl = "https://example.invalid/avatar.png", points = 900)
            every { firestore.collection("users").document("owner").update(any<Map<String, Any?>>()) } returns Tasks.forResult(null)
            assertTrue(repository.updateProfile(edited) is Result.Success)
            verify { firestore.collection("users").document("owner").update(match<Map<String, Any?>> {
                it["phone"] == edited.phone && it["location"] == location && it["profileImageUrl"] == edited.profileImageUrl &&
                    it.keys == setOf("displayName", "phone", "location", "profileImageUrl")
            }) }
            coVerify { dao.upsertUserProfile(match { it.phone == edited.phone && it.location == location && it.points == 8 && it.role == role && it.profileImageUrl == edited.profileImageUrl }) }
        }
    }
    @Test fun `editing another users profile is rejected before network writes`() = runTest {
        val auth = mockk<AuthService>()
        every { auth.currentUserId } returns "owner"
        val remote = mockk<FirestoreService>()
        val repository = AuthRepositoryImpl(auth, remote, mockk(relaxed = true), mockk(relaxed = true))
        assertTrue(repository.updateProfile(UserProfile(userId = "stranger")) is Result.Error)
        coVerify(exactly = 0) { remote.updateFields(any(), any(), any()) }
    }
    @Test fun `failed profile document update preserves cached avatar`() = runTest {
        val auth = mockk<AuthService>()
        every { auth.currentUserId } returns "owner"
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>()
        val original = UserProfile(userId = "owner", profileImageUrl = "https://example.invalid/old.jpg")
        every { snapshot.toObject(UserProfile::class.java) } returns original
        every { firestore.collection("users").document("owner").get() } returns Tasks.forResult(snapshot)
        every { firestore.collection("users").document("owner").update(any<Map<String, Any?>>()) } returns
            Tasks.forException(IllegalStateException("profile save rejected"))
        val dao = mockk<UserProfileDao>(relaxed = true)
        val repository = AuthRepositoryImpl(auth, FirestoreService(firestore), dao, mockk(relaxed = true))
        val result = repository.updateProfile(original.copy(profileImageUrl = "https://example.invalid/new.jpg"))
        assertTrue(result is Result.Error && result.cause?.message == "profile save rejected")
        coVerify(exactly = 0) { dao.upsertUserProfile(any()) }
    }

}
