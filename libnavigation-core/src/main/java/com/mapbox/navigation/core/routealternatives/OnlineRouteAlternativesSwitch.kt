package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.internal.route.DEFAULT_AVOID_MANEUVER_SECONDS_FOR_ROUTE_ALTERNATIVES
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin.Offboard
import com.mapbox.navigation.base.route.RouterOrigin.Onboard
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesSetStartedParams
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.core.internal.extensions.flowSetNavigationRoutesStarted
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.reroute.applyAvoidManeuvers
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val LOG_CATEGORY = "OnlineRouteAlternativesSwitch"

/***
 * This components replaces offline route by an online alternative when it's available. Under the
 * hood it's requesting routes in cycle from the current position until the first successful result.
 * Normally this job should be done by the SDK. The problem is that it currently relies on platform
 * reachability/connectivity notifications. However some environments
 * don't have working reachability/connectivity API. This class is a temporary workaround for
 * those environments until we reimplement route alternatives logic so that it handles
 * offline-online switch even if platform's reachability API doesn't work.
 *
 * To enable automatic online-offline switch functionality, attach an instance of this class
 * to [MapboxNavigation] using [MapboxNavigationApp.registerObserver], followed by
 * [MapboxNavigationApp.unregisterObserver] to disable automatic switch.
 *
 * Example:
 * ```kotlin
 * MapboxNavigationApp.registerObserver(OnlineRouteAlternativesSwitch())
 * ```
 *
 * If you don't use [MapboxNavigationApp], you can enable the switch logic by calling
 * [OnlineRouteAlternativesSwitch.onAttached] directly, and then
 * [OnlineRouteAlternativesSwitch.onDetached] to disable.
 *
 * Known limitations: this class doesn't use user provided router.
 * Warning: this is a temporary solution and will be removed in the next versions of the SDK.
 *
 * @param connectTimeoutMilliseconds - is forwarded to OkHttp as is
 * @param readTimeoutMilliseconds - is forwarded to OkHttp as is
 * @param minimumRetryInterval - defines an interval that limits how often routes will be requested.
 * Route requests won't happen more often than this interval.
 */
@ExperimentalMapboxNavigationAPI
class OnlineRouteAlternativesSwitch(
    private val connectTimeoutMilliseconds: Int = 10_000,
    private val readTimeoutMilliseconds: Int = 30_000,
    private val minimumRetryInterval: Int = 60_000,
    private val avoidManeuverSeconds: Int = DEFAULT_AVOID_MANEUVER_SECONDS_FOR_ROUTE_ALTERNATIVES
) : MapboxNavigationObserver {

    private lateinit var mapboxNavigationScope: CoroutineScope

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigationScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        val accessToken = mapboxNavigation.navigationOptions.accessToken ?: return
        mapboxNavigationScope.launch {
            requestOnlineRoutes(
                mapboxNavigation.flowRoutesUpdated()
                    .map { it.navigationRoutes },
                mapboxNavigation.flowLocationMatcherResult(),
                mapboxNavigation.flowRouteProgress(),
                mapboxNavigation.flowSetNavigationRoutesStarted(),
                routeRequestMechanism = { options ->
                    requestRoutes(
                        options,
                        accessToken,
                        MapboxNavigationAccounts,
                        connectTimeoutMilliseconds = connectTimeoutMilliseconds,
                        readTimeoutMilliseconds = readTimeoutMilliseconds,
                    )
                },
                minimumRetryInterval = minimumRetryInterval.toLong(),
                navigationRouteSerializationDispatcher = Dispatchers.Default,
                avoidManeuverSeconds = avoidManeuverSeconds
            )
                .collectLatest {
                    mapboxNavigation.setNavigationRoutes(it)
                }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigationScope.cancel()
    }
}

internal sealed class DirectionsRequestResult {
    data class SuccessfulResponse(val body: DirectionsResponse) : DirectionsRequestResult()
    sealed class ErrorResponse : DirectionsRequestResult() {
        object RetryableError : ErrorResponse()
        object NotRetryableError : ErrorResponse()
        data class RetryableErrorWithDelay(val delayMilliseconds: Long) : ErrorResponse()
    }
}

internal typealias RouteRequestMechanism = suspend (RouteOptions) -> DirectionsRequestResult

@OptIn(ExperimentalCoroutinesApi::class)
internal fun requestOnlineRoutes(
    routesUpdatedEvents: Flow<List<NavigationRoute>>,
    matchingResults: Flow<LocationMatcherResult>,
    routeProgressUpdates: Flow<RouteProgress>,
    setNavigaitonRoutesStartedEvents: Flow<RoutesSetStartedParams>,
    routeRequestMechanism: RouteRequestMechanism,
    minimumRetryInterval: Long,
    navigationRouteSerializationDispatcher: CoroutineDispatcher,
    avoidManeuverSeconds: Int
): Flow<List<NavigationRoute>> =
    merge(
        setNavigaitonRoutesStartedEvents.map {
            emptyList()
        },
        routesUpdatedEvents
    ).mapLatest {
        if (it.isEmpty()) {
            null
        } else {
            val primaryRoute = it.first()
            if (primaryRoute.origin == Onboard) {
                logI(LOG_CATEGORY) { "Current route is offline, requesting online routes" }
                requestOnlineRouteWithRetryOrNull(
                    matchingResults,
                    primaryRoute,
                    routeProgressUpdates,
                    routeRequestMechanism,
                    navigationRouteSerializationDispatcher,
                    minimumRetryInterval,
                    avoidManeuverSeconds,
                )
            } else {
                null
            }
        }
    }.filterNotNull()

