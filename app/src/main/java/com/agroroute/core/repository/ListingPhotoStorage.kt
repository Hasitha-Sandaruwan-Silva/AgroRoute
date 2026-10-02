package com.agroroute.core.repository

interface ListingPhotoStorage {
    suspend fun uploadListingPhoto(localUri: String, farmerId: String, listingId: String): String
}

fun listingPhotoStoragePath(farmerId: String, listingId: String): String {
    require(farmerId.isNotBlank() && '/' !in farmerId) { "A valid Farmer UID is required" }
    require(listingId.isNotBlank() && '/' !in listingId) { "A valid listing ID is required" }
    return "listing_images/$farmerId/$listingId/photo"
}