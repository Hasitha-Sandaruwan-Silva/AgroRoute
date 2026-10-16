package com.agroroute.core.repository

/**
 * Repository interface for listing-photo storage.
 *
 * Implemented by [com.agroroute.core.remote.CloudinaryPhotoStorage], which uploads
 * to Cloudinary using an unsigned preset (no API secret in the app).
 *
 * The [farmerId] and [listingId] parameters are used to derive a scoped storage
 * path/public_id.  They must be non-blank and must not contain `/` so they
 * can be safely composed into a path segment.
 */
interface ListingPhotoStorage {
    /**
     * Uploads the image at [localUri] (an Android `content://` or `file://` URI)
     * and returns the remote `https://` URL for the uploaded photo.
     *
     * Throws [ListingPhotoStorageException] on any failure — the caller must not
     * save a listing whose [imageUrls] contains a local URI or placeholder.
     */
    suspend fun uploadListingPhoto(localUri: String, farmerId: String, listingId: String): String
}

/**
 * Thrown by [ListingPhotoStorage] implementations to carry a user-facing message
 * and the original cause.  The [stage] allows callers to distinguish between a
 * network/upload failure and a response-parsing failure.
 */
class ListingPhotoStorageException(
    val stage: Stage,
    cause: Throwable
) : Exception(
    when (stage) {
        Stage.UPLOAD -> com.agroroute.core.util.photoUploadMessage(cause)
        Stage.DOWNLOAD_URL -> "Couldn't process the photo. Please try again."
    },
    cause
) {
    enum class Stage { UPLOAD, DOWNLOAD_URL }
}

/**
 * Derives the scoped storage path / Cloudinary public_id for a listing photo.
 * Used by [ListingPhotoStorage] implementations and in tests to assert path correctness.
 */
fun listingPhotoStoragePath(farmerId: String, listingId: String): String {
    require(farmerId.isNotBlank() && '/' !in farmerId) { "Sign in as a Farmer to upload photos" }
    require(listingId.isNotBlank() && '/' !in listingId) { "Choose a listing before adding photos" }
    return "listing_images/$farmerId/$listingId/photo"
}