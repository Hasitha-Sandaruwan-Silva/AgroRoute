package com.agroroute.core.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.ceil

data class RouteManeuver(val instruction: String, val location: SelectedLocation)

/** Geometry must be returned by a road-routing provider, never synthesized between pins. */
data class RoadRoute(
    val geometry: List<SelectedLocation>,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maneuvers: List<RouteManeuver> = emptyList()
) {
    init {
        require(geometry.size >= 2)
        require(distanceMeters.isFinite() && distanceMeters > 0)
        require(durationSeconds > 0)
    }

    fun summary(): String = String.format(Locale.getDefault(), "%.1f km • %d min",
        distanceMeters / 1000, ceil(durationSeconds / 60.0).toLong().coerceAtLeast(1))
}

sealed interface RouteResult {
    data class Available(val route: RoadRoute) : RouteResult
    data object NotConfigured : RouteResult
    data object Unavailable : RouteResult
}

fun interface RouteProvider {
    suspend fun roadRoute(start: SelectedLocation, destination: SelectedLocation): RouteResult
}

interface RouteRepository {
    suspend fun route(start: SelectedLocation, destination: SelectedLocation): RouteResult
}

/** The existing MapTiler tile/style key is not a configured routing provider. */
object UnconfiguredRouteProvider : RouteProvider {
    override suspend fun roadRoute(start: SelectedLocation, destination: SelectedLocation) = RouteResult.NotConfigured
}

/** Small session cache; callers request once per stage, not on every position update. */
class CachedRouteRepository(private val provider: RouteProvider = UnconfiguredRouteProvider) : RouteRepository {
    private data class Key(val startLat: Double, val startLng: Double, val endLat: Double, val endLng: Double)
    private val cache = LinkedHashMap<Key, RouteResult>()
    private val mutex = Mutex()

    override suspend fun route(start: SelectedLocation, destination: SelectedLocation): RouteResult = mutex.withLock {
        val key = Key(start.latitude, start.longitude, destination.latitude, destination.longitude)
        cache[key]?.let { return@withLock it }
        val result = try {
            withTimeoutOrNull(10_000) { provider.roadRoute(start, destination) } ?: RouteResult.Unavailable
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { RouteResult.Unavailable }
        // Own immutable copies: a provider cannot mutate a displayed/cached route later.
        val cached = if (result is RouteResult.Available) result.copy(route = result.route.copy(
            geometry = result.route.geometry.toList(), maneuvers = result.route.maneuvers.toList())) else result
        if (cache.size >= 4) cache.remove(cache.keys.first())
        cache[key] = cached
        cached
    }
}
