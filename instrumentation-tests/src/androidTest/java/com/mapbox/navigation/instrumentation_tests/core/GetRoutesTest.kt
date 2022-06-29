package com.mapbox.navigation.instrumentation_tests.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.http.MockWebServerRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.net.URL

class GetRoutesTest {

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    @Test
    fun requestManyRoutes() = sdkTest {
        val context = getApplicationContext<Context>()
        val url = URL(
            "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/11.5679995,48.1341107;13.37691,52.51604?access_token=****jgZw&geometries=polyline6&alternatives=true&overview=full&steps=true&bearings=332.4625244%2C45%3B&layers=0%3B&continue_straight=false&annotations=congestion%2Cclosure%2Cdistance%2Cspeed%2Cstate_of_charge&language=en&roundabout_exits=true&voice_instructions=true&banner_instructions=true&voice_units=imperial&enable_refresh=true&snapping_include_closures=true%3Btrue&max_width=2.2&metadata=true"
        )
        val options = RouteOptions.fromUrl(url)
        val updatedUrlOptions = options.toBuilder().baseUrl(mockWebServerRule.baseUrl).build()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(context, R.raw.route_response_route_refresh),
                options.coordinatesList()
            )
        )

        val navigationOptions = NavigationOptions.Builder(context)
            .accessToken("test-token")
            .build()
        val mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
        try {
            overloadDefaultDispatcher()
            val routes = withTimeoutOrNull(10_000) {
                mapboxNavigation.requestRoutes(updatedUrlOptions)
                    .getSuccessfulResultOrThrowException()
            }
            assertNotNull("timeout calculating routes", routes)
        } finally {
            mapboxNavigation.onDestroy()
        }
    }

    private fun overloadDefaultDispatcher() {
        repeat(20) {
            GlobalScope.launch(Dispatchers.Default) {
                Thread.sleep(100_000)
            }
        }
    }
}