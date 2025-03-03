package com.mapbox.navigation.core.trip.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.base.internal.route.testing.createNavigationRouteForTest
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.mapToLocation
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.test.R
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.SetRoutesReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RunWith(AndroidJUnit4::class)
class ArtificialDriverTest {

    @Test
    @Ignore("test sometimes fails because of https://mapbox.atlassian.net/browse/NN-418")
    fun nativeNavigatorFollowsArtificialDriverWithoutReroutes() =
        runBlocking<Unit>(Dispatchers.Main) {
            withNavigators { mapboxNavigation ->
                mapboxNavigation.historyRecorder.startRecording()
                val testRoute = getTestRoute()
                val events = createArtificialLocationUpdates(testRoute)
                val setRoutesResult = mapboxNavigation.navigator.setRoutes(
                    testRoute,
                    reason = SetRoutesReason.NEW_ROUTE,
                )
                assertTrue("result is $setRoutesResult", setRoutesResult.isValue)
                val statusesTracking = async<List<NavigationStatus>> {
                    mapboxNavigation.navigator.collectStatuses(
                        untilRouteState = RouteState.COMPLETE,
                    )
                }

                for (location in events.map { it.location.mapToLocation() }) {
                    assertTrue(mapboxNavigation.navigator.updateLocation(location.toFixLocation()))
                }

                val states = statusesTracking.await()
                val historyFile = suspendCoroutine<String> { continuation ->
                    mapboxNavigation.historyRecorder.stopRecording {
                        continuation.resume(it ?: "null")
                    }
                }
                val offRouteState = states.filter { it.routeState == RouteState.OFF_ROUTE }
                assertTrue(
                    "${offRouteState.size} off-route states have been detected(" +
                        "more info in $historyFile): $offRouteState",
                    offRouteState.isEmpty(),
                )
            }
        }
}

private fun createArtificialLocationUpdates(
    testRoute: NavigationRoute,
): List<ReplayEventUpdateLocation> {
    val replayRouteMapper = ReplayRouteMapper()
    return replayRouteMapper
        .mapDirectionsRouteGeometry(testRoute.directionsRoute)
        .filterIsInstance<ReplayEventUpdateLocation>()
}

private suspend fun MapboxNativeNavigator.collectStatuses(
    untilRouteState: RouteState,
): MutableList<NavigationStatus> {
    val statues = mutableListOf<NavigationStatus>()
    statusUpdates()
        .map { it.status }
        .takeWhile { it.routeState != untilRouteState }
        .toList(statues)
    return statues
}

data class OnStatusUpdateParameters(
    val origin: NavigationStatusOrigin,
    val status: NavigationStatus,
)

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNativeNavigator.statusUpdates(): Flow<OnStatusUpdateParameters> {
    return callbackFlow {
        val observer = NavigatorObserver { origin, status ->
            this.trySend(OnStatusUpdateParameters(origin, status))
        }
        addNavigatorObserver(observer)
        awaitClose {
            removeNavigatorObserver(observer)
        }
    }
}

private suspend fun withNavigators(
    block: suspend (MapboxNavigation) -> Unit,
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val mapboxNavigation = MapboxNavigationProvider.create(
        NavigationOptions.Builder(context)
            .build(),
    )
    try {
        block(mapboxNavigation)
    } finally {
        mapboxNavigation.onDestroy()
    }
}

private fun getTestRoute(): NavigationRoute {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return createNavigationRouteForTest(
        directionsResponseJson = context.resources.openRawResource(R.raw.test_long_route)
            .readBytes().decodeToString(),
        routeRequestUrl = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
            "11.566744%2C48.143769%3B8.675521%2C50.119087" +
            "?alternatives=false" +
            "&geometries=polyline6" +
            "&language=en" +
            "&overview=full" +
            "&steps=true" +
            "&access_token=YOUR_MAPBOX_ACCESS_TOKEN",
        routerOrigin = RouterOrigin.ONLINE,
    ).first()
}
