package com.agroroute.core.remote

import android.net.Uri
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.repository.ListingPhotoStorageException
import com.agroroute.core.repository.listingPhotoStoragePath
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around [FirebaseStorage] that provides suspend-based image upload and download URL retrieval.
 */
@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : ListingPhotoStorage {

    override suspend fun uploadListingPhoto(
        localUri: String,
        farmerId: String,
        listingId: String
    ): String = uploadImage(Uri.parse(localUri), listingPhotoStoragePath(farmerId, listingId))

    /**
     * Uploads the file at [localUri] to the given [storagePath].
     * Returns the public download URL string on success.
     * Throws on failure — wrap with [safeCall] in repository.
     *
     * Example path: "listing_images/{userId}/{uuid}.jpg"
     */
    suspend fun uploadImage(localUri: Uri, storagePath: String): String {
        val ref = storage.reference.child(storagePath)
        val contentType = (context.contentResolver.getType(localUri) ?: if (localUri.scheme == "file") android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(localUri.lastPathSegment?.substringAfterLast(".")?.lowercase()) else null)
            ?.takeIf { it.startsWith("image/") }
            ?: error("Unable to determine the selected image type")
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()
        try {
            ref.putFile(localUri, metadata).await()
        } catch (error: Exception) {
            throw ListingPhotoStorageException(ListingPhotoStorageException.Stage.UPLOAD, error)
        }
        return try {
            ref.downloadUrl.await().toString()
        } catch (error: Exception) {
            throw ListingPhotoStorageException(ListingPhotoStorageException.Stage.DOWNLOAD_URL, error)
        }
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
