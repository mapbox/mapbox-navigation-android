package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.base.trip.model.roadobject.ic.Interchange
import com.mapbox.navigation.base.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.base.trip.model.roadobject.jct.Junction
import com.mapbox.navigation.base.trip.model.roadobject.merge.MergingArea
import com.mapbox.navigation.base.trip.model.roadobject.railwaycrossing.RailwayCrossing
import com.mapbox.navigation.base.trip.model.roadobject.restrictedarea.RestrictedArea
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.Tunnel
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.net.URI
import kotlin.math.abs

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

    @Test
    fun sanityDistanceToStart() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
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
        mapboxNavigation = createMapboxNavigation()
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
        mapboxNavigation = createMapboxNavigation()
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
    @Ignore("NN does not support AG yet")
    fun incidentTrafficCodesTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        mapboxNavigation.startTripSession()
        val origin = Point.fromLngLat(137.52814, 34.837394)
        stayOnPosition(origin)
        setUpRoutes(
            R.raw.route_with_jartic_codes,
            "137.5281542766699,34.83741480073306;137.53072840419628,34.83636637489471"
        )

        val upcomingIncidents = mapboxNavigation.routeProgressUpdates()
            .first {
                it.currentState == RouteProgressState.TRACKING &&
                    it.navigationRoute.id.startsWith("route_with_jartic_codes")
            }
            .upcomingRoadObjects
            .filter { it.roadObject.objectType == RoadObjectType.INCIDENT }
        assertEquals(1, upcomingIncidents.size)
        val incident = upcomingIncidents[0].roadObject as Incident
        println("[ddlog] ${incident.info.trafficCodes}")
        assertEquals(
            31,
            incident.info.trafficCodes["jartic_regulation_code"]
        )
        assertEquals(
            449,
            incident.info.trafficCodes["jartic_cause_code"]
        )
    }

    @Test
    fun distanceToIncidentDoesNotChangeAfterAddingNewWaypointOnTheRouteGeometry() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
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

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun roadObjectsEuropeSanityTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        val coordinates = listOf(
            Point.fromLngLat(14.238166, 50.32172),
            Point.fromLngLat(13.234976, 51.551251)
        )
        mockWebServerRule.requestHandlers.clear()
        val routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(context, R.raw.route_with_road_objects_europe),
            coordinates,
            relaxedExpectedCoordinates = true
        )
        mockWebServerRule.requestHandlers.add(routeHandler)
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_with_road_objects_europe",
                    readRawFileText(context, R.raw.route_with_road_objects_europe_refresh1),
                    acceptedGeometryIndex = 61
                )
            )
        )

        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .alternatives(false)
                .enableRefresh(true)
                .coordinatesList(coordinates)
                .baseUrl(mockWebServerRule.baseUrl)
                .build()
        ).getSuccessfulResultOrThrowException().routes
        val originalRoadObjects = routes.first().upcomingRoadObjects
        val expectedOriginalRoadObjectClasses = listOf(
            RailwayCrossing::class.java,
            TollCollection::class.java,
            TollCollection::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            TollCollection::class.java,
            TollCollection::class.java,
            TollCollection::class.java,
            TollCollection::class.java,
            TollCollection::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            TollCollection::class.java,
            CountryBorderCrossing::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Incident::class.java,
            RailwayCrossing::class.java,
            RailwayCrossing::class.java,
            RestrictedArea::class.java,
            RestrictedArea::class.java,
            RailwayCrossing::class.java,
            RailwayCrossing::class.java,
            RailwayCrossing::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Incident::class.java,
            Tunnel::class.java,
            CountryBorderCrossing::class.java,
            TollCollection::class.java,
            RestrictedArea::class.java,
        )

        assertEquals(
            expectedOriginalRoadObjectClasses,
            originalRoadObjects.map { it.roadObject::class.java }
        )

        stayOnPosition(coordinates[0])

        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutes(routes)

        // distance travelled ~ 4894.979, first road object passed
        val movedAlongTheRoutePosition =
            routes.first().directionsRoute.completeGeometryToPoints()[60]
        stayOnPosition(movedAlongTheRoutePosition)
        val updateAfterMovedAlongTheRoute = mapboxNavigation.routeProgressUpdates()
            .filter {
                it.currentRouteGeometryIndex == 61
            }
            .first()
        val distanceDiffAfterMovedAlongTheRoute = updateAfterMovedAlongTheRoute.distanceTraveled
        val expectedRoadObjectsAfterMovedAlongTheRoute = originalRoadObjects.drop(1).map {
            it.roadObject::class.java to it.distanceToStart!! - distanceDiffAfterMovedAlongTheRoute
        }

        checkRoadObjects(
            expectedRoadObjectsAfterMovedAlongTheRoute,
            updateAfterMovedAlongTheRoute.upcomingRoadObjects
        )

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()

        // distance traveled ~ 22073, second and third road objects are passed
        val positionAfterFirstRefresh =
            routes.first().directionsRoute.completeGeometryToPoints()[250]
        stayOnPosition(positionAfterFirstRefresh)
        val updateAfterRefresh = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentRouteGeometryIndex == 249 }
            .first()
        val distanceDiffAfterFirstRefresh = updateAfterRefresh.distanceTraveled
        // refresh geometry_index_start = 1810, so resulting geometry_index_start = 1871
        // (refresh was made with current_route_geometry_index = 61),
        // this corresponds to distanceTravelled = 134617.89 ~ 134612.883
        // (1 geometry index != 1 meter, so 134617.89 is an approximate value),
        val newIncidentDistanceToStart = 134612.883
        val newIncidentIndex = 22

        val expectedObjectsAfterFirstRefresh = originalRoadObjects
            .map {
                it.roadObject::class.java to it.distanceToStart!! - distanceDiffAfterFirstRefresh
            }.toMutableList().apply {
                add(
                    newIncidentIndex,
                    Incident::class.java
                        to newIncidentDistanceToStart - distanceDiffAfterFirstRefresh
                )
            }.drop(3)

        checkRoadObjects(expectedObjectsAfterFirstRefresh, updateAfterRefresh.upcomingRoadObjects)

        mockWebServerRule.requestHandlers.removeLast()
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_with_road_objects_europe",
                    readRawFileText(context, R.raw.route_with_road_objects_europe_refresh2),
                    acceptedGeometryIndex = 249
                )
            )
        )

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .take(2)
            .toList()

        // distanceTravelled = 52112.027, 10 road objects passed
        val positionAfterSecondRefresh =
            routes.first().directionsRoute.completeGeometryToPoints()[700]
        stayOnPosition(positionAfterSecondRefresh)
        val updateAfterSecondRefresh = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentRouteGeometryIndex == 700 }
            .first()
        val distanceDiffAfterSecondRefresh = updateAfterSecondRefresh.distanceTraveled
        val expectedObjectsAfterSecondRefresh = originalRoadObjects
            .map {
                it.roadObject::class.java to it.distanceToStart!! - distanceDiffAfterSecondRefresh
            }.toMutableList().apply {
                add(
                    newIncidentIndex,
                    Incident::class.java
                        to newIncidentDistanceToStart - distanceDiffAfterSecondRefresh
                )
                removeAt(newIncidentIndex - 1) // first incident removed
            }.drop(10)

        checkRoadObjects(
            expectedObjectsAfterSecondRefresh,
            updateAfterSecondRefresh.upcomingRoadObjects
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun roadObjectsJapanSanityTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        val coordinates = listOf(
            Point.fromLngLat(139.790845, 35.634688),
            Point.fromLngLat(139.778818, 35.635951),
            Point.fromLngLat(139.785936, 35.550703),
            Point.fromLngLat(140.025875, 35.66031),
        )
        mockWebServerRule.requestHandlers.clear()
        val routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(context, R.raw.route_with_road_objects_japan),
            coordinates,
            relaxedExpectedCoordinates = true
        )
        mockWebServerRule.requestHandlers.add(routeHandler)
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_with_road_objects_japan",
                    readRawFileText(context, R.raw.route_with_road_objects_japan_refresh1),
                    acceptedGeometryIndex = 60
                )
            )
        )

        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .alternatives(false)
                .enableRefresh(true)
                .coordinatesList(coordinates)
                .baseUrl(mockWebServerRule.baseUrl)
                .build()
        ).getSuccessfulResultOrThrowException().routes
        val originalRoadObjects = routes.first().upcomingRoadObjects
        val expectedOriginalRoadObjectClasses = listOf(
            Junction::class.java,
            RestStop::class.java,
            Junction::class.java,
            Junction::class.java,
            Incident::class.java,
            Incident::class.java,
            TollCollection::class.java,
            Junction::class.java,
            MergingArea::class.java,
            Junction::class.java,
            Junction::class.java,
            Tunnel::class.java,
            Junction::class.java,
            MergingArea::class.java,
            RestStop::class.java,
            Junction::class.java,
            Tunnel::class.java,
            MergingArea::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Junction::class.java,
            Tunnel::class.java,
            TollCollection::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            Tunnel::class.java,
            RestStop::class.java,
            Junction::class.java,
            Tunnel::class.java,
            Junction::class.java,
            Junction::class.java,
            Junction::class.java,
            Junction::class.java,
            Junction::class.java,
            Interchange::class.java,
            Interchange::class.java,
            TollCollection::class.java,
        )

        assertEquals(
            expectedOriginalRoadObjectClasses,
            originalRoadObjects.map { it.roadObject::class.java }
        )

        mapboxNavigation.startTripSession()
        stayOnPosition(coordinates[0], 180f)
        mapboxNavigation.flowLocationMatcherResult()
            .filter {
                abs(it.enhancedLocation.latitude - coordinates[0].latitude()) < 0.01 &&
                    abs(it.enhancedLocation.longitude - coordinates[0].longitude()) < 0.01
            }.first()

        mapboxNavigation.setNavigationRoutes(routes)
        mapboxNavigation.routeProgressUpdates()
            .filter {
                it.currentState == RouteProgressState.TRACKING &&
                    it.currentRouteGeometryIndex == 0
            }
            .first()

        // distance travelled ~ 2327.765, first road object passed
        val movedAlongTheRoutePosition =
            routes.first().directionsRoute.completeGeometryToPoints()[60]
        stayOnPosition(movedAlongTheRoutePosition, 280f)
        mapboxNavigation.navigateNextRouteLeg()

        val updateAfterMovedAlongTheRoute = mapboxNavigation.routeProgressUpdates()
            .filter {
                it.currentRouteGeometryIndex == 60
            }
            .first()
        val distanceDiffAfterMovedAlongTheRoute = updateAfterMovedAlongTheRoute.distanceTraveled
        val expectedRoadObjectsAfterMovedAlongTheRoute = originalRoadObjects.drop(1).map {
            it.roadObject::class.java to it.distanceToStart!! - distanceDiffAfterMovedAlongTheRoute
        }

        checkRoadObjects(
            expectedRoadObjectsAfterMovedAlongTheRoute,
            updateAfterMovedAlongTheRoute.upcomingRoadObjects
        )

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()

        // distance traveled ~ 5222, second and third road objects are passed
        val positionAfterFirstRefresh =
            routes.first().directionsRoute.completeGeometryToPoints()[150]
        stayOnPosition(positionAfterFirstRefresh, 0f)
        val updateAfterRefresh = mapboxNavigation.routeProgressUpdates()
            .filter { it.currentRouteGeometryIndex == 150 }
            .first()
        val distanceDiffAfterFirstRefresh = updateAfterRefresh.distanceTraveled

        val expectedObjectsAfterFirstRefresh = originalRoadObjects
            .map {
                it.roadObject::class.java to it.distanceToStart!! - distanceDiffAfterFirstRefresh
            }.drop(3)

        checkRoadObjects(expectedObjectsAfterFirstRefresh, updateAfterRefresh.upcomingRoadObjects)
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

    private fun setRoutesOriginAsCurrentLocation(
        oneLegRoute: List<NavigationRoute>,
        twoLegsRoute: List<NavigationRoute>
    ) {
        val origin = oneLegRoute.first().waypoints!!.first().location()
        assertEquals(origin, twoLegsRoute.first().waypoints!!.first().location())
        stayOnPosition(origin)
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

    private fun checkRoadObjects(
        expectedInput: List<Pair<Class<*>, Double>>,
        actualInput: List<UpcomingRoadObject>
    ) {
        val expected = expectedInput.map { it.first to ApproxMeters(it.second) }
        val actual = actualInput.map {
            it.roadObject::class.java to ApproxMeters(it.distanceToStart!!)
        }
        try {
            assertEquals(expected, actual)
        } catch (ex: Throwable) {
            Log.e(
                "[UpcomingRouteObjectsTest]",
                "Expected: ${expected.joinToString("\n")}, \nactual: ${actual.joinToString("\n")}"
            )
            throw ex
        }
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

    private class ApproxMeters(val value: Double) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ApproxMeters

            if (abs(value - other.value) > TOLERANCE) return false

            return true
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        override fun toString(): String {
            return "$value"
        }

        private companion object {
            private const val TOLERANCE = 0.01
        }
    }
}
