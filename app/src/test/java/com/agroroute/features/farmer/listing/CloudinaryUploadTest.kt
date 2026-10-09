package com.agroroute.features.farmer.listing

import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingCategory
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.ListingPhotoStorageException
import com.agroroute.core.util.CloudinaryConfig
import com.agroroute.core.util.Result
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Cloudinary upload integration via [CreateListingUseCase].
 *
 * All Firebase and Cloudinary I/O is faked — these are pure JVM tests that run
 * with no Android framework or network access.
 */
class CloudinaryUploadTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun farmerAuth() = FakeAuthRepository(UserRole.FARMER, "farmer-uid")

    private fun baseListing(imageUrls: List<String> = emptyList()) = HarvestListing(
        location = "North Farm\n[7.000000, 80.000000]",
        listingId = "listing-cld",
        farmerId = "farmer-uid",
        farmerName = "Test Farmer",
        cropName = "Tomatoes",
        quantity = 50.0,
        reservePrice = 5.0,
        category = ListingCategory.ORGANIC,
        imageUrls = imageUrls,
        availableQuantity = 50.0
    )

    // ── CloudinaryConfig constant assertions ──────────────────────────────────

    @Test
    fun cloudinaryConfigHasCorrectUploadUrl() {
        val url = CloudinaryConfig.UPLOAD_URL
        assertTrue(
            "Upload URL must use Cloudinary API and the configured cloud name",
            url.startsWith("https://api.cloudinary.com/v1_1/") &&
                url.contains(CloudinaryConfig.CLOUD_NAME) &&
                url.endsWith("/image/upload")
        )
    }

    @Test
    fun cloudinaryConfigHasNonBlankPreset() {
        assertTrue(
            "Unsigned upload preset must be non-blank",
            CloudinaryConfig.UNSIGNED_PRESET.isNotBlank()
        )
    }

    @Test
    fun cloudinaryConfigMaxPhotoSizeIsAtLeast5MB() {
        assertTrue(
            "Max photo size should be at least 5 MB",
            CloudinaryConfig.MAX_PHOTO_BYTES >= 5L * 1_024 * 1_024
        )
    }

    @Test
    fun cloudinaryConfigAllowedMimesIncludeJpegAndPng() {
        assertTrue(CloudinaryConfig.ALLOWED_MIME_TYPES.contains("image/jpeg"))
        assertTrue(CloudinaryConfig.ALLOWED_MIME_TYPES.contains("image/png"))
    }

    // ── Upload success path ───────────────────────────────────────────────────

    @Test
    fun successfulCloudinaryUploadSavesSecureUrlIntoImageUrls() = runBlocking {
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage(secureUrl = "https://res.cloudinary.com/test/image/upload/listing_cld.jpg")
        val listing = baseListing(imageUrls = listOf("content://media/picked/image"))

        val result = CreateListingUseCase(repo, storage, farmerAuth())(listing)

        assertTrue("Use case should succeed", result is Result.Success)
        assertEquals(
            "The returned Cloudinary secure_url must be saved into imageUrls",
            "https://res.cloudinary.com/test/image/upload/listing_cld.jpg",
            repo.savedListing?.imageUrls?.singleOrNull()
        )
        assertNull(
            "Local content:// URI must not appear in the saved listing",
            repo.savedListing?.imageUrls?.firstOrNull { it.startsWith("content://") }
        )
    }

    @Test
    fun cloudinaryStorageReceivesCorrectFarmerIdAndListingId() = runBlocking {
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage()
        val listing = baseListing(imageUrls = listOf("content://media/picked/image"))

        CreateListingUseCase(repo, storage, farmerAuth())(listing)

        assertEquals("farmer-uid", storage.uploadedFarmerId)
        assertEquals("listing-cld", storage.uploadedListingId)
        assertEquals("content://media/picked/image", storage.uploadedUri)
    }

    @Test
    fun listingWithNoPhotoIsSavedWithEmptyImageUrls() = runBlocking {
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage()
        val listing = baseListing(imageUrls = emptyList())

        val result = CreateListingUseCase(repo, storage, farmerAuth())(listing)

        assertTrue(result is Result.Success)
        assertEquals(0, storage.callCount)
        assertEquals(emptyList<String>(), repo.savedListing?.imageUrls)
    }

    @Test
    fun listingWithExistingHttpsUrlSkipsCloudinaryUpload() = runBlocking {
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage()
        val existingUrl = "https://res.cloudinary.com/demo/image/upload/already_uploaded.jpg"
        val listing = baseListing(imageUrls = listOf(existingUrl))

        val result = CreateListingUseCase(repo, storage, farmerAuth())(listing)

        assertTrue(result is Result.Success)
        assertEquals(0, storage.callCount)
        assertEquals(existingUrl, repo.savedListing?.imageUrls?.singleOrNull())
    }

    // ── Upload failure paths ──────────────────────────────────────────────────

    @Test
    fun cloudinaryUploadFailurePreventsListingFromBeingSaved() = runBlocking {
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage(
            failure = ListingPhotoStorageException(
                ListingPhotoStorageException.Stage.UPLOAD,
                java.io.IOException("Network error during photo upload: connect timed out")
            )
        )
        val listing = baseListing(imageUrls = listOf("content://media/picked/image"))

        val result = CreateListingUseCase(repo, storage, farmerAuth())(listing)

        assertTrue("Result must be Error when upload fails", result is Result.Error)
        assertNull("Listing must NOT be saved when photo upload fails", repo.savedListing)
    }

    @Test
    fun cloudinaryUploadFailureResultCarriesListingPhotoStorageException() = runBlocking {
        val cause = java.io.IOException("Cloudinary upload failed (HTTP 400). Check your connection and try again.")
        val uploadException = ListingPhotoStorageException(
            ListingPhotoStorageException.Stage.UPLOAD,
            cause
        )
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage(failure = uploadException)
        val listing = baseListing(imageUrls = listOf("content://media/picked/image"))

        val result = CreateListingUseCase(repo, storage, farmerAuth())(listing)

        val error = result as Result.Error
        assertTrue(
            "The error cause must be a ListingPhotoStorageException",
            error.cause is ListingPhotoStorageException
        )
        assertEquals(
            "The exception stage must be UPLOAD",
            ListingPhotoStorageException.Stage.UPLOAD,
            (error.cause as ListingPhotoStorageException).stage
        )
    }

    @Test
    fun cloudinaryUrlResolutionFailurePreventsListingFromBeingSaved() = runBlocking {
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage(
            failure = ListingPhotoStorageException(
                ListingPhotoStorageException.Stage.DOWNLOAD_URL,
                java.io.IOException("Photo uploaded, but no URL was returned.")
            )
        )
        val listing = baseListing(imageUrls = listOf("content://media/picked/image"))

        val result = CreateListingUseCase(repo, storage, farmerAuth())(listing)

        assertTrue("Result must be Error when URL resolution fails", result is Result.Error)
        assertNull("Listing must NOT be saved when URL resolution fails", repo.savedListing)
        assertEquals(
            ListingPhotoStorageException.Stage.DOWNLOAD_URL,
            ((result as Result.Error).cause as? ListingPhotoStorageException)?.stage
        )
    }

    @Test
    fun listingWithLocalFileUriAlsoTriggersCloudinaryUpload() = runBlocking {
        val repo = FakeListingRepository(HarvestListing())
        val storage = FakeCloudinaryStorage()
        val listing = baseListing(imageUrls = listOf("file:///data/user/0/com.agroroute/cache/photo.jpg"))

        val result = CreateListingUseCase(repo, storage, farmerAuth())(listing)

        assertTrue(result is Result.Success)
        assertEquals(1, storage.callCount)
    }

    // ── shouldShowPhotoUploadRetry — already covered in HarvestListingRulesTest
    // but repeat here for clarity around the Cloudinary context.

    @Test
    fun photoUploadRetryFlagBehavesCorrectlyForCloudinaryErrors() {
        assertTrue(shouldShowPhotoUploadRetry(true, false, "Cloudinary upload failed (HTTP 400)."))
        assertFalse(shouldShowPhotoUploadRetry(true, true, "Cloudinary upload failed (HTTP 400)."))
        assertFalse(shouldShowPhotoUploadRetry(false, false, "Cloudinary upload failed (HTTP 400)."))
        assertFalse(shouldShowPhotoUploadRetry(true, false, null))
    }

    // ── Fakes ──────────────────────────────────────────────────────────────────

    /**
     * In-memory fake that simulates CloudinaryPhotoStorage.
     * Returns [secureUrl] on success, or throws [failure] if set.
     */
    private class FakeCloudinaryStorage(
        private val secureUrl: String = "https://res.cloudinary.com/fake/image/upload/listing-cld.jpg",
        private val failure: Exception? = null
    ) : com.agroroute.core.repository.ListingPhotoStorage {
        var uploadedUri: String? = null
        var uploadedFarmerId: String? = null
        var uploadedListingId: String? = null
        var callCount = 0

        override suspend fun uploadListingPhoto(
            localUri: String,
            farmerId: String,
            listingId: String
        ): String {
            callCount++
            failure?.let { throw it }
            uploadedUri = localUri
            uploadedFarmerId = farmerId
            uploadedListingId = listingId
            return secureUrl
        }
    }
}

