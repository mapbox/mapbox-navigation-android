package com.mapbox.navigation.instrumentation_tests.utils.coroutines

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.RoutesSetCallback
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun MapboxNavigation.clearNavigationRoutesAndWaitForUpdate() =
    withTimeout(MAX_TIME_TO_UPDATE_ROUTE) {
        suspendCancellableCoroutine<Unit?> {
            val observer = object : RoutesObserver {
                override fun onRoutesChanged(result: RoutesUpdatedResult) {
                    if (result.reason == RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP) {
                        unregisterRoutesObserver(this)
                        it.resume(null) {}
                    }
                }
            }
            it.invokeOnCancellation { unregisterRoutesObserver(observer) }
            val hadRoutes = getNavigationRoutes().isNotEmpty()
            registerRoutesObserver(observer)
            setNavigationRoutes(emptyList())
            if (!hadRoutes) {
                unregisterRoutesObserver(observer)
                it.resume(null) {}
            }
        }
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

suspend fun MapboxNavigation.setNavigationRoutesAndAwaitError(
    routes: List<NavigationRoute>,
    legIndex: Int
) = withTimeout(MAX_TIME_TO_UPDATE_ROUTE) {
    suspendCancellableCoroutine<Unit?> { continuation ->
        val callback = RoutesSetCallback {
            if (it.isError) {
                continuation.resume(null)
            } else {
                continuation.resumeWithException(
                    IllegalStateException("Expected error, but got success")
                )
            }
        }
        setNavigationRoutes(routes, legIndex, callback)
    }
}

suspend fun MapboxNavigation.setNavigationRoutesAndWaitForAlternativesUpdate(
    routes: List<NavigationRoute>
) =
    withTimeout(MAX_TIME_TO_UPDATE_ROUTE) {
        setNavigationRoutes(routes)
        waitForAlternativeRoute()
    }

suspend fun MapboxNavigation.waitForNewRoute() {
    waitForRoutesUpdate(RoutesExtra.ROUTES_UPDATE_REASON_NEW)
}

suspend fun MapboxNavigation.waitForAlternativeRoute() {
    waitForRoutesUpdate(RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE)
}

private suspend fun MapboxNavigation.waitForRoutesUpdate(
    @RoutesExtra.RoutesUpdateReason reason: String
) {
    routesUpdates()
        .filter { it.reason == reason }
        .first()
}
