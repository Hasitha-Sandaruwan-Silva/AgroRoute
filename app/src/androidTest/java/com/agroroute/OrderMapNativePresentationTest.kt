package com.agroroute

import android.graphics.Bitmap
import android.location.Location
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.agroroute.core.components.LocationMap
import com.agroroute.core.components.OrderLocationSection
import com.agroroute.core.location.*
import com.agroroute.core.model.Order
import com.agroroute.core.theme.AgroRouteTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File

/** Real native projection/camera tests. Only saved dummy coordinates; no order writes. */
class OrderMapNativePresentationTest {
    @get:Rule val compose = createComposeRule()
    private val pickup = SelectedLocation("Test Farmer pickup", 7.29, 80.63)
    private val delivery = SelectedLocation("Test delivery depot", 6.91, 79.86)
    private fun fix(lat: Double = 7.31, lng: Double = 80.61) = Location("presentation-test").apply {
        latitude = lat; longitude = lng
    }

    private fun mapContent(body: @Composable () -> Unit) = compose.setContent {
        AgroRouteTheme {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) { body() }
        }
    }
    private fun View.findMapView(): MapView? {
        if (this is MapView) return this
        if (this is ViewGroup) for (i in 0 until childCount) getChildAt(i).findMapView()?.let { return it }
        return null
    }
    private fun nativeMap(): Pair<MapView, MapLibreMap> {
        var view: MapView? = null
        var map: MapLibreMap? = null
        compose.runOnIdle {
            val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            view = activities.firstNotNullOfOrNull { it.window.decorView.findMapView() }
            view?.getMapAsync { map = it }
        }
        assumeTrue("MapTiler configuration is required for native map tests", view != null)
        compose.waitUntil(25_000) {
            var ready = false
            compose.runOnIdle { ready = map?.style?.isFullyLoaded == true }
            ready || compose.onAllNodesWithText("Map unavailable. The saved destination is shown below.").fetchSemanticsNodes().isNotEmpty()
        }
        var ready = false
        compose.runOnIdle { ready = map?.style?.isFullyLoaded == true }
        assumeTrue("Native style unavailable; camera verification needs a loaded map", ready)
        return view!! to map!!
    }
    private fun assertVisible(view: MapView, map: MapLibreMap, vararg pins: SelectedLocation) {
        compose.runOnIdle {
            for (pin in pins) {
                val pixel = map.projection.toScreenLocation(LatLng(pin.latitude, pin.longitude))
                assertTrue("Pin clipped horizontally: $pixel", pixel.x > 16 && pixel.x < view.width - 16)
                assertTrue("Pin clipped vertically: $pixel", pixel.y > 24 && pixel.y < view.height - 16)
            }
        }
    }
    @Test fun initialBoundsKeepCurrentAndPickupOnScreenAtCityZoom() {
        mapContent {
            LocationMap(pickup, currentFix = fix(), orderViewport = OrderMapViewport("waiting", OrderDestinationKind.PICKUP),
                modifier = Modifier.fillMaxWidth().height(260.dp))
        }
        val (view, map) = nativeMap()
        compose.waitUntil(5_000) { var zoom = 0.0; compose.runOnIdle { zoom = map.cameraPosition.zoom }; zoom > 10 }
        assertVisible(view, map, pickup, SelectedLocation("", 7.31, 80.61))
        capture("pickup-bounds")
    }
    @Test fun destinationOnlyCameraCentersSavedPinAtStreetZoom() {
        mapContent {
            LocationMap(delivery, orderViewport = OrderMapViewport("transit", OrderDestinationKind.DELIVERY),
                modifier = Modifier.fillMaxWidth().height(260.dp))
        }
        val (view, map) = nativeMap()
        compose.runOnIdle {
            assertEquals(15.5, map.cameraPosition.zoom, .05)
            assertEquals(delivery.latitude, map.cameraPosition.target!!.latitude, .0001)
            assertEquals(delivery.longitude, map.cameraPosition.target!!.longitude, .0001)
        }
        assertVisible(view, map, delivery)
        capture("delivery-destination")
    }
    @Test fun positionUpdatesPreserveManualCameraUntilExplicitRecenter() {
        var current by mutableStateOf(fix())
        var recenter by mutableIntStateOf(0)
        mapContent {
            LocationMap(pickup, currentFix = current,
                orderViewport = OrderMapViewport("waiting", OrderDestinationKind.PICKUP, recenterRequest = recenter),
                modifier = Modifier.fillMaxWidth().height(260.dp))
        }
        val (view, map) = nativeMap()
        compose.runOnIdle { map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(8.0, 81.0), 13.0)) }
        compose.runOnIdle { current = fix(7.32, 80.62) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(8.0, map.cameraPosition.target!!.latitude, .0001)
            assertEquals(81.0, map.cameraPosition.target!!.longitude, .0001)
            recenter++
        }
        compose.waitUntil(5_000) { var lat = 0.0; compose.runOnIdle { lat = map.cameraPosition.target!!.latitude }; lat in 7.28..7.34 }
        assertVisible(view, map, pickup, SelectedLocation("", 7.32, 80.62))
    }
    @Test fun stageChangeReusesMapViewAndMovesToDeliverySnapshot() {
        var destination by mutableStateOf(pickup)
        var stage by mutableStateOf("waiting")
        mapContent {
            LocationMap(destination, orderViewport = OrderMapViewport(stage,
                if (stage == "waiting") OrderDestinationKind.PICKUP else OrderDestinationKind.DELIVERY),
                modifier = Modifier.fillMaxWidth().height(260.dp))
        }
        val (view, _) = nativeMap()
        compose.runOnIdle { destination = delivery; stage = "transit" }
        val (updatedView, updatedMap) = nativeMap()
        assertSame(view, updatedView)
        compose.runOnIdle {
            assertEquals(delivery.latitude, updatedMap.cameraPosition.target!!.latitude, .0001)
            assertEquals(delivery.longitude, updatedMap.cameraPosition.target!!.longitude, .0001)
        }
        assertVisible(view, updatedMap, delivery)
    }
    @Test fun providerRoadGeometryFitsItsDetourAndShowsBothPins() {
        val current = SelectedLocation("", 7.31, 80.61)
        val detour = SelectedLocation("", 7.35, 80.68)
        val route = RoadRoute(listOf(current, detour, pickup), 12400.0, 1440)
        mapContent {
            LocationMap(pickup, currentFix = fix(), orderViewport = OrderMapViewport("test-provider", OrderDestinationKind.PICKUP, route = route),
                modifier = Modifier.fillMaxWidth().height(260.dp))
            Text(route.summary())
        }
        val (view, map) = nativeMap()
        assertVisible(view, map, current, detour, pickup)
        compose.onNodeWithText("12.4 km • 24 min").assertIsDisplayed()
        capture("provider-fixture-route")
    }
    @Test fun nativeStyleFailureKeepsSavedAddressAndExternalNavigationAvailable() {
        compose.setContent {
            AgroRouteTheme {
                Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    OrderLocationSection(Order(orderId = "style-test", pickupLocation = pickup.toStoredText(),
                        deliveryLocation = delivery.toStoredText()), true)
                }
            }
        }
        val (_, map) = nativeMap()
        capture("pickup-card")
        // Deliberately invalid local style JSON, no network request or secret-bearing URL.
        compose.runOnIdle { map.setStyle(Style.Builder().fromJson("invalid local test style")) }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Map unavailable. The saved destination is shown below.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Test Farmer pickup").assertIsDisplayed()
        compose.onNodeWithText("Navigate to Farmer").assertIsEnabled()
    }
    private fun capture(name: String) {
        // Style parsing is not tile/render completion; allow native tiles and symbol fade to settle.
        val started = android.os.SystemClock.uptimeMillis()
        compose.waitUntil(5_000) { android.os.SystemClock.uptimeMillis() - started >= 1_500 }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        File(instrumentation.targetContext.cacheDir, "order-map-$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
