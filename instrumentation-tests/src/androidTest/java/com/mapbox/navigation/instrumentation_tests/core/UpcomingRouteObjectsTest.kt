package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.ic.Interchange
import com.mapbox.navigation.base.trip.model.roadobject.jct.Junction
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.net.URI

class UpcomingRouteObjectsTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)
    private val tolerance = 0.0001

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 48.143406486859135
        longitude = 11.428011943347627
    }

    @Before
    fun setup() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
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
    }

    @Test
    fun sanityDistanceToStart() = sdkTest {
        val origin = Point.fromLngLat(139.790845, 35.634688)
        val positionAlongTheRoute = Point.fromLngLat(139.77466009278072, 35.625810364295305)
        stayOnPosition(origin)
        mapboxNavigation.startTripSession()
        setUpRoutes(R.raw.route_with_road_objects, "139.790845,35.634688;139.758247,35.649077")

        val upcomingRoadObjectsOnStart = mapboxNavigation.routeProgressUpdates()
            .first {
                it.currentState == RouteProgressState.TRACKING &&
                    it.currentRouteGeometryIndex == 0
            }
            .upcomingRoadObjects
        assertEquals(7, upcomingRoadObjectsOnStart.size)
        assertEquals(220.27365, upcomingRoadObjectsOnStart[0].distanceToStart!!, tolerance)
        assertEquals(2421.0498, upcomingRoadObjectsOnStart[1].distanceToStart!!, tolerance)
        assertEquals(1789.1295, upcomingRoadObjectsOnStart[2].distanceToStart!!, tolerance)
        assertEquals(4576.7544, upcomingRoadObjectsOnStart[3].distanceToStart!!, tolerance)
        assertEquals(4576.7544, upcomingRoadObjectsOnStart[4].distanceToStart!!, tolerance)
        assertEquals(9232.6493, upcomingRoadObjectsOnStart[5].distanceToStart!!, tolerance)
        assertEquals(9434.4270, upcomingRoadObjectsOnStart[6].distanceToStart!!, tolerance)

        val distanceDiff = 1763.9070399
        stayOnPosition(positionAlongTheRoute)
        val upcomingRoadObjectsAlongTheRoute = mapboxNavigation.routeProgressUpdates()
            .first {
                it.currentState == RouteProgressState.TRACKING &&
                    it.currentRouteGeometryIndex > 0
            }
            .upcomingRoadObjects
        assertEquals(6, upcomingRoadObjectsAlongTheRoute.size)
        upcomingRoadObjectsAlongTheRoute.forEachIndexed { index, objectAlongTheRoute ->
            assertEquals(
                "Object along the route #$index",
                upcomingRoadObjectsOnStart[index + 1].distanceToStart!! - distanceDiff,
                objectAlongTheRoute.distanceToStart!!,
                tolerance
            )
        }
    }

    @Test
    fun icTest() = sdkTest {
        mapboxNavigation.startTripSession()
        val icOrigin = Point.fromLngLat(140.025875, 35.66031)
        stayOnPosition(icOrigin)
        setUpRoutes(R.raw.route_with_ic, "140.025878,35.660315;140.019419,35.664076")

        val upcomingInterchanges = mapboxNavigation.routeProgressUpdates()
            .first {
                it.currentState == RouteProgressState.TRACKING &&
                    it.navigationRoute.id.startsWith("route_with_ic")
            }
            .upcomingRoadObjects
            .filter { it.roadObject.objectType == RoadObjectType.IC }
        assertEquals(1, upcomingInterchanges.size)
        assertEquals(
            "Wangannarashino IC",
            (upcomingInterchanges[0].roadObject as Interchange).name[0].value
        )
    }

    @Test
    fun jctTest() = sdkTest {
        val jctOrigin = Point.fromLngLat(139.790845, 35.634688)
        stayOnPosition(jctOrigin)
        mapboxNavigation.startTripSession()
        setUpRoutes(R.raw.route_with_jct, "139.790833,35.63468;139.778821,35.635955")

        val upcomingJunctions = mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
            .upcomingRoadObjects
            .filter { it.roadObject.objectType == RoadObjectType.JCT }
        assertEquals(1, upcomingJunctions.size)
        assertEquals("Ariake JCT", (upcomingJunctions[0].roadObject as Junction).name[0].value)
    }

    @Test
    @Ignore("waiting for the NN fix, see NN-449")
    fun distanceToIncidentDoesNotChangeAfterAddingNewWaypointOnTheRouteGeometry() = sdkTest {
        val (oneLegRoute, twoLegsRoute, incidentId) =
            getRoutesFromTheSameOriginButDifferentWaypointsCount()

        setRoutesOriginAsCurrentLocation(oneLegRoute, twoLegsRoute)

        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(oneLegRoute)
        val upcomingIncidentForOneLeg = mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
            .upcomingRoadObjects
            .first { it.roadObject.id == incidentId }

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(twoLegsRoute)
        val upcomingIncidentForTwoLegsRoute = mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
            .upcomingRoadObjects
            .first { it.roadObject.id == incidentId }

        assertEquals(
            upcomingIncidentForOneLeg.distanceToStart!!,
            upcomingIncidentForTwoLegsRoute.distanceToStart!!,
            0.1
        )
    }

    private fun stayOnPosition(position: Point) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = position.latitude()
                longitude = position.longitude()
            },
            times = 120
        )
    }

    private fun setRoutesOriginAsCurrentLocation(
        oneLegRoute: List<NavigationRoute>,
        twoLegsRoute: List<NavigationRoute>
    ) {
        val origin = oneLegRoute.first().waypoints!!.first().location()
        assertEquals(origin, twoLegsRoute.first().waypoints!!.first().location())
        mockLocationUpdatesRule.generateLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }

    private suspend fun setUpRoutes(file: Int, coordinates: String) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = "driving-traffic",
                jsonResponse = readRawFileText(context, file),
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

    private fun getRoutesFromTheSameOriginButDifferentWaypointsCount():
        Triple<List<NavigationRoute>, List<NavigationRoute>, String> {
        val origin = "11.428011943347627,48.143406486859135"
        val destination = "11.443258702449555,48.14554279886465"
        val routeWithIncident = NavigationRoute.create(
            directionsResponseJson = readRawFileText(
                context,
                R.raw.route_through_incident_6058002857835914_one_leg
            ),
            routeRequestUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                "$origin;$destination" +
                "?access_token=**&alternatives=true" +
                "&annotations=closure,congestion_numeric,congestion,speed,duration,distance" +
                "&geometries=polyline6&language=en&overview=full&steps=true",
            routerOrigin = RouterOrigin.Offboard
        )
        val routeWithIncidentTwoLegs = NavigationRoute.create(
            directionsResponseJson = readRawFileText(
                context,
                R.raw.route_through_incident_6058002857835914_two_legs
            ),
            routeRequestUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                "$origin;11.42945687746061,48.1436160028498" +
                ";$destination" +
                "?access_token=**&alternatives=true" +
                "&annotations=closure,congestion_numeric,congestion,speed,duration,distance" +
                "&geometries=polyline6&language=en&overview=full&steps=true",
            routerOrigin = RouterOrigin.Offboard
        )
        val incident = routeWithIncident.first()
            .directionsRoute.legs()!!.first()
            .incidents()!!.first()
        return Triple(routeWithIncident, routeWithIncidentTwoLegs, incident.id())
    }
}
