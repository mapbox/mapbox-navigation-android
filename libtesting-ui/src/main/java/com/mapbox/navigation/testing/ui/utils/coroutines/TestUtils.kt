package com.mapbox.navigation.testing.ui.utils.coroutines

import android.annotation.SuppressLint
import android.util.Log
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.RoutesSetCallback
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
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
    block: suspend CoroutineScope.() -> Unit
) {
    runBlocking(Dispatchers.Main.immediate) {
        withTimeout(timeout) {
            block()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun MapboxNavigation.executeActionAndWaitForSessionState(
    stateClass: Class<out NavigationSessionState>,
    action: MapboxNavigation.() -> Unit
) {
    suspendCancellableCoroutine<Unit?> {
        val observer = object : NavigationSessionStateObserver {
            override fun onNavigationSessionStateChanged(
                navigationSession: NavigationSessionState
            ) {
                if (navigationSession.javaClass == stateClass) {
                    unregisterNavigationSessionStateObserver(this)
                    it.resume(null) {}
                }
            }
        }
        it.invokeOnCancellation { unregisterNavigationSessionStateObserver(observer) }
        registerNavigationSessionStateObserver(observer)
        action()
    }
}

@SuppressLint("MissingPermission")
suspend fun MapboxNavigation.startTripSessionAndWaitForFreeDriveState() {
    check(getNavigationRoutes().isEmpty()) {
        "startTripSessionAndWaitForFreeDriveState should not be invoked " +
            "when routes have previously been set to MapboxNavigation"
    }

    executeActionAndWaitForSessionState(NavigationSessionState.FreeDrive::class.java) {
        startTripSession()
    }
}

@SuppressLint("MissingPermission")
suspend fun MapboxNavigation.startTripSessionAndWaitForActiveGuidanceState() {
    check(getNavigationRoutes().isNotEmpty()) {
        "startTripSessionAndWaitForActiveGuidanceState should only be invoked " +
            "when routes have previously been set to MapboxNavigation"
    }

    executeActionAndWaitForSessionState(NavigationSessionState.ActiveGuidance::class.java) {
        startTripSession()
    }
}

suspend fun MapboxNavigation.stopTripSessionAndWaitForIdleState() {
    executeActionAndWaitForSessionState(NavigationSessionState.Idle::class.java) {
        stopTripSession()
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
    routes: List<NavigationRoute>,
    initialLegIndex: Int = 0,
) =
    withTimeout(MAX_TIME_TO_UPDATE_ROUTE) {
        setNavigationRoutes(routes, initialLegIndex)
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

suspend fun MapboxNavigation.resetTripSessionAndWaitForResult() =
    suspendCancellableCoroutine<Unit> { cont ->
        resetTripSession { cont.resume(Unit) }
    }

inline fun <T> withLogOnTimeout(message: String, body: () -> T): T {
    try {
        return body()
    } catch (ce: TimeoutCancellationException) {
        Log.e("sdk-test", "timeout: $message")
        throw ce
    }
}
