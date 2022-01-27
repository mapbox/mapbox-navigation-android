package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.RequestRoutesResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteOptionsBuilderTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @Test
    fun navigateFromCurrentLocation() {
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING,
                jsonResponse = readRawFileText(activity, R.raw.reroute_response_dc_very_short),
                expectedCoordinates = null,
                relaxedExpectedCoordinates = true
            )
        )

        val mapboxNavigation = runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().getTargetContext()
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .accessToken(getMapboxAccessTokenFromResources(context))
                    .build()
            )
        }

        val routeRequest = runBlocking(Dispatchers.Main) {
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes { builder ->
                builder
                    .fromCurrentLocation()
                    .toDestination(
                        coordinate = Point.fromLngLat(2.0, 2.0)
                    )
                    .profileDriving()
                    .baseUrl(mockWebServerRule.baseUrl)
            } as RequestRoutesResult.Successful
        }

        val routeOptions = routeRequest.routes.first().routeOptions
        assertEquals(
            listOf(
                Point.fromLngLat(1.0, 1.0),
                Point.fromLngLat(2.0, 2.0),
            ),
            routeOptions.coordinatesList()
        )
    }

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 1.0
            latitude = 1.0
        }
    }
}
