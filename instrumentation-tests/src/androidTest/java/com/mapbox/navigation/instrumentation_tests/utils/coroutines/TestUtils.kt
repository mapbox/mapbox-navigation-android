package com.mapbox.navigation.instrumentation_tests.utils.coroutines

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val MAX_TIME_TO_UPDATE_ROUTE = 5_000L
private const val DEFAULT_TIMEOUT_FOR_SDK_TEST = 30_000L

fun sdkTest(
    timeout: Long = DEFAULT_TIMEOUT_FOR_SDK_TEST,
    block: suspend () -> Unit
) {
    runBlocking(Dispatchers.Main) {
        withTimeout(timeout) {
            block()
        }
    }
}

fun MapboxNavigation.clearNavigationRoutesAndWaitForUpdate() {
    val latch = CountDownLatch(1)
    val observer = object : RoutesObserver {
        override fun onRoutesChanged(result: RoutesUpdatedResult) {
            if (result.reason == RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP) {
                unregisterRoutesObserver(this)
                latch.countDown()
            }
        }
    }
    registerRoutesObserver(observer)
    setNavigationRoutes(emptyList())
    if (getNavigationRoutes().isEmpty()) {
        latch.countDown()
    }
    latch.await(MAX_TIME_TO_UPDATE_ROUTE, TimeUnit.MILLISECONDS)
}

suspend fun MapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes: List<NavigationRoute>) {
    if (routes.isEmpty()) {
        throw IllegalArgumentException(
            "For empty routes use `clearNavigationRoutesAndWaitForUpdate` instead"
        )
    }
    withTimeout(MAX_TIME_TO_UPDATE_ROUTE) {
        coroutineScope {
            launch {
                waitForNewRoute()
            }
            setNavigationRoutes(routes)
        }
    }
}

suspend fun MapboxNavigation.waitForNewRoute() {
    waitForRoutesUpdate(RoutesExtra.ROUTES_UPDATE_REASON_NEW)
}

private suspend fun MapboxNavigation.waitForRoutesUpdate(
    @RoutesExtra.RoutesUpdateReason reason: String
) {
    routesUpdates()
        .filter { it.reason == reason }
        .first()
}