// Reuse HarvestListingRulesTest fakes via same package — these are package-private
// helper classes defined in HarvestListingRulesTest.kt.  Rather than duplicating
// them we define minimal local versions here.

private class FakeAuthRepository(
    private val role: com.agroroute.core.model.UserRole?,
    override val currentUserId: String? = role?.name
) : com.agroroute.core.repository.AuthRepository {
    override val isLoggedIn = currentUserId != null

    override fun getCurrentUserProfile(): kotlinx.coroutines.flow.Flow<Result<com.agroroute.core.model.UserProfile?>> =
        kotlinx.coroutines.flow.flowOf(
            Result.Success(role?.let { com.agroroute.core.model.UserProfile(userId = currentUserId.orEmpty(), role = it) })
        )

    override suspend fun restoreSession(): Result<com.agroroute.core.model.UserProfile?> =
        Result.Success(role?.let { com.agroroute.core.model.UserProfile(userId = currentUserId.orEmpty(), role = it) })

    override suspend fun login(email: String, password: String) =
        Result.Error("Not used") as Result<com.agroroute.core.model.UserProfile>

    override suspend fun signInWithGoogle(idToken: String): Result<com.agroroute.core.repository.GoogleSignInOutcome> =
        Result.Error("Not used")

    override suspend fun completeGoogleSignIn(role: com.agroroute.core.model.UserRole): Result<com.agroroute.core.model.UserProfile> =
        Result.Error("Not used")

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: com.agroroute.core.model.UserRole
    ) = Result.Error("Not used") as Result<com.agroroute.core.model.UserProfile>

    override suspend fun logout() = Result.Success(Unit)
    override suspend fun updateProfile(profile: com.agroroute.core.model.UserProfile) = Result.Success(Unit)
    override suspend fun sendPasswordResetEmail(email: String) = Result.Success(Unit)
}

