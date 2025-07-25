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
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.testing.utils.assertions.compareIdWithIncidentId
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.utils.routes.requestMockRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.URI
import kotlin.math.abs

class UpcomingRouteObjectsTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)
    private val tolerance = 0.0001

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 48.143406486859135
        longitude = 11.428011943347627
    }

    @After
    fun tearDown() {
        runBlocking(Dispatchers.Main.immediate) {
            val path = mapboxNavigation.historyRecorder.stopRecording()
            Log.i("Tests", "history file recorder: $path")
            MapboxNavigationProvider.destroy()
        }
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

        // distanceTraveled diff from RouteProgress objects received on initialLocation and on positionAlongTheRoute
        val distanceDiff = 1766.675
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
                tolerance,
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
            (upcomingInterchanges[0].roadObject as Interchange).name[0].value,
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
    fun incidentTrafficCodesTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        mapboxNavigation.startTripSession()
        val origin = Point.fromLngLat(137.52814, 34.837394)
        stayOnPosition(origin)
        setUpRoutes(
            R.raw.route_with_jartic_codes,
            "137.5281542766699,34.83741480073306;137.53072840419628,34.83636637489471",
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
        assertEquals(
            31,
            incident.info.trafficCodes["jartic_regulation_code"],
        )
        assertEquals(
            449,
            incident.info.trafficCodes["jartic_cause_code"],
        )
    }

    @Test
    fun incidentLengthTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        mapboxNavigation.startTripSession()
        val origin = Point.fromLngLat(137.52814, 34.837394)
        stayOnPosition(origin)
        setUpRoutes(
            R.raw.route_with_length,
            "137.5281542766699,34.83741480073306;137.53072840419628,34.83636637489471",
        )

        val upcomingIncidents = mapboxNavigation.routeProgressUpdates()
            .first { it.navigationRoute.id.startsWith("route_with_length") }
            .upcomingRoadObjects
            .filter { it.roadObject.objectType == RoadObjectType.INCIDENT }
        assertEquals(1, upcomingIncidents.size)
        val incident = upcomingIncidents[0].roadObject as Incident
        assertEquals(
            1784,
            incident.info.length,
        )
    }

    @Test
    fun distanceToIncidentDoesNotChangeAfterAddingNewWaypointOnTheRouteGeometry() = sdkTest {
        mapboxNavigation = createMapboxNavigation()

        val (oneLegMockRoute, twoLegsMockRoute, incidentId) =
            RoutesProvider.two_routes_different_legs_count_the_same_incident(context)

        val oneLegRoute = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            oneLegMockRoute,
        )

        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(oneLegRoute)
        mapboxNavigation.moveAlongTheRouteUntilTracking(oneLegRoute[0], mockLocationReplayerRule)
        val upcomingIncidentForOneLeg = mapboxNavigation.routeProgressUpdates()
            .first {
                it.currentState == RouteProgressState.TRACKING
            }
            .upcomingRoadObjects
            .first { it.roadObject.compareIdWithIncidentId(incidentId) }

        val twoLegsRoute = mapboxNavigation.requestMockRoutes(
            mockWebServerRule,
            twoLegsMockRoute,
        )
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(twoLegsRoute)
        val upcomingIncidentForTwoLegsRoute = mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
            .upcomingRoadObjects
            .first { it.roadObject.compareIdWithIncidentId(incidentId) }

        assertEquals(
            upcomingIncidentForOneLeg.distanceToStart!!,
            upcomingIncidentForTwoLegsRoute.distanceToStart!!,
            0.1,
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun roadObjectsEuropeSanityTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        val coordinates = listOf(
            Point.fromLngLat(14.238166, 50.32172),
            Point.fromLngLat(13.234976, 51.551251),
        )
        mockWebServerRule.requestHandlers.clear()
        val routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(context, R.raw.route_with_road_objects_europe),
            coordinates,
            relaxedExpectedCoordinates = true,
        )
        mockWebServerRule.requestHandlers.add(routeHandler)
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_with_road_objects_europe",
                    readRawFileText(context, R.raw.route_with_road_objects_europe_refresh1),
                    acceptedGeometryIndex = 61,
                ),
            ),
        )

        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .alternatives(false)
                .enableRefresh(true)
                .coordinatesList(coordinates)
                .baseUrl(mockWebServerRule.baseUrl)
                .build(),
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
            originalRoadObjects.map { it.roadObject::class.java },
        )

        stayOnPosition(coordinates[0])

        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutes(routes)

        // distance travelled ~ 4894.979, first road object passed
        val movedAlongTheRoutePosition =
            routes.first().directionsRoute.completeGeometryToPoints()[61]
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
            updateAfterMovedAlongTheRoute.upcomingRoadObjects,
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
                        to newIncidentDistanceToStart - distanceDiffAfterFirstRefresh,
                )
            }.drop(3)

        checkRoadObjects(expectedObjectsAfterFirstRefresh, updateAfterRefresh.upcomingRoadObjects)

        mockWebServerRule.requestHandlers[mockWebServerRule.requestHandlers.lastIndex] =
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_with_road_objects_europe",
                    readRawFileText(context, R.raw.route_with_road_objects_europe_refresh2),
                    acceptedGeometryIndex = 249,
                ),
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
                        to newIncidentDistanceToStart - distanceDiffAfterSecondRefresh,
                )
                removeAt(newIncidentIndex - 1) // first incident removed
            }.drop(10)

        checkRoadObjects(
            expectedObjectsAfterSecondRefresh,
            updateAfterSecondRefresh.upcomingRoadObjects,
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
            relaxedExpectedCoordinates = true,
        )
        mockWebServerRule.requestHandlers.add(routeHandler)
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_with_road_objects_japan",
                    readRawFileText(context, R.raw.route_with_road_objects_japan_refresh1),
                    acceptedGeometryIndex = 60,
                ),
            ),
        )

        val routes = mapboxNavigation.requestRoutes(
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .alternatives(false)
                .enableRefresh(true)
                .coordinatesList(coordinates)
                .baseUrl(mockWebServerRule.baseUrl)
                .build(),
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
            originalRoadObjects.map { it.roadObject::class.java },
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
            updateAfterMovedAlongTheRoute.upcomingRoadObjects,
        )

        mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()

        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()

        // distance traveled ~ 5222, second and third road objects are passed
        val positionAfterFirstRefresh =
            routes.first().directionsRoute.completeGeometryToPoints()[151]
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

    @Test
    fun duplicateIncidentIdsTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        val origin = Point.fromLngLat(139.696561, 35.655992)
        val positionAlongTheRoute = Point.fromLngLat(139.69483, 35.655424)
        stayOnPosition(origin)
        mapboxNavigation.startTripSession()
        setUpRoutes(
            R.raw.route_with_duplicate_incidents,
            "139.696561,35.655992;139.87476,35.017036;139.581421,35.543326",
        )

        val upcomingIncidentsOnStart = mapboxNavigation.routeProgressUpdates()
            .first {
                it.currentState == RouteProgressState.TRACKING &&
                    it.currentRouteGeometryIndex == 0
            }
            .upcomingRoadObjects
            .filter { it.roadObject.objectType == RoadObjectType.INCIDENT }

        val incident1 = upcomingIncidentsOnStart.first()
        val incident2 = upcomingIncidentsOnStart.last()
        assertTrue(incident1.roadObject.id.startsWith("5372267336278653"))
        assertTrue(incident2.roadObject.id.startsWith("5372267336278653"))

        assertTrue(incident2.distanceToStart!! > incident1.distanceToStart!!)

        val distanceDiff = 170.4787
        stayOnPosition(positionAlongTheRoute)
        val upcomingRoadObjectsAlongTheRoute = mapboxNavigation.routeProgressUpdates()
            .first {
                it.currentState == RouteProgressState.TRACKING &&
                    it.currentRouteGeometryIndex > 0
            }
            .upcomingRoadObjects
            .filter { it.roadObject.objectType == RoadObjectType.INCIDENT }
        val updatedIncident1 = upcomingRoadObjectsAlongTheRoute.first()
        val updatedIncident2 = upcomingRoadObjectsAlongTheRoute.last()

        assertEquals(updatedIncident1.roadObject.id, incident1.roadObject.id)
        assertEquals(updatedIncident2.roadObject.id, incident2.roadObject.id)

        assertEquals(
            updatedIncident1.distanceToStart!!,
            incident1.distanceToStart!! - distanceDiff,
            tolerance,
        )
        assertEquals(
            updatedIncident2.distanceToStart!!,
            incident2.distanceToStart!! - distanceDiff,
            tolerance,
        )
    }

    @Test
    fun affectedRoadNamesTest() = sdkTest {
        mapboxNavigation = createMapboxNavigation()
        val origin = Point.fromLngLat(140.025878, 35.660315)
        stayOnPosition(origin)
        mapboxNavigation.startTripSession()
        setUpRoutes(
            R.raw.route_with_multilingual_affected_road_names,
            "140.025878,35.660315;140.1611265965725,35.6873837089764",
        )

        val incident = mapboxNavigation.getNavigationRoutes().first().upcomingRoadObjects
            .first { it.roadObject.objectType == RoadObjectType.INCIDENT }

        assertEquals(
            listOf("Higashikanto Expwy(Koya To Itako)"),
            (incident.roadObject as Incident).info.affectedRoadNames,
        )
        assertEquals(
            mapOf(
                "en" to listOf("Higashikanto Expwy(Koya To Itako)"),
                "ja" to listOf("E51/東関東自動車道（高谷～潮来）"),
            ),
            (incident.roadObject as Incident).info.multilingualAffectedRoadNames,
        )
    }

    private fun stayOnPosition(position: Point, bearing: Float = 0f) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = position.latitude()
                longitude = position.longitude()
                this.bearing = bearing
            },
            times = 120,
        )
    }

    private suspend fun setUpRoutes(file: Int, coordinates: String) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = "driving-traffic",
                jsonResponse = readRawFileText(context, file),
                expectedCoordinates = null,
                relaxedExpectedCoordinates = true,
            ),
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

    private fun checkRoadObjects(
        expectedInput: List<Pair<Class<*>, Double>>,
        actualInput: List<UpcomingRoadObject>,
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
                "Expected: ${expected.joinToString("\n")}, \nactual: ${actual.joinToString("\n")}",
            )
            throw ex
        }
    }

    private fun createMapboxNavigation(): MapboxNavigation = MapboxNavigationProvider.create(
        NavigationOptions.Builder(context)
            .routingTilesOptions(
                RoutingTilesOptions.Builder()
                    .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                    .build(),
            )
            .build(),
    ).also {
        mapboxHistoryTestRule.historyRecorder = it.historyRecorder
        it.historyRecorder.startRecording()
    }

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
