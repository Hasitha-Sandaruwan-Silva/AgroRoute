package com.agroroute.common.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around [FirebaseStorage] that provides suspend-based image upload and download URL retrieval.
 */
@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage
) {

    /**
     * Uploads the file at [localUri] to the given [storagePath].
     * Returns the public download URL string on success.
     * Throws on failure — wrap with [safeCall] in repository.
     *
     * Example path: "listing_images/{userId}/{uuid}.jpg"
     */
    suspend fun uploadImage(localUri: Uri, storagePath: String): String {
        val ref = storage.reference.child(storagePath)
        ref.putFile(localUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Deletes the file at [storagePath] from Firebase Storage.
     * No-op if the file does not exist (catches StorageException silently).
     */
    suspend fun deleteImage(storagePath: String) {
        try {
            storage.reference.child(storagePath).delete().await()
        } catch (e: Exception) {
            // Silently ignore — file may already be deleted or path may be invalid.
        }
    }

    /**
     * Returns the download URL for an existing file at [storagePath], or null if not found.
     */
    suspend fun getDownloadUrl(storagePath: String): String? = try {
        storage.reference.child(storagePath).downloadUrl.await().toString()
    } catch (e: Exception) {
        null
    }
}
