package com.agroroute.features.farmer.listing

import com.agroroute.core.model.Bid
import com.agroroute.core.model.BidStatus
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingCategory
import com.agroroute.core.model.Order
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateListingUseCaseTest {

    @Test
    fun farmerCanEditTheirOwnListingAndExistingPhotoUrlIsPreserved() = runBlocking {
        val fixture = fixture()
        val updated = fixture.existing.copy(cropName = "Brown Rice", reservePrice = 12.0)
        val saved = slot<HarvestListing>()
        coEvery { fixture.listingRepository.updateListing(capture(saved)) } returns Result.Success(Unit)

        val result = fixture.useCase(updated)

        assertTrue(result is Result.Success)
        assertEquals(fixture.existing.imageUrls, saved.captured.imageUrls)
        assertEquals(fixture.existing.soldQuantity, saved.captured.soldQuantity, 0.0)
        assertEquals(fixture.existing.status, saved.captured.status)
        assertTrue(saved.captured.biddingEnabled)
        coVerify(exactly = 0) { fixture.photoStorage.uploadListingPhoto(any(), any(), any()) }
    }

    @Test
    fun farmerCannotEditAnotherFarmersListing() = runBlocking {
        val fixture = fixture(currentUid = "farmer-2", profileUid = "farmer-2")
        val result = fixture.useCase(fixture.existing)

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { fixture.listingRepository.updateListing(any()) }
    }

    @Test
    fun pendingBidsPreventEditing() = runBlocking {
        val fixture = fixture(pendingBids = listOf(Bid(listingId = "listing-1", status = BidStatus.PENDING)))
        val result = fixture.useCase(fixture.existing.copy(cropName = "Changed"))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("pending bids"))
        coVerify(exactly = 0) { fixture.listingRepository.updateListing(any()) }
    }

    @Test
    fun existingOrdersPreventEditing() = runBlocking {
        val fixture = fixture(orders = listOf(Order(listingId = "listing-1")))
        val result = fixture.useCase(fixture.existing.copy(reservePrice = 99.0))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("order history"))
        coVerify(exactly = 0) { fixture.listingRepository.updateListing(any()) }
    }

    @Test
    fun replacementPhotoIsUploadedAndItsSecureUrlIsSaved() = runBlocking {
        val fixture = fixture()
        val saved = slot<HarvestListing>()
        coEvery { fixture.photoStorage.uploadListingPhoto("content://picked/new", "farmer-1", "listing-1") } returns
            "https://res.cloudinary.com/demo/image/upload/new-photo"
        coEvery { fixture.listingRepository.updateListing(capture(saved)) } returns Result.Success(Unit)

        val result = fixture.useCase(fixture.existing.copy(imageUrls = listOf("content://picked/new")))

        assertTrue(result is Result.Success)
        assertEquals(
            "https://res.cloudinary.com/demo/image/upload/new-photo",
            saved.captured.imageUrls.single()
        )
        coVerify(exactly = 1) {
            fixture.photoStorage.uploadListingPhoto("content://picked/new", "farmer-1", "listing-1")
        }
    }

    private fun fixture(
        currentUid: String = "farmer-1",
        profileUid: String = currentUid,
        pendingBids: List<Bid> = emptyList(),
        orders: List<Order> = emptyList()
    ): Fixture {
        val existing = HarvestListing(
            listingId = "listing-1",
            farmerId = "farmer-1",
            farmerName = "Farmer One",
            cropName = "Rice",
            quantity = 10.0,
            availableQuantity = 8.0,
            soldQuantity = 2.0,
            reservePrice = 10.0,
            imageUrls = listOf("https://res.cloudinary.com/demo/image/upload/original-photo"),
            category = ListingCategory.ORGANIC
        )
        val listingRepository = mockk<HarvestListingRepository>()
        val photoStorage = mockk<ListingPhotoStorage>()
        val authRepository = mockk<AuthRepository>()
        val bidRepository = mockk<BidRepository>()
        val orderRepository = mockk<OrderRepository>()

        every { authRepository.currentUserId } returns currentUid
        every { authRepository.getCurrentUserProfile() } returns flowOf(
            Result.Success(UserProfile(userId = profileUid, role = UserRole.FARMER))
        )
        every { listingRepository.getListingById("listing-1") } returns flowOf(Result.Success(existing))
        coEvery { bidRepository.syncBidsForListing("listing-1") } returns Result.Success(Unit)
        every { bidRepository.getBidsForListing("listing-1") } returns flowOf(Result.Success(pendingBids))
        coEvery { orderRepository.syncOrders(currentUid) } returns Result.Success(Unit)
        every { orderRepository.getOrdersForFarmer(currentUid) } returns flowOf(Result.Success(orders))

        return Fixture(
            existing = existing,
            listingRepository = listingRepository,
            photoStorage = photoStorage,
            useCase = UpdateListingUseCase(
                listingRepository,
                photoStorage,
                authRepository,
                bidRepository,
                orderRepository
            )
        )
    }

    private data class Fixture(
        val existing: HarvestListing,
        val listingRepository: HarvestListingRepository,
        val photoStorage: ListingPhotoStorage,
        val useCase: UpdateListingUseCase
    )
}