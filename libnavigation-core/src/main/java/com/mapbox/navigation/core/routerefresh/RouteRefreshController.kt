package com.mapbox.navigation.core.routerefresh

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.internal.time.parseISO8601DateToLocalTimeOrNull
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.RouteRefreshRequestDataProvider
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.resume

/**
 * This class is responsible for refreshing the current direction route's traffic.
 * This does not support alternative routes.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteRefreshController(
    private val routeRefreshOptions: RouteRefreshOptions,
    private val routeRefresh: RouteRefresh,
    private val routeRefreshRequestDataProvider: RouteRefreshRequestDataProvider,
    private val routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
    private val localDateProvider: () -> Date,
) {

    private var state: RouteRefreshStateResult? = null
        set(value) {
            if (field == value) return
            field = value
            value?.let { nonNullValue ->
                observers.forEach {
                    it.onNewState(nonNullValue)
                }
            }
        }

    private val observers = CopyOnWriteArraySet<RouteRefreshStatesObserver>()

    internal companion object {
        @VisibleForTesting
        internal const val LOG_CATEGORY = "RouteRefreshController"

        @VisibleForTesting
        internal const val FAILED_ATTEMPTS_TO_INVALIDATE_EXPIRING_DATA = 3
    }

    suspend fun refresh(routes: List<NavigationRoute>): RefreshedRouteInfo {
        try {
            return if (routes.isNotEmpty()) {
                val routesValidationResults = routes.map { validateRoute(it) }
                if (routesValidationResults.any { it is RouteValidationResult.Valid }) {
                    tryRefreshingRoutesUntilRouteChanges(routes)
                } else {
                    val message = joinValidationErrorMessages(routesValidationResults, routes)
                    onNewState(
                        RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED,
                        "No routes which could be refreshed. $message"
                    )
                    waitForever("No routes which could be refreshed. $message")
                }
            } else {
                resetState()
                waitForever("routes are empty")
            }
        } catch (e: CancellationException) {
            onNewStateIfCurrentIs(
                RouteRefreshExtra.REFRESH_STATE_CANCELED,
                current = RouteRefreshExtra.REFRESH_STATE_STARTED,
            )
            resetState()
            throw e
        }
    }

    fun registerRouteRefreshStateObserver(observer: RouteRefreshStatesObserver) {
        observers.add(observer)
        state?.let { observer.onNewState(it) }
    }

    fun unregisterRouteRefreshStateObserver(observer: RouteRefreshStatesObserver) {
        observers.remove(observer)
    }

    fun unregisterAllRouteRefreshStateObservers() {
        observers.clear()
    }

    private fun onNewState(
        @RouteRefreshExtra.RouteRefreshState state: String,
        message: String? = null
    ) {
        this.state = RouteRefreshStateResult(state, message)
    }

    private fun onNewStateIfCurrentIs(
        @RouteRefreshExtra.RouteRefreshState state: String,
        message: String? = null,
        @RouteRefreshExtra.RouteRefreshState current: String,
    ) {
        if (current == this.state?.state) {
            onNewState(state, message)
        }
    }

    private fun resetState() {
        this.state = null
    }

    private fun joinValidationErrorMessages(
        routeValidation: List<RouteValidationResult>,
        routes: List<NavigationRoute>
    ): String = routeValidation.filterIsInstance<RouteValidationResult.Invalid>()
        .mapIndexed { index, validation -> "${routes[index].id} ${validation.reason}" }
        .joinToString(separator = ". ")

    private suspend fun tryRefreshingRoutesUntilRouteChanges(
        initialRoutes: List<NavigationRoute>
    ): RefreshedRouteInfo {
        while (true) {
            val refreshed = refreshRoutesWithRetry(initialRoutes)
            if (refreshed.routes != initialRoutes) {
                return refreshed
            }
        }
    }

    private suspend fun refreshRoutesWithRetry(
        routes: List<NavigationRoute>
    ): RefreshedRouteInfo = coroutineScope {
        var timeUntilNextAttempt = async { delay(routeRefreshOptions.intervalMillis) }
        try {
            repeat(FAILED_ATTEMPTS_TO_INVALIDATE_EXPIRING_DATA) {
                timeUntilNextAttempt.await()
                if (it == 0) {
                    onNewState(RouteRefreshExtra.REFRESH_STATE_STARTED)
                }
                timeUntilNextAttempt = async { delay(routeRefreshOptions.intervalMillis) }
                val routeRefreshRequestData = routeRefreshRequestDataProvider
                    .getRouteRefreshRequestDataOrWait()
                val refreshedRoutes = refreshRoutesOrNull(routes, routeRefreshRequestData)
                if (refreshedRoutes.any { it != null }) {
                    onNewState(RouteRefreshExtra.REFRESH_STATE_FINISHED_SUCCESS)
                    return@coroutineScope RefreshedRouteInfo(
                        refreshedRoutes.mapIndexed { index, navigationRoute ->
                            navigationRoute ?: routes[index]
                        },
                        routeRefreshRequestData
                    )
                }
            }
        } finally {
            timeUntilNextAttempt.cancel() // otherwise current coroutine will wait for its child
        }
        onNewState(RouteRefreshExtra.REFRESH_STATE_FINISHED_FAILED)
        val requestData = routeRefreshRequestDataProvider.getRouteRefreshRequestDataOrWait()
        RefreshedRouteInfo(
            routes.map { removeExpiringDataFromRoute(it, requestData.legIndex) },
            requestData
        )
    }

    private fun removeExpiringDataFromRoute(
        route: NavigationRoute,
        currentLegIndex: Int,
    ): NavigationRoute {
        val routeLegs = route.directionsRoute.legs()
        return route.update(
            {
                toBuilder().legs(
                    routeLegs?.mapIndexed { legIndex, leg ->
                        val legHasAlreadyBeenPassed = legIndex < currentLegIndex
                        if (legHasAlreadyBeenPassed) {
                            leg
                        } else {
                            removeExpiredDataFromLeg(leg)
                        }
                    }
                ).build()
            },
            { this }
        )
    }

    private fun removeExpiredDataFromLeg(leg: RouteLeg) =
        leg.toBuilder()
            .annotation(
                leg.annotation()?.toBuilder()
                    ?.congestion(leg.annotation()?.congestion()?.map { "unknown" })
                    ?.congestionNumeric(
                        leg.annotation()?.congestionNumeric()?.map { null }
                    )
                    ?.build()
            )
            .incidents(
                leg.incidents()?.filter {
                    val parsed = parseISO8601DateToLocalTimeOrNull(it.endTime())
                        ?: return@filter true
                    val currentDate = localDateProvider()
                    parsed > currentDate
                }
            )
            .build()

    private suspend fun refreshRouteOrNull(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData,
    ): NavigationRoute? {
        val validationResult = validateRoute(route)
        if (validationResult is RouteValidationResult.Invalid) {
            logI("route ${route.id} can't be refreshed because ${validationResult.reason}")
            return null
        }
        return when (val result = requestRouteRefresh(route, routeRefreshRequestData)) {
            is RouteRefreshResult.Fail -> {
                logE(
                    "Route refresh error: ${result.error.message} " +
                        "throwable=${result.error.throwable}",
                    LOG_CATEGORY
                )
                null
            }
            is RouteRefreshResult.Success -> {
                logI("Received refreshed route ${result.route.id}", LOG_CATEGORY)
                logRoutesDiff(
                    newRoute = result.route,
                    oldRoute = route,
                    currentLegIndex = routeRefreshRequestData.legIndex
                )
                result.route
            }
        }
    }

    private suspend fun refreshRoutesOrNull(
        routes: List<NavigationRoute>,
        routeRefreshRequestData: RouteRefreshRequestData,
    ): List<NavigationRoute?> {
        return coroutineScope {
            routes.map { route ->
                async {
                    withTimeoutOrNull(routeRefreshOptions.intervalMillis) {
                        refreshRouteOrNull(route, routeRefreshRequestData)
                    }
                }
            }.awaitAll()
        }
    }

    private fun logRoutesDiff(
        newRoute: NavigationRoute,
        oldRoute: NavigationRoute,
        currentLegIndex: Int,
    ) {
        val routeDiffs = routeDiffProvider.buildRouteDiffs(
            oldRoute,
            newRoute,
            currentLegIndex,
        )
        if (routeDiffs.isEmpty()) {
            logI("No changes in annotations for route ${newRoute.id}", LOG_CATEGORY)
        } else {
            for (diff in routeDiffs) {
                logI(diff, LOG_CATEGORY)
            }
        }
    }

    private suspend fun requestRouteRefresh(
        route: NavigationRoute,
        routeRefreshRequestData: RouteRefreshRequestData
    ): RouteRefreshResult =
        suspendCancellableCoroutine { continuation ->
            val requestId = routeRefresh.requestRouteRefresh(
                route,
                routeRefreshRequestData,
                object : NavigationRouterRefreshCallback {
                    override fun onRefreshReady(route: NavigationRoute) {
                        continuation.resume(RouteRefreshResult.Success(route))
                    }

                    override fun onFailure(error: NavigationRouterRefreshError) {
                        continuation.resume(RouteRefreshResult.Fail(error))
                    }
                }
            )
            continuation.invokeOnCancellation {
                routeRefresh.cancelRouteRefreshRequest(requestId)
            }
        }

    private suspend fun <T> waitForever(message: String): T {
        logI("Route won't be refreshed because $message", LOG_CATEGORY)
        return CompletableDeferred<T>().await()
    }

    private fun validateRoute(route: NavigationRoute): RouteValidationResult = when {
        route.routeOptions.enableRefresh() != true ->
            RouteValidationResult.Invalid("RouteOptions#enableRefresh is false")
        route.directionsRoute.requestUuid()?.isNotBlank() != true ->
            RouteValidationResult.Invalid(
                "DirectionsRoute#requestUuid is blank. " +
                    "This can be caused by a route being generated by " +
                    "an Onboard router (in offline mode). " +
                    "Make sure to switch to an Offboard route when possible, " +
                    "only Offboard routes support the refresh feature."
            )
        else -> RouteValidationResult.Valid
    }

    private sealed class RouteValidationResult {
        object Valid : RouteValidationResult()
        data class Invalid(val reason: String) : RouteValidationResult()
    }

    private sealed class RouteRefreshResult {
        data class Success(val route: NavigationRoute) : RouteRefreshResult()
        data class Fail(val error: NavigationRouterRefreshError) : RouteRefreshResult()
    }
}
