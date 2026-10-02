package com.agroroute.core.repository

interface ListingPhotoStorage {
    suspend fun uploadListingPhoto(localUri: String, farmerId: String, listingId: String): String
}

class ListingPhotoStorageException(
    val stage: Stage,
    cause: Throwable
) : Exception(
    when (stage) {
        Stage.UPLOAD -> "Photo upload failed: ${cause.message ?: "Firebase Storage rejected the file."}"
        Stage.DOWNLOAD_URL -> "Photo uploaded, but its download URL could not be retrieved: ${cause.message ?: "Firebase Storage did not return a URL."}"
    },
    cause
) {
    enum class Stage { UPLOAD, DOWNLOAD_URL }
}

fun listingPhotoStoragePath(farmerId: String, listingId: String): String {
    require(farmerId.isNotBlank() && '/' !in farmerId) { "A valid Farmer UID is required" }
    require(listingId.isNotBlank() && '/' !in listingId) { "A valid listing ID is required" }
    return "listing_images/$farmerId/$listingId/photo"
}