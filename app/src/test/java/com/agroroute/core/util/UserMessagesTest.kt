package com.agroroute.core.util

import com.agroroute.core.components.presentationError
import com.agroroute.core.repository.ListingPhotoStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UserMessagesTest {
    @Test fun providerDiagnosticsNeverReachDisplay() {
        for (message in listOf("Cloudinary upload failed (HTTP 401)", "Firestore transaction failed",
            "FirebaseAuthException: invalid credential", "Room database write failed",
            "Repository error", "java.lang.IllegalStateException: internal",
            "SQL constraint failed", "HTTP 503 Service Unavailable", "Invalid JSON response",
            "Unexpected document path: users/private-user", "collection reference users/private-user",
            "Request failed: INTERNAL_ERROR", "Status{code=INTERNAL, details=private}", "https://private.example/api?token=secret",
            "Check failed.", "Required value was null.", "x".repeat(401))) {
            assertEquals(message, GENERIC_ACTION_ERROR, presentationError(message))
        }
    }
    @Test fun businessGuidanceAndDomainTermsStayIntact() {
        for (message in listOf("Incorrect pickup code", "Requested quantity exceeds available stock",
            "This listing has pending bids. Resolve them before editing it.",
            "Reviews are available after delivery.", "Maximum 5 photos", "Choose a JPEG, PNG, or WebP photo.",
            "Primary photo", "Supplier collection is already confirmed", "Orders cannot be cancelled after pickup")) {
            assertEquals(message, presentationError(message))
        }
    }
    @Test fun knownServiceFailuresGiveUsefulRecovery() {
        assertEquals("This item is unavailable. Refresh and try again.", presentationError("NOT_FOUND: private/key"))
        assertEquals("This action is not available for your account or the current item state. Refresh and try again.",
            presentationError("FirebaseFirestoreException: PERMISSION_DENIED"))
        assertEquals("Could not connect. Check your connection and try again.",
            presentationError("UNAVAILABLE: failed to connect to Firestore"))
    }
    @Test fun unknownThrowableNeverExposesItsMessage() = runBlocking {
        val cause = Exception("provider diagnostic with private account details")
        val result = safeCall<Unit> { throw cause } as Result.Error
        assertEquals(GENERIC_ACTION_ERROR, result.message)
        assertSame(cause, result.cause)
    }
    @Test fun localValidationPreservesMessageAndCause() = runBlocking {
        val cause = IllegalArgumentException("Quantity must be greater than zero")
        val result = safeCall<Unit> { throw cause } as Result.Error
        assertEquals(cause.message, result.message)
        assertSame(cause, result.cause)
    }
    @Test fun sdkValidationDoesNotExposeOtherwiseInnocentLookingDiagnostics() {
        val error = IllegalStateException("The connection pool is closed")
        error.stackTrace = arrayOf(StackTraceElement("com.vendor.Internal", "read", "Internal.java", 12))
        assertEquals(GENERIC_ACTION_ERROR, userFacingThrowable(error))
    }
    @Test fun networkFailureDoesNotExposeHost() = runBlocking {
        val result = safeCall<Unit> { throw java.net.UnknownHostException("private.example") } as Result.Error
        assertEquals("Could not connect. Check your connection and try again.", result.message)
    }
    @Test fun photoFailureKeepsStageAndDiagnosticCauseWithoutDisplayingIt() {
        val cause = java.io.IOException("Cloudinary upload failed (HTTP 400): private preset")
        val upload = ListingPhotoStorageException(ListingPhotoStorageException.Stage.UPLOAD, cause)
        assertEquals(PHOTO_UPLOAD_ERROR, upload.message)
        assertSame(cause, upload.cause)
        assertEquals(ListingPhotoStorageException.Stage.UPLOAD, upload.stage)
        assertEquals("Couldn't process the photo. Please try again.",
            ListingPhotoStorageException(ListingPhotoStorageException.Stage.DOWNLOAD_URL, cause).message)
    }
    @Test fun supportedPhotoValidationRemainsActionable() {
        assertEquals("Choose a JPEG, PNG, or WebP photo.", photoUploadMessage(java.io.IOException(
            "The selected file is not a supported image type. Please choose a JPEG, PNG, or WebP image.")))
        assertTrue(photoUploadMessage(java.io.IOException("Photo is too large (12 MB).")).contains("smaller photo"))
    }
    @Test fun cancellationStillPropagates() = runBlocking {
        val cancellation = CancellationException("cancelled")
        try {
            safeCall<Unit> { throw cancellation }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) { assertSame(cancellation, actual) }
    }
}
