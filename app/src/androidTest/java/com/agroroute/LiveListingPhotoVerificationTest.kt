package com.agroroute

import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Opt-in, read-only verification of a listing created through the real app UI. */
class LiveListingPhotoVerificationTest {
    @Test fun serverStoresOnlyRemotePhotoUrlsForTheSignedInFarmer() {
        val id = InstrumentationRegistry.getArguments().getString("listing_photo_qa_id").orEmpty()
        assumeTrue("Pass the explicitly created QA listing ID to run this live read", id.isNotBlank())
        val user = requireNotNull(FirebaseAuth.getInstance().currentUser) { "Keep the Farmer signed in for live QA" }
        val listing = Tasks.await(FirebaseFirestore.getInstance().collection("listings").document(id).get(Source.SERVER),
            30, TimeUnit.SECONDS)
        assertTrue("The QA listing must exist on the server", listing.exists())
        assertEquals(user.uid, listing.getString("farmerId"))
        val images = listing.get("imageUrls") as? List<*>
        assertTrue("Listing must contain one to five photos", images != null && images.size in 1..5)
        assertTrue("Listing must store the existing Cloudinary HTTPS URL, never a local capture URI",
            images?.all { (it as? String)?.startsWith("https://res.cloudinary.com/") == true } == true)
    }
}