private class FakeListingRepository(var listing: HarvestListing) : com.agroroute.core.repository.HarvestListingRepository {
    var savedListing: HarvestListing? = null
    override fun getAllListings() = kotlinx.coroutines.flow.flowOf(Result.Success(emptyList<HarvestListing>()))
    override fun getListingsByFarmer(farmerId: String) = kotlinx.coroutines.flow.flowOf(Result.Success(emptyList<HarvestListing>()))
    override fun getDraftsByFarmer(farmerId: String) = kotlinx.coroutines.flow.flowOf(Result.Success(emptyList<HarvestListing>()))
    override fun getListingsByStatus(status: com.agroroute.core.model.ListingStatus) = kotlinx.coroutines.flow.flowOf(Result.Success(emptyList<HarvestListing>()))
    override fun getListingById(listingId: String) = kotlinx.coroutines.flow.flowOf(Result.Success(listing.takeIf { it.listingId == listingId }))
    override suspend fun saveListing(listing: HarvestListing): Result<Unit> {
        savedListing = listing
        this.listing = listing
        return Result.Success(Unit)
    }
    override suspend fun saveDraft(listing: HarvestListing) = Result.Success(Unit)
    override suspend fun deleteListing(listingId: String) = Result.Success(Unit)
    override suspend fun syncListings() = Result.Success(Unit)
    override suspend fun toggleBiddingMode(listingId: String, enabled: Boolean) = Result.Success(Unit)
}