private suspend fun requestOnlineRouteWithRetryOrNull(
    matchingResults: Flow<LocationMatcherResult>,
    primaryRoute: NavigationRoute,
    routeProgress: Flow<RouteProgress>,
    routeRequestMechanism: RouteRequestMechanism,
    navigationRouteSerializationDispatcher: CoroutineDispatcher,
    minimumRetryInterval: Long,
    avoidManeuverSeconds: Int
): List<NavigationRoute>? = coroutineScope {
    while (true) {
        val retryLimiter = launch { delay(minimumRetryInterval) }
        try {
            val latestMatchingResult = matchingResults.first()
            val latestRouteProgress = routeProgress.first()
            logI(LOG_CATEGORY) { "Requesting routes from ${latestMatchingResult.enhancedLocation}" }
            val newRouteOptions = updateRouteOptionsOrNull(
                primaryRoute,
                latestRouteProgress,
                latestMatchingResult
            )?.applyAvoidManeuvers(
                avoidManeuverSeconds,
                latestMatchingResult.enhancedLocation.speed
            )
            if (newRouteOptions == null) {
                logE(LOG_CATEGORY) {
                    "Error calculating route options for online route, will retry later"
                }
                retryLimiter.join()
                continue
            }

            val routeRequestResult = try {
                routeRequestMechanism(newRouteOptions)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                DirectionsRequestResult.ErrorResponse.RetryableError
            }
            when (routeRequestResult) {
                is DirectionsRequestResult.SuccessfulResponse -> {
                    val navigationRoutes = withContext(navigationRouteSerializationDispatcher) {
                        NavigationRoute.create(
                            routeRequestResult.body,
                            newRouteOptions,
                            Offboard
                        )
                    }
                    return@coroutineScope navigationRoutes
                }
                is DirectionsRequestResult.ErrorResponse.NotRetryableError -> {
                    logE(LOG_CATEGORY) {
                        "Not retryable error has occurred, " +
                            "won't retry until new offline route is set"
                    }
                    return@coroutineScope null
                }
                is DirectionsRequestResult.ErrorResponse.RetryableErrorWithDelay -> {
                    logI(LOG_CATEGORY) {
                        "Error requesting route. " +
                            "Applying additional delay of ${routeRequestResult.delayMilliseconds}" +
                            " milliseconds"
                    }
                    delay(routeRequestResult.delayMilliseconds)
                    retryLimiter.join()
                }
                else -> {
                    logI(LOG_CATEGORY) { "failed to receive an online route, will retry later" }
                    retryLimiter.join()
                }
            }
        } finally {
            retryLimiter.cancel()
        }
    }
    null
}

private fun updateRouteOptionsOrNull(
    primaryRoute: NavigationRoute,
    routeProgress: RouteProgress,
    locations: LocationMatcherResult
): RouteOptions? {
    val routeOptionsUpdateResult = RouteOptionsUpdater().update(
        primaryRoute.routeOptions,
        routeProgress,
        locations
    )
    val newRouteOptions = when (routeOptionsUpdateResult) {
        is RouteOptionsUpdater.RouteOptionsResult.Error -> {
            logE(LOG_CATEGORY) { "Can't update route options: ${routeOptionsUpdateResult.error}" }
            null
        }
        is RouteOptionsUpdater.RouteOptionsResult.Success -> {
            routeOptionsUpdateResult.routeOptions
        }
    }
    return newRouteOptions
}

private suspend fun requestRoutes(
    routeOptions: RouteOptions,
    accessToken: String,
    urlSkuTokenProvider: UrlSkuTokenProvider,
    connectTimeoutMilliseconds: Int,
    readTimeoutMilliseconds: Int
): DirectionsRequestResult {
    val mapboxDirections = MapboxDirections.builder()
        .routeOptions(routeOptions)
        .accessToken(accessToken)
        .interceptor {
            val httpUrl = it.request().url
            val skuUrl = urlSkuTokenProvider.obtainUrlWithSkuToken(httpUrl.toUrl())
            val request = it.request().newBuilder().url(skuUrl).build()
            it.withConnectTimeout(connectTimeoutMilliseconds, TimeUnit.MILLISECONDS)
                .withReadTimeout(readTimeoutMilliseconds, TimeUnit.MILLISECONDS)
                .proceed(request)
        }
        .build()
    logI(LOG_CATEGORY) {
        "Requesting online route: ${routeOptions.toUrl("***")}"
    }
    return suspendCancellableCoroutine { continuation ->
        mapboxDirections.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val body = response.body()
                val result = if (body != null && response.code() in 200..299) {
                    DirectionsRequestResult.SuccessfulResponse(body)
                } else {
                    logE(LOG_CATEGORY) {
                        "Error receiving routes ${response.code()}: ${response.message()}"
                    }
                    when (response.code()) {
                        in 400..499 -> {
                            DirectionsRequestResult.ErrorResponse.NotRetryableError
                        }
                        in 500..599 -> {
                            DirectionsRequestResult.ErrorResponse.RetryableErrorWithDelay(
                                60_000
                            )
                        }
                        else -> {
                            DirectionsRequestResult.ErrorResponse.RetryableError
                        }
                    }
                }
                continuation.resume(result)
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                logE(LOG_CATEGORY) {
                    "Error requesting routes: $t"
                }
                continuation.resume(DirectionsRequestResult.ErrorResponse.RetryableError)
            }
        })
        continuation.invokeOnCancellation {
            mapboxDirections.cancelCall()
        }
    }
}
