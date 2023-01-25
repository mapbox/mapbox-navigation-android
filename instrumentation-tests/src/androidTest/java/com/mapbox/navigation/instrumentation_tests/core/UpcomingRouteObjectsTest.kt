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
    @Ignore("waiting for the NN fix, see NAVAND-1108")
    fun distanceToIncidentDoNotChangeAfterAddingNewWaypoint() = sdkTest {
        val routeWithIncident = NavigationRoute.create(
            directionsResponseJson = readRawFileText(activity, R.raw.route_witn_incident_one_leg),
            routeRequestUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                "11.428011943347627,48.143406486859135;11.443258702449555,48.14554279886465" +
                "?access_token=**&alternatives=true" +
                "&annotations=closure,congestion_numeric,congestion,speed,duration,distance" +
                "&geometries=polyline6&language=en&overview=full&steps=true",
            routerOrigin = RouterOrigin.Offboard
        )
        val routeWithIncidentTwoLegs = NavigationRoute.create(
            directionsResponseJson = readRawFileText(activity, R.raw.route_witn_incident_two_legs),
            routeRequestUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                "11.428011943347627,48.143406486859135;11.42945687746061,48.1436160028498" +
                ";11.443258702449555,48.14554279886465" +
                "?access_token=**&alternatives=true" +
                "&annotations=closure,congestion_numeric,congestion,speed,duration,distance" +
                "&geometries=polyline6&language=en&overview=full&steps=true",
            routerOrigin = RouterOrigin.Offboard
        )
        val incidentId = "6058002857835914"

        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routeWithIncident)
        val upcomingIncidentForOneLeg = mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
            .upcomingRoadObjects
            .first { it.roadObject.id == incidentId }

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routeWithIncidentTwoLegs)
        val upcomingIncidentForTwoLegsRoute = mapboxNavigation.routeProgressUpdates()
            .first { it.currentState == RouteProgressState.TRACKING }
            .upcomingRoadObjects
            .first { it.roadObject.id == incidentId }

        assertEquals(
            upcomingIncidentForOneLeg.distanceToStart,
            upcomingIncidentForTwoLegsRoute.distanceToStart
        )
    }
}
