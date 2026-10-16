package com.agroroute

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.remote.CloudinaryPhotoStorage
import com.agroroute.core.remote.ProfilePhotoStorage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Uses a generated QA image; does not change any account or listing. */
class ProfilePhotoUploadTest {
    @Test fun preparedAvatarUploadsThroughCloudinaryForBothRoles() = runBlocking {
        assumeTrue("Opt in to real QA uploads with live_profile_photo_upload=true",
            InstrumentationRegistry.getArguments().getString("live_profile_photo_upload") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = ProfilePhotoStorage(CloudinaryPhotoStorage(context), context)
        val source = File.createTempFile("profile-qa-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        try {
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            for (role in listOf("farmer", "supplier")) {
                val url = storage.upload(Uri.fromFile(source), "qa-profile-$role")
                assertTrue(url.startsWith("https://res.cloudinary.com/"))
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                try {
                    assertEquals(200, connection.responseCode)
                    assertTrue(connection.contentType.startsWith("image/"))
                } finally { connection.disconnect() }
            }
            assertFalse(context.cacheDir.listFiles().orEmpty().any { it.name.startsWith("avatar-") })
        } finally { bitmap.recycle(); source.delete() }
    }

    @Test fun invalidImageFailsBeforeUpload() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("invalid-profile-", ".jpg", context.cacheDir)
        try {
            file.writeText("not an image")
            try {
                ProfilePhotoStorage(CloudinaryPhotoStorage(context), context).upload(Uri.fromFile(file), "qa")
                fail("Invalid image must be rejected")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("Cannot read"))
            }
        } finally { file.delete() }
    }
    /** Reuploads the existing photo (or a QA image if absent), then restores the original URL. */
    @Test fun signedInProfileSaveAndDiskCacheRoundTrip(): Unit = runBlocking {
        assumeTrue("Opt in to reversible account verification",
            InstrumentationRegistry.getArguments().getString("live_profile_photo_account") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        assumeTrue("A real account must already be signed in", auth.currentUser != null)
        val uid = requireNotNull(auth.currentUser).uid
        val remote = com.agroroute.core.remote.FirestoreService(com.google.firebase.firestore.FirebaseFirestore.getInstance())
        val original = requireNotNull(remote.getOnce<com.agroroute.core.model.UserProfile>("users", uid))
        val source = File.createTempFile("profile-roundtrip-", ".jpg", context.cacheDir)
        var db = com.agroroute.core.di.DatabaseModule.provideAppDatabase(context)
        fun repository() = com.agroroute.core.repository.AuthRepositoryImpl(
            com.agroroute.core.remote.AuthService(auth), remote, db.userProfileDao(),
            com.agroroute.core.notification.NotificationService(context))
        var changed = false
        try {
            if (!original.profileImageUrl.isNullOrBlank()) {
                val connection = URL(original.profileImageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 30000; connection.readTimeout = 30000
                try { connection.inputStream.use { input -> source.outputStream().use { input.copyTo(it) } } }
                finally { connection.disconnect() }
            } else {
                val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                try { source.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, 88, it) } }
                finally { image.recycle() }
            }
            val url = ProfilePhotoStorage(CloudinaryPhotoStorage(context), context).upload(Uri.fromFile(source), uid)
            val edited = original.copy(profileImageUrl = url)
            changed = true
            val result = repository().updateProfile(edited)
            assertTrue("Real profile save must succeed: $result", result is com.agroroute.core.util.Result.Success)
            val server = com.google.android.gms.tasks.Tasks.await(
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get(com.google.firebase.firestore.Source.SERVER), 30, java.util.concurrent.TimeUnit.SECONDS)
            assertEquals(url, server.getString("profileImageUrl"))
            assertEquals(url, db.userProfileDao().getUserProfile(uid).first()?.profileImageUrl)
            assertEquals(url, com.agroroute.features.participants.ParticipantRepositoryImpl(
                com.google.firebase.firestore.FirebaseFirestore.getInstance()).identity(uid).imageUrl)
            db.close()
            db = com.agroroute.core.di.DatabaseModule.provideAppDatabase(context)
            assertEquals(url, db.userProfileDao().getUserProfile(uid).first()?.profileImageUrl)
            android.util.Log.i("ProfilePhotoQA", "Live ${original.role} upload, Firestore, participant identity and reopened Room verified")
        } finally {
            try {
                if (changed) assertTrue("Restore original avatar URL", repository().updateProfile(original) is com.agroroute.core.util.Result.Success)
            } finally { db.close(); source.delete() }
        }
    }

}

