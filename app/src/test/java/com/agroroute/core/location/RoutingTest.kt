package com.agroroute.core.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RoutingTest {
    private val start = SelectedLocation("", 7.31, 80.61)
    private val destination = SelectedLocation("Farm", 7.29, 80.63)
    private val road = RoadRoute(listOf(start, SelectedLocation("", 7.30, 80.62), destination), 12400.0, 1440)

    @Test fun unconfiguredProviderReturnsNoGeometryOrInventedEta() = runTest {
        assertEquals(RouteResult.NotConfigured, CachedRouteRepository().route(start, destination))
    }
    @Test fun providerFailureIsReadableUnavailableAndNeverThrowsRawException() = runTest {
        val repository = CachedRouteRepository(RouteProvider { _, _ -> error("private provider diagnostic") })
        assertEquals(RouteResult.Unavailable, repository.route(start, destination))
    }
    @Test fun slowProviderTimesOutWithoutBlockingDestinationMap() = runTest {
        val repository = CachedRouteRepository(RouteProvider { _, _ -> awaitCancellation() })
        assertEquals(RouteResult.Unavailable, repository.route(start, destination))
    }
    @Test fun successfulGeometryAndMetricsAreCachedWithoutRepeatedProviderCalls() = runTest {
        var calls = 0
        val repository = CachedRouteRepository(RouteProvider { _, _ -> calls++; RouteResult.Available(road) })
        repeat(5) { assertEquals(RouteResult.Available(road), repository.route(start, destination)) }
        assertEquals(1, calls)
        assertEquals("12.4 km • 24 min", road.summary())
    }
    @Test fun changedDestinationRequestsItsOwnRoute() = runTest {
        var calls = 0
        val repository = CachedRouteRepository(RouteProvider { _, _ -> calls++; RouteResult.Unavailable })
        repository.route(start, destination)
        repository.route(start, destination.copy(longitude = 79.86))
        assertEquals(2, calls)
    }
    @Test fun duplicateConcurrentRequestsShareOneProviderResult() = runTest {
        var calls = 0
        val repository = CachedRouteRepository(RouteProvider { _, _ -> calls++; RouteResult.Available(road) })
        val a = async { repository.route(start, destination) }
        val b = async { repository.route(start, destination) }
        assertEquals(a.await(), b.await())
        assertEquals(1, calls)
    }
    @Test fun providerCannotMutateCachedGeometryAfterReturningIt() = runTest {
        val points = road.geometry.toMutableList()
        val repository = CachedRouteRepository(RouteProvider { _, _ -> RouteResult.Available(road.copy(geometry = points)) })
        val result = repository.route(start, destination) as RouteResult.Available
        points.clear()
        assertEquals(3, result.route.geometry.size)
    }
    @Test fun invalidRouteMetricsAndMissingGeometryAreRejected() {
        assertTrue(runCatching { road.copy(distanceMeters = Double.NaN) }.isFailure)
        assertTrue(runCatching { road.copy(durationSeconds = 0) }.isFailure)
        assertTrue(runCatching { road.copy(geometry = emptyList()) }.isFailure)
    }
    @Test fun cancellationPropagatesInsteadOfBeingCachedAsFailure() = runTest {
        val repository = CachedRouteRepository(RouteProvider { _, _ -> throw CancellationException("screen disposed") })
        try { repository.route(start, destination); fail("Expected cancellation") }
        catch (_: CancellationException) { }
    }
}
