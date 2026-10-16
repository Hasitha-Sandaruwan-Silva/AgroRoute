package com.agroroute.features.farmer.listing

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.repository.ListingPhotoStorage

internal const val MAX_LISTING_PHOTOS = 5

internal fun appendListingPhoto(photos: List<String>, photo: String): List<String> =
    if (photos.size >= MAX_LISTING_PHOTOS || photo in photos) photos else photos + photo

internal fun primaryListingPhoto(photos: List<String>, index: Int): List<String> =
    if (index !in photos.indices) photos else listOf(photos[index]) + photos.filterIndexed { i, _ -> i != index }

internal suspend fun uploadListingPhotos(listing: HarvestListing, storage: ListingPhotoStorage): List<String> {
    require(listing.imageUrls.size <= MAX_LISTING_PHOTOS) { "Maximum 5 photos" }
    return listing.imageUrls.map { uri ->
        val url = if (uri.startsWith("content://") || uri.startsWith("file://"))
            storage.uploadListingPhoto(uri, listing.farmerId, listing.listingId) else uri
        require(url.startsWith("https://")) { "Photo upload did not return a secure image URL" }
        url
    }
}
