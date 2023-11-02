package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.withoutInternet
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * See https://docs.mapbox.com/api/navigation/directions/#directions-api-errors for info
 * about different Directions API failures
 */
class RouteRequestTests : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private val origin = Point.fromLngLat(
        13.361378213031003,
        52.49813341962201
    )

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = origin.longitude()
            latitude = origin.latitude()
        }
    }

    @Test
    fun requestRouteWithoutInternetAndTiles() = sdkTest {
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            withoutInternet {
                val routes = navigation.requestRoutes(createTestRouteOptions())
                assertTrue(routes is RouteRequestResult.Failure)
                val failture = (routes as RouteRequestResult.Failure).reasons.first()
                assertTrue(failture.isRetryable)
            }
        }
    }

    @Test
    fun error500Unknown() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse().setBody("unexpected server error").setResponseCode(500)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    @Test
    fun invalidInput() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse()
                    .setBody(readRawFileText(context, R.raw.invalid_alternative_response_body))
                    .setResponseCode(422)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    @Test
    fun wrongSegment() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse()
                    .setBody(readRawFileText(context, R.raw.wrong_segment_response_body))
                    .setResponseCode(200)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    @Test
    fun noRouteFound() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse()
                    .setBody("{\"code\":\"NoRoute\",\"message\":\"No route found\",\"routes\":[]}")
                    .setResponseCode(200)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    @Test
    fun invalidAccessToken() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse()
                    .setBody("{\"message\":\"Not Authorized - Invalid Token\"}")
                    .setResponseCode(401)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    @Test
    fun noAccessToken() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse()
                    .setBody(readRawFileText(context, R.raw.no_access_token_response_body))
                    .setResponseCode(401)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    @Test
    fun forbidden() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse()
                    .setBody("{\"message\":\"Forbidden\"}")
                    .setResponseCode(403)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    @Test
    fun profileNotFound() = sdkTest {
        mockWebServerRule.requestHandlers.add(
            MockRequestHandler {
                MockResponse()
                    .setBody("{\"message\":\"Profile not found\"}")
                    .setResponseCode(401)
            }
        )
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            val routes = navigation.requestRoutes(createTestRouteOptions())
            assertTrue(routes is RouteRequestResult.Failure)
            val failture = (routes as RouteRequestResult.Failure).reasons.first()
            assertFalse(failture.isRetryable)
        }
    }

    private fun createTestRouteOptions(): RouteOptions {
        return RouteOptions.builder()
            .baseUrl(mockWebServerRule.baseUrl)
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    origin,
                    Point.fromLngLat(
                        13.361478213031003,
                        52.49823341962201
                    )
                )
            )
            .build()
    }
}
