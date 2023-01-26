package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.net.URI

class UpcomingRouteObjectsTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 48.143406486859135
        longitude = 11.428011943347627
    }

    @Before
    fun setup() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
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
            upcomingIncidentForOneLeg.distanceToStart,
            upcomingIncidentForTwoLegsRoute.distanceToStart
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

    private fun getRoutesFromTheSameOriginButDifferentWaypointsCount():
        Triple<List<NavigationRoute>, List<NavigationRoute>, String> {
        val origin = "11.428011943347627,48.143406486859135"
        val destination = "11.443258702449555,48.14554279886465"
        val routeWithIncident = NavigationRoute.create(
            directionsResponseJson = readRawFileText(
                activity,
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
                activity,
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
