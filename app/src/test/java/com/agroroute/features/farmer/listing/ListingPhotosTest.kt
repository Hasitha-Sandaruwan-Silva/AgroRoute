package com.agroroute.features.farmer.listing

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.util.Result
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ListingPhotosTest {
    @Test fun cameraAndGalleryAppendAndSixthIsBlocked() {
        val photos = listOf("content://camera/1", "content://camera/2", "content://gallery/3", "content://gallery/4", "content://gallery/5")
        var selected = emptyList<String>()
        photos.forEach { selected = appendListingPhoto(selected, it) }
        assertEquals(photos, selected)
        assertEquals(photos, appendListingPhoto(selected, "content://gallery/6"))
        assertEquals(photos.take(1), appendListingPhoto(photos.take(1), photos.first()))
    }
    @Test fun coverCanChangeAndRemovalPromotesNextOrLeavesNoPhoto() {
        val photos = listOf("https://one", "https://two", "https://three")
        assertEquals(listOf("https://three", "https://one", "https://two"), primaryListingPhoto(photos, 2))
        assertEquals("https://two", photos.drop(1).first())
        assertTrue(photos.filter { false }.isEmpty())
    }
    @Test fun existingSingleAndMultipleRemotePhotosNeedNoUpload() = runBlocking {
        val storage = mockk<ListingPhotoStorage>()
        for (photos in listOf(emptyList(), listOf("https://old"), listOf("https://one", "https://two"))) {
            assertEquals(photos, uploadListingPhotos(HarvestListing(imageUrls = photos), storage))
        }
        coVerify(exactly = 0) { storage.uploadListingPhoto(any(), any(), any()) }
    }
    @Test fun everyLocalPhotoUploadsInCoverOrder() = runBlocking {
        val storage = mockk<ListingPhotoStorage>()
        coEvery { storage.uploadListingPhoto(any(), "farmer", "listing") } answers { "https://uploaded/" + firstArg<String>().substringAfterLast('/') }
        val listing = HarvestListing(listingId = "listing", farmerId = "farmer", imageUrls = listOf("content://camera/1", "https://old", "content://gallery/2"))
        assertEquals(listOf("https://uploaded/1", "https://old", "https://uploaded/2"), uploadListingPhotos(listing, storage))
        coVerify(exactly = 2) { storage.uploadListingPhoto(any(), "farmer", "listing") }
    }
    @Test fun failureLeavesOriginalSelectionAndNeverSavesListing() = runBlocking {
        val storage = mockk<ListingPhotoStorage>()
        val repository = mockk<HarvestListingRepository>()
        val auth = mockk<com.agroroute.core.repository.AuthRepository>()
        every { auth.currentUserId } returns "farmer"
        every { auth.getCurrentUserProfile() } returns kotlinx.coroutines.flow.flowOf(Result.Success(com.agroroute.core.model.UserProfile(userId = "farmer", role = com.agroroute.core.model.UserRole.FARMER)))
        coEvery { storage.uploadListingPhoto("content://camera/1", any(), any()) } returns "https://one"
        coEvery { storage.uploadListingPhoto("content://gallery/2", any(), any()) } throws IllegalStateException("offline")
        val photos = listOf("content://camera/1", "content://gallery/2")
        val listing = HarvestListing(listingId = "listing", farmerId = "farmer", cropName = "Rice", category = com.agroroute.core.model.ListingCategory.entries.first(), quantity = 10.0, location = "Farm\n[7.000000, 80.000000]", imageUrls = photos)
        val useCase = CreateListingUseCase(repository, storage, auth)
        assertTrue(useCase(listing) is Result.Error)
        assertEquals(photos, listing.imageUrls)
        coVerify(exactly = 0) { repository.saveListing(any()) }
    }
    @Test fun removalUsesCloseAndPropagatesNetworkFailure() = runBlocking {
        val repository = mockk<HarvestListingRepository>()
        val listing = HarvestListing(listingId = "listing")
        coEvery { repository.closeListing("listing") } returns Result.Error("offline")
        assertTrue(CloseListingUseCase(repository)(listing) is Result.Error)
        coVerify(exactly = 0) { repository.deleteListing(any()) }
        coVerify(exactly = 0) { repository.saveListing(any()) }
        assertTrue(CloseListingUseCase(repository)(listing.copy(status = ListingStatus.CLOSED)) is Result.Error)
    }
}
