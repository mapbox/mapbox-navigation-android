package com.mapbox.navigation.instrumentation_tests.ui.navigationview

import android.location.Location
import android.widget.FrameLayout
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.ViewProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.NavigationViewTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class NavigationViewExternalObserversTests :
    BaseTest<NavigationViewTestActivity>(NavigationViewTestActivity::class.java) {

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private val mockRoute: MockRoute by lazy {
        MockRoutesProvider.dc_very_short(activity)
    }

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Test
    fun addRouteProgressObserver() {
        val routeProgressCount = CountDownLatch(1)
        runOnMainSync {
            val navigationView = getNavigationView()
            navigationView.navigationViewApi.addRouteProgressObserver { _ ->
                routeProgressCount.countDown()
            }
            setRoute()
            navigationView.navigationViewApi.temporaryStartNavigation()
        }
        routeProgressCount.await()
    }

    @Test
    fun addRoutesObserver() {
        val routeProgressCount = CountDownLatch(1)
        runOnMainSync {
            val navigationView = getNavigationView()
            navigationView.navigationViewApi.addRoutesObserver { _ ->
                routeProgressCount.countDown()
            }
            setRoute()
        }
        routeProgressCount.await()
    }

    @Test
    fun addTripSessionStateObserver() {
        val routeProgressCount = CountDownLatch(1)
        runOnMainSync {
            val navigationView = getNavigationView()
            navigationView.navigationViewApi.addTripSessionStateObserver { observer ->
                assertEquals(TripSessionState.STARTED.name, observer.name)
                routeProgressCount.countDown()
            }
            setRoute()
            navigationView.navigationViewApi.temporaryStartNavigation()
        }
        routeProgressCount.await()
    }

    @Test
    fun addVoiceInstructionObserver() {
        val routeProgressCount = CountDownLatch(1)
        runOnMainSync {
            val navigationView = getNavigationView()
            navigationView.navigationViewApi.addVoiceInstructionsObserver { observer ->
                assertEquals(
                    "Head east on Fulton Street, then turn left onto 27th Avenue",
                    observer.announcement()
                )
                routeProgressCount.countDown()
            }
            setRoute()
            navigationView.navigationViewApi.temporaryStartNavigation()
        }
        routeProgressCount.await()
    }

    @Test
    fun addBannerInstructionsObserver() {
        val routeProgressCount = CountDownLatch(1)
        runOnMainSync {
            val navigationView = getNavigationView()
            navigationView.navigationViewApi.addBannerInstructionsObserver { observer ->
                assertEquals(628.0, observer.distanceAlongGeometry(), 0.0)
                routeProgressCount.countDown()
            }
            setRoute()
            navigationView.navigationViewApi.temporaryStartNavigation()
        }
        routeProgressCount.await()
    }

    @Test
    fun addLocationObserver() {
        val routeProgressCount = CountDownLatch(1)
        runOnMainSync {
            val navigationView = getNavigationView()
            navigationView.navigationViewApi.addLocationObserver(object : LocationObserver {
                override fun onNewLocationMatcherResult(
                    locationMatcherResult: LocationMatcherResult
                ) {
                    routeProgressCount.countDown()
                }

                override fun onNewRawLocation(rawLocation: Location) {
                    //
                }
            })
            setRoute()
            navigationView.navigationViewApi.temporaryStartNavigation()
        }
        routeProgressCount.await()
    }

    @Test
    fun addArrivalObserver() {
        val routeProgressCount = CountDownLatch(1)
        runOnMainSync {
            val navigationView = getNavigationView()
            navigationView.navigationViewApi.addArrivalObserver(object : ArrivalObserver {
                override fun onWaypointArrival(routeProgress: RouteProgress) {
                    //
                }

                override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
                    //
                }

                override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
                    assertEquals(RouteProgressState.COMPLETE, routeProgress.currentState)
                    routeProgressCount.countDown()
                }
            })
            setShortRoute()
            navigationView.navigationViewApi.temporaryStartNavigation()
        }
        routeProgressCount.await()
    }

    private fun getNavigationView(): NavigationView {
        return NavigationView(
            activity,
            null,
            getMapboxAccessTokenFromResources(activity)
        ).also {
            activity.findViewById<FrameLayout>(R.id.rootView).addView(it)
            it.navigationViewApi.configureNavigationView(ViewProvider())
        }
    }

    private fun setRoute() {
        val directionsResponse = MockRoutesProvider
            .loadDirectionsResponse(activity, R.raw.multiple_routes)
        val route = directionsResponse.routes()[0]
        val mapboxNavigation = MapboxNavigationProvider.retrieve()
        mapboxNavigation.setRoutes(listOf(route))
        mockLocationReplayerRule.playRoute(route)
    }

    private fun setShortRoute() {
        val routeAsString = readRawFileText(activity, R.raw.short_route)
        val route = DirectionsRoute.fromJson(routeAsString)
        val mapboxNavigation = MapboxNavigationProvider.retrieve()
        mapboxNavigation.setRoutes(listOf(route))
        mockLocationReplayerRule.playRoute(route)
    }
}
