package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigator.RouteAlertLocation
import kotlinx.coroutines.flow.*
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit

class JniOverflowTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 48.143406486859135
        longitude = 11.428011943347627
    }

    @Test
    fun testLargeRoadObjects() = sdkTest(timeout = TimeUnit.MINUTES.toMillis(5)) {
        mapboxNavigation = createMapboxNavigation()
        val origin = Point.fromLngLat(139.790845, 35.634688)
        stayOnPosition(origin)
        mapboxNavigation.startTripSession()

        val numberOfIncidents = 1000

        setUpRoutes(
            rawRoutesResponseWithIncidents(numberOfIncidents),
            "139.790845,35.634688;139.758247,35.649077"
        )

        var numberOfObjects = 0L
        var numberOfRouteAlertsObjects = 0L
        val routeAlertObjects = mutableListOf<RouteAlertLocation>()

        stayOnPosition(
            latitude = origin.latitude(),
            longitude = origin.longitude(),
            bearing = 280.0f,
            frequencyHz = 100,
        ) {
            mapboxNavigation.routeProgressUpdates().collect {
                val upcomingRoadObjects = it.upcomingRoadObjects

                numberOfObjects += upcomingRoadObjects.size

                numberOfRouteAlertsObjects += upcomingRoadObjects.filter { upcomingRoadObject ->
                    upcomingRoadObject.roadObject.nativeRoadObject.location.isRouteAlertLocation
                }.size

                routeAlertObjects += upcomingRoadObjects.map { upcomingRoadObject ->
                    upcomingRoadObject.roadObject.nativeRoadObject.location.routeAlertLocation
                }

                Log.d("Test.", "Test. " +
                    "numberOfRouteAlertsObjects = ${routeAlertObjects.size}, " +
                    "numberOfObjects = $numberOfObjects"
                )
            }
        }
    }

    private fun generateIncidentJson(id: Int): String {
        return """{
              "id": "$id",
              "type": "construction",
              "creation_time": "2023-02-14T15:20:00Z",
              "start_time": "2023-02-14T15:20:00Z",
              "end_time": "2023-02-14T15:35:00Z",
              "iso_3166_1_alpha2": "JP",
              "iso_3166_1_alpha3": "JPN",
              "description": "Construction",
              "sub_type": "LANE_RESTRICTION",
              "sub_type_description": "One lane restriction",
              "alertc_codes": [
                449,
                21
              ],
              "lanes_blocked": [],
              "length": 2363,
              "south": 35.610916,
              "west": 139.755889,
              "north": 35.625759,
              "east": 139.774398,
              "congestion": {
                "value": 101
              },
              "geometry_index_start": 14,
              "geometry_index_end": 25,
              "affected_road_names": [
                "B/首都高速湾岸線/Expwy Wangan Line"
              ]
            }"""
    }

    private fun rawRoutesResponseWithIncidents(numberOfIncidents: Int): String {
        val rawJson = readRawFileText(context, R.raw.route_with_road_objects_large)
        val newIncidents = (0 until numberOfIncidents).joinToString {
            generateIncidentJson(it)
        }
        return rawJson.replace("incidents\": []", "incidents\": [$newIncidents]")
    }

    private fun stayOnPosition(position: Point, bearing: Float = 0f) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = position.latitude()
                longitude = position.longitude()
                this.bearing = bearing
            },
            times = 120
        )
    }

    private suspend fun setUpRoutes(rawResponse: String, coordinates: String) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = "driving-traffic",
                jsonResponse = rawResponse,
                expectedCoordinates = null,
                relaxedExpectedCoordinates = true
            )
        )
        val routeOptions = RouteOptions.builder()
            .profile("driving-traffic")
            .baseUrl(mockWebServerRule.baseUrl)
            .coordinates(coordinates)
            .alternatives(true)
            .annotations("closure,congestion_numeric,congestion,speed,duration,distance")
            .geometries("polyline6")
            .overview("full")
            .steps(true)
            .build()
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes.routes)
    }

    private fun createMapboxNavigation(): MapboxNavigation = MapboxNavigationProvider.create(
        NavigationOptions.Builder(context)
            .accessToken(getMapboxAccessTokenFromResources(context))
            .routingTilesOptions(
                RoutingTilesOptions.Builder()
                    .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                    .build()
            )
            .build()
    )
}
