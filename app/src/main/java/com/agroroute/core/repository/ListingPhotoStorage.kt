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
        Stage.UPLOAD ->
            "Photo upload failed: ${cause.message ?: "The photo could not be uploaded. Check your connection and try again."}"
        Stage.DOWNLOAD_URL ->
            "Photo uploaded, but its URL could not be confirmed: ${cause.message ?: "The upload service did not return a photo URL."}"
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
    require(farmerId.isNotBlank() && '/' !in farmerId) { "A valid Farmer UID is required" }
    require(listingId.isNotBlank() && '/' !in listingId) { "A valid listing ID is required" }
    return "listing_images/$farmerId/$listingId/photo"
}