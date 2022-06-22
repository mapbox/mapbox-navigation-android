package com.mapbox.navigation.instrumentation_tests.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.utils.getMapboxAccessTokenFromResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.net.URL
import kotlin.coroutines.resume

class GetRoutesTest {
@Test
fun requestManyRoutes() = sdkTest {
    val context = getApplicationContext<Context>()
    val navigationOptions = NavigationOptions.Builder(context)
        .accessToken(getMapboxAccessTokenFromResources(context))
        .build()
    val mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
    try {

        val url = URL(
            "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/11.5679995,48.1341107;13.37691,52.51604?access_token=****jgZw&geometries=polyline6&alternatives=true&overview=full&steps=true&bearings=332.4625244%2C45%3B&layers=0%3B&continue_straight=false&annotations=congestion%2Cclosure%2Cdistance%2Cspeed%2Cstate_of_charge&language=en&roundabout_exits=true&voice_instructions=true&banner_instructions=true&voice_units=imperial&enable_refresh=true&snapping_include_closures=true%3Btrue&max_width=2.2&metadata=true"
        )

        coroutineScope {
            repeat(20) {
                val options = RouteOptions.fromUrl(url)
                launch { val route = mapboxNavigation.requestRoutes(options).getSuccessfulResultOrThrowException() }
            }
        }
    } finally {
        mapboxNavigation.onDestroy()
    }
}

// HELPER FUNCTIONS WE USE IN OUR TESTS
fun sdkTest(
    timeout: Long = 60_000,
    block: suspend () -> Unit
) {
    runBlocking(Dispatchers.Main) {
        withTimeout(timeout) {
            block()
        }
    }
}

suspend fun MapboxNavigation.requestRoutes(options: RouteOptions) =
    suspendCancellableCoroutine<RouteRequestResult> { continuation ->
        val callback = object : NavigationRouterCallback {
            override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                continuation.resume(RouteRequestResult.Success(routes, routerOrigin))
            }

            override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                continuation.resume(RouteRequestResult.Failure(reasons))
            }

            override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            }
        }
        val id = requestRoutes(options, callback)
        continuation.invokeOnCancellation {
            cancelRouteRequest(id)
        }
    }

sealed class RouteRequestResult {
    data class Success(
        val routes: List<NavigationRoute>,
        val routerOrigin: RouterOrigin
    ) : RouteRequestResult()

    data class Failure(
        val reasons: List<RouterFailure>
    ) : RouteRequestResult()
}

fun RouteRequestResult.getSuccessfulResultOrThrowException(): RouteRequestResult.Success {
    return when (this) {
        is RouteRequestResult.Success -> this
        is RouteRequestResult.Failure -> error("result is failure: ${this.reasons}")
    }
}
}