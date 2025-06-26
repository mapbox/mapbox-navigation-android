package com.mapbox.navigation.core.trip.service

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.internal.route.testing.createNavigationRouteForTest
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.test.R
import com.mapbox.navigation.core.tests.activity.TripServiceActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteParser
import com.mapbox.navigator.RoutesChangeInfo
import com.mapbox.navigator.SetRoutesDataParams
import com.mapbox.navigator.SetRoutesParams
import com.mapbox.navigator.SetRoutesReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.Collections
import java.util.Date
import kotlin.coroutines.resume

internal class NativeNavigatorCallbackOrderTest :
    BaseTest<TripServiceActivity>(TripServiceActivity::class.java) {

    private val startLocation = Point.fromLngLat(-121.496066, 38.577764)
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigator: Navigator
    private lateinit var routes: List<NavigationRoute>
    private lateinit var route: NavigationRoute
    private lateinit var multilegRoute: NavigationRoute
    private lateinit var refreshedRouteResponse: String
    private lateinit var callbackInvocations: MutableList<CallbackInvocation>

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = startLocation.latitude()
            longitude = startLocation.longitude()
        }
    }

    @Before
    fun setUp() {
        runOnMainSync {
            callbackInvocations = Collections.synchronizedList(mutableListOf<CallbackInvocation>())
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .build(),
            )
            // starts raw location updates - otherwise we don't get onStatus calls
            mapboxNavigation.startTripSession()

            val nativeNavigatorImpl = mapboxNavigation.navigator
            val navigatorField = nativeNavigatorImpl.javaClass.getDeclaredField("navigator")
            navigatorField.isAccessible = true
            navigator = navigatorField.get(nativeNavigatorImpl) as Navigator
            navigatorField.isAccessible = false
            routes = createNavigationRouteForTest(
                activity.resources.openRawResource(R.raw.route_response_route_refresh)
                    .readBytes().decodeToString().let { DirectionsResponse.fromJson(it) },
                RouteOptions.builder()
                    .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                    .coordinatesList(
                        listOf(
                            startLocation,
                            Point.fromLngLat(-121.480256, 38.576795),
                        ),
                    )
                    .build(),
                RouterOrigin.ONLINE,
            )
            route = routes.first()
            refreshedRouteResponse = activity.resources
                .openRawResource(R.raw.route_response_route_refresh_annotations)
                .readBytes().decodeToString()
            val multiLegDirectionRoute = DirectionsRoute.fromJson(
                activity.resources.openRawResource(R.raw.multileg_route).readBytes()
                    .decodeToString(),
            )
            multilegRoute = createNavigationRouteForTest(
                DirectionsResponse.builder()
                    .code("Ok")
                    .routes(listOf(multiLegDirectionRoute))
                    .build(),
                multiLegDirectionRoute.routeOptions()!!,
                RouterOrigin.ONLINE,
            ).first()
        }
    }

    @After
    fun tearDown() {
        MapboxNavigationProvider.destroy()
    }

    @Test
    fun setRoutes() = runBlocking(Dispatchers.Main.immediate) {
        navigator.addObserver(
            object : NavigatorObserver {
                override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
                    callbackInvocations.add(CallbackInvocation.Status(origin))
                }

                override fun onRoutesChanged(info: RoutesChangeInfo) {
                    // no-op
                }
            },
        )
        waitForStatusUpdatesToBegin()
        navigator.setRoutes(
            SetRoutesParams(route.nativeRoute(), 0, emptyList<RouteInterface>()),
            SetRoutesReason.NEW_ROUTE,
        ) { callbackInvocations.add(CallbackInvocation.RoutesSet) }
        callbackInvocations.waitUntilHas(CallbackInvocation.RoutesSet, elementsAfter = 1)

        callbackInvocations.checkThatHas(
            CallbackInvocation.Status(NavigationStatusOrigin.SET_ROUTE),
        ) strictlyAfter CallbackInvocation.RoutesSet
    }

    @Test
    fun setRoutesData() = runBlocking(Dispatchers.Main.immediate) {
        navigator.addObserver(
            object : NavigatorObserver {
                override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
                    callbackInvocations.add(CallbackInvocation.Status(origin))
                }

                override fun onRoutesChanged(info: RoutesChangeInfo) {
                    // no-op
                }
            },
        )
        waitForStatusUpdatesToBegin()
        navigator.setRoutesData(
            SetRoutesDataParams(
                RouteParser.createRoutesData(
                    route.nativeRoute(),
                    listOf(routes[1].nativeRoute()),
                ),
                0,
            ),
            SetRoutesReason.NEW_ROUTE,
        ) {
            callbackInvocations.add(CallbackInvocation.RoutesDataSet)
        }

        callbackInvocations.waitUntilHas(CallbackInvocation.RoutesDataSet, elementsAfter = 1)

        callbackInvocations.checkThatHas(
            CallbackInvocation.Status(NavigationStatusOrigin.SET_ROUTE),
        ) strictlyAfter CallbackInvocation.RoutesDataSet
    }

    @Test
    fun refreshRoute() = runBlocking(Dispatchers.Main.immediate) {
        navigator.addObserver(
            object : NavigatorObserver {
                override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
                    callbackInvocations.add(CallbackInvocation.Status(origin))
                }

                override fun onRoutesChanged(info: RoutesChangeInfo) {
                    // no-op
                }
            },
        )
        navigator.setRoutesAndWaitForResult(
            SetRoutesParams(route.nativeRoute(), 0, emptyList<RouteInterface>()),
            SetRoutesReason.NEW_ROUTE,
        )
        // we will expect SET_ROUTE after refresh, clear the previous ones
        callbackInvocations.waitUntilHas(
            CallbackInvocation.Status(NavigationStatusOrigin.SET_ROUTE),
        )
        callbackInvocations.clear()

        waitForStatusUpdatesToBegin()
        navigator.refreshRoute(refreshedRouteResponse, route.id, 0) {
            callbackInvocations.add(CallbackInvocation.Refresh)
        }

        callbackInvocations.waitUntilHas(CallbackInvocation.Refresh, elementsAfter = 1)

        callbackInvocations.checkThatHas(
            CallbackInvocation.Status(NavigationStatusOrigin.ROUTE_REFRESH),
        ) strictlyAfter CallbackInvocation.Refresh
    }

    @Test
    fun changeLeg() = runBlocking(Dispatchers.Main.immediate) {
        navigator.setRoutesAndWaitForResult(
            SetRoutesParams(multilegRoute.nativeRoute(), 0, emptyList<RouteInterface>()),
            SetRoutesReason.NEW_ROUTE,
        )
        navigator.addObserver(
            object : NavigatorObserver {
                override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
                    callbackInvocations.add(CallbackInvocation.Status(origin))
                }

                override fun onRoutesChanged(info: RoutesChangeInfo) {
                    // no-op
                }
            },
        )
        waitForStatusUpdatesToBegin()

        navigator.changeLeg(1) {
            callbackInvocations.add(CallbackInvocation.LegChanged)
        }
        callbackInvocations.waitUntilHas(CallbackInvocation.LegChanged, elementsAfter = 1)

        callbackInvocations.checkThatHas(
            CallbackInvocation.Status(NavigationStatusOrigin.LEG_CHANGE),
        ) strictlyAfter CallbackInvocation.LegChanged
    }

    @Ignore("bump NN to 124.0.0 (includes NN-361)")
    @Test
    fun updateLocation() = runBlocking(Dispatchers.Main.immediate) {
        navigator.addObserver(
            object : NavigatorObserver {
                override fun onStatus(origin: NavigationStatusOrigin, status: NavigationStatus) {
                    callbackInvocations.add(CallbackInvocation.Status(origin, status.location))
                }

                override fun onRoutesChanged(info: RoutesChangeInfo) {
                    // no-op
                }
            },
        )
        waitForStatusUpdatesToBegin()

        val location = getSecondLocation()
        navigator.updateLocation(location) {
            callbackInvocations.add(CallbackInvocation.LocationUpdated)
        }
        callbackInvocations.waitUntilHas(CallbackInvocation.LocationUpdated, elementsAfter = 1)

        callbackInvocations.checkThatHas(
            CallbackInvocation.Status(NavigationStatusOrigin.LOCATION_UPDATE, location),
        ) strictlyAfter CallbackInvocation.LocationUpdated
    }

    private fun List<CallbackInvocation>.checkThatHas(
        hasWhat: CallbackInvocation,
    ): CallbackInvocationsCheckThatHasScope = CallbackInvocationsCheckThatHasScope(this, hasWhat)

    private infix fun CallbackInvocationsCheckThatHasScope.strictlyAfter(
        milestone: CallbackInvocation,
    ) {
        val index = actual.indexOf(milestone)
        val beforeMilestone = actual.take(index)
        val afterMilestone = actual[index + 1]
        assertTrue(
            "Statuses before $milestone should not contain $hasWhat, " +
                "actual: $this",
            beforeMilestone.none { it == hasWhat },
        )
        assertEquals(hasWhat, afterMilestone)
    }

    private fun getSecondLocation(): FixLocation {
        // second location on route
        val longitude = -121.496166
        val latitude = 38.577533
        return FixLocation(
            Point.fromLngLat(longitude, latitude),
            System.nanoTime(),
            Date(),
            5f,
            120f,
            12f,
            1f,
            "fused",
            1f,
            1f,
            1f,
            HashMap(),
            false,
        )
    }

    private suspend fun List<*>.waitUntilHasSize(size: Int) {
        waitUntilCondition { it.size >= size }
    }

    private suspend fun List<CallbackInvocation>.waitUntilHas(
        hasWhat: CallbackInvocation,
        elementsAfter: Int = 0,
    ) {
        waitUntilCondition {
            val indexOf = it.indexOf(hasWhat)
            indexOf != -1 && indexOf + elementsAfter <= it.lastIndex
        }
    }

    private suspend fun <T> List<T>.waitUntilCondition(condition: (List<T>) -> Boolean) {
        val list = this
        withTimeout(10000) {
            while (!condition(list)) {
                delay(50)
            }
        }
    }

    private suspend fun Navigator.setRoutesAndWaitForResult(
        params: SetRoutesParams,
        reason: SetRoutesReason,
    ) = suspendCancellableCoroutine<Unit> { cont ->
        setRoutes(
            params,
            reason,
        ) { cont.resume(Unit) }
    }

    private suspend fun waitForStatusUpdatesToBegin() {
        // 2 is not important: may be 1, may be 3, may be 10
        callbackInvocations.waitUntilHasSize(2)
    }
}

private data class CallbackInvocationsCheckThatHasScope(
    val actual: List<CallbackInvocation>,
    val hasWhat: CallbackInvocation,
)

private sealed class CallbackInvocation {

    data class Status(
        val origin: NavigationStatusOrigin,
        val location: FixLocation? = null,
    ) : CallbackInvocation()

    object RoutesSet : CallbackInvocation()
    object RoutesDataSet : CallbackInvocation()
    object Refresh : CallbackInvocation()
    object LegChanged : CallbackInvocation()
    object LocationUpdated : CallbackInvocation()
}
