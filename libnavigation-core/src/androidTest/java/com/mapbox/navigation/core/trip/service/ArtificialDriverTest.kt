package com.mapbox.navigation.core.trip.service

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.mapToLocation
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.test.R
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtificialDriverTest {

    @Test
    fun testFollowingRoute() = runBlocking<Unit>(Dispatchers.Main) {
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
        getNativeNavigator { nativeNavigator ->
            val testRoute = getTestRoute()
            val replayRouteMapper = ReplayRouteMapper()
            val events = replayRouteMapper.mapDirectionsRouteGeometry(testRoute.directionsRoute)
            val result = nativeNavigator.setRoutes(testRoute, reason = SetRoutesReason.NEW_ROUTE)
            assertTrue("result is $result", result.isValue)
            val states = mutableListOf<NavigationStatus>()
            val statusTracking = launch {
                nativeNavigator.statusUpdates().collect {
                    states.add(it.second)
                }
            }
            for (event in events.filterIsInstance<ReplayEventUpdateLocation>()) {
                val location = event.location.mapToLocation(event.eventTimestamp)
                assertTrue(nativeNavigator.updateLocation(location.toFixLocation()))
            }
            statusTracking.cancel()
            assertTrue(states.all { it.routeState == RouteState.TRACKING || it.routeState == RouteState.COMPLETE })
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun MapboxNativeNavigator.statusUpdates(): Flow<Pair<NavigationStatusOrigin, NavigationStatus>> {
        return callbackFlow {
            val observer = NavigatorObserver { origin, status ->
                Log.d("vadzim-debug", "$origin, $status")
                this.trySend(Pair(origin, status))
            }
            addNavigatorObserver(observer)
            awaitClose {
                removeNavigatorObserver(observer)
            }
        }
    }

    private suspend fun getNativeNavigator(block: suspend (MapboxNativeNavigator) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(context)
                .accessToken(context.getString(R.string.mapbox_access_token))
                .build()
        )
        block(MapboxNativeNavigatorImpl)
        mapboxNavigation.onDestroy()
    }

    private fun getTestRoute(): NavigationRoute {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return DirectionsRoute.fromJson(
            context.resources.openRawResource(R.raw.multileg_route)
                .readBytes().decodeToString()
        ).toNavigationRoute(RouterOrigin.Custom())
    }
}