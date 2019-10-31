package com.mapbox.services.android.navigation.v5.navigation

import com.google.gson.GsonBuilder
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.api.directions.v5.DirectionsAdapterFactory
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigator.Navigator
import com.mapbox.services.android.navigation.v5.BaseTest
import com.mapbox.services.android.navigation.v5.internal.navigation.FreeDriveLocationUpdater
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationTelemetry
import com.mapbox.services.android.navigation.v5.route.FasterRoute
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import io.mockk.mockk
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FasterRouteDetectorTest : BaseTest() {

    companion object {
        private const val PRECISION_6 = "directions_v5_precision_6.json"
    }

    @Test
    fun sanity() {
        val fasterRouteDetector = FasterRouteDetector()

        assertNotNull(fasterRouteDetector)
    }

    @Test
    fun defaultFasterRouteEngine_didGetAddedOnInitialization() {
        val navigation = buildNavigationWithFasterRouteEnabled()

        assertNotNull(navigation.fasterRouteEngine)
    }

    @Test
    fun addFasterRouteEngine_didGetAdded() {
        val navigation = buildNavigationWithFasterRouteEnabled()
        val fasterRouteEngine = mockk<FasterRoute>(relaxed = true)

        navigation.fasterRouteEngine = fasterRouteEngine

        assertEquals(navigation.fasterRouteEngine, fasterRouteEngine)
    }

    @Test
    fun onFasterRouteResponse_isFasterRouteIsTrue() {
        val navigation = buildNavigationWithFasterRouteEnabled()
        val fasterRouteEngine = navigation.fasterRouteEngine
        var currentProgress = obtainDefaultRouteProgress()
        val longerRoute = currentProgress.directionsRoute()!!.toBuilder()
            .duration(10000000.0)
            .build()
        currentProgress = currentProgress.toBuilder()
            .directionsRoute(longerRoute)
            .build()
        val response = obtainADirectionsResponse()

        val isFasterRoute = fasterRouteEngine.isFasterRoute(response, currentProgress)

        assertTrue(isFasterRoute)
    }

    @Test
    fun onSlowerRouteResponse_isFasterRouteIsFalse() {
        val navigation = buildNavigationWithFasterRouteEnabled()
        val fasterRouteEngine = navigation.fasterRouteEngine
        var currentProgress = obtainDefaultRouteProgress()
        val longerRoute = currentProgress.directionsRoute()!!.toBuilder()
            .duration(1000.0)
            .build()
        currentProgress = currentProgress.toBuilder()
            .directionsRoute(longerRoute)
            .build()
        val response = obtainADirectionsResponse()

        val isFasterRoute = fasterRouteEngine.isFasterRoute(response, currentProgress)

        assertFalse(isFasterRoute)
    }

    private fun buildNavigationWithFasterRouteEnabled(): MapboxNavigation {
        val options = MapboxNavigationOptions.Builder()
            .enableFasterRouteDetection(true)
            .build()
        val context = RuntimeEnvironment.application
        return MapboxNavigation(
            context,
            ACCESS_TOKEN,
            options,
            mockk<NavigationTelemetry>(relaxed = true),
            mockk<LocationEngine>(relaxed = true),
            mockk<Navigator>(relaxed = true),
            mockk<FreeDriveLocationUpdater>(relaxed = true)
        )
    }

    @Throws(Exception::class)
    private fun obtainDefaultRouteProgress(): RouteProgress {
        val aRoute = obtainADirectionsRoute()
        return buildTestRouteProgress(aRoute, 100.0, 700.0, 1000.0, 0, 0)
    }

    @Throws(IOException::class)
    private fun obtainADirectionsRoute(): DirectionsRoute {
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create()
        val body = loadJsonFixture(PRECISION_6)
        val response = gson.fromJson(body, DirectionsResponse::class.java)
        return response.routes()[0]
    }

    @Throws(IOException::class)
    private fun obtainADirectionsResponse(): DirectionsResponse {
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create()
        val body = loadJsonFixture(PRECISION_6)
        return gson.fromJson(body, DirectionsResponse::class.java)
    }
}
