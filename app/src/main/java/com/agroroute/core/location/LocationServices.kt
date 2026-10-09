package com.agroroute.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

class LocationServices(private val context: Context) {
    @Suppress("DEPRECATION")
    suspend fun search(query: String): List<SelectedLocation> = withContext(Dispatchers.IO) {
        check(Geocoder.isPresent()) { "Place search is unavailable on this device. Choose a pin on the map." }
        Geocoder(context, Locale.getDefault()).getFromLocationName(query, 6).orEmpty().map {
            SelectedLocation(it.getAddressLine(0).orEmpty(), it.latitude, it.longitude)
        }
    }

    @Suppress("DEPRECATION")
    suspend fun address(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) null else runCatching {
            Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()?.getAddressLine(0)
        }.getOrNull()
    }

    fun hasPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @Suppress("MissingPermission", "DEPRECATION")
    suspend fun currentLocation(): Location? = withContext(Dispatchers.Main.immediate) {
        if (!hasPermission()) return@withContext null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val providers = manager.getProviders(true).filter {
            (fineGranted && it == LocationManager.GPS_PROVIDER) || it == LocationManager.NETWORK_PROVIDER
        }
        val cached = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < 120_000_000_000L }
            .maxByOrNull { it.elapsedRealtimeNanos }
        if (cached != null) return@withContext cached
        if (providers.isEmpty()) return@withContext null
        withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                try {
                    providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) }
                } catch (_: SecurityException) {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                } catch (_: IllegalArgumentException) {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }
}
