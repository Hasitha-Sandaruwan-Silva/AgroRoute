package com.agroroute.core.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocationServicesFailureTest {
    @After fun cleanup() { Dispatchers.resetMain(); unmockkAll() }
    private fun permitted(context: Context, granted: Boolean) {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, any()) } returns
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }
    @Test fun deniedPermissionDoesNotAccessLocationManager() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val context = mockk<Context>()
        permitted(context, false)
        assertFalse(LocationServices(context).hasPermission())
        assertNull(LocationServices(context).currentLocation())
        verify(exactly = 0) { context.getSystemService(any<String>()) }
    }
    @Test fun disabledProvidersReturnNoFixWithoutRequestingUpdates() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val context = mockk<Context>(); val manager = mockk<LocationManager>()
        permitted(context, true)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns manager
        every { manager.getProviders(true) } returns emptyList()
        assertNull(LocationServices(context).currentLocation())
        verify(exactly = 0) { manager.requestLocationUpdates(any<String>(), any<Long>(), any<Float>(), any<LocationListener>(), any()) }
    }
    @Test fun noLastKnownLocationTimesOutAndUnregistersListener() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val context = mockk<Context>(); val manager = mockk<LocationManager>(relaxed = true)
        permitted(context, true)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns manager
        every { manager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
        every { manager.getLastKnownLocation(any()) } returns null
        mockkStatic(android.os.Looper::class)
        every { android.os.Looper.getMainLooper() } returns mockk()
        assertNull(LocationServices(context).currentLocation())
        verify(exactly = 1) { manager.removeUpdates(any<LocationListener>()) }
    }
    @Test fun absentGeocoderKeepsCoordinatesUsableAndSearchFailureReadable() = runTest {
        mockkStatic(Geocoder::class)
        every { Geocoder.isPresent() } returns false
        val services = LocationServices(mockk())
        assertNull(services.address(7.0, 80.0))
        val saved = SelectedLocation("Selected location", 7.0, 80.0).toStoredText()
        assertNotNull(SelectedLocation.fromStoredText(saved))
        try { services.search("Farm"); fail("Expected unavailable search") }
        catch (error: IllegalStateException) { assertTrue(error.message!!.contains("Choose a pin")) }
    }
}
