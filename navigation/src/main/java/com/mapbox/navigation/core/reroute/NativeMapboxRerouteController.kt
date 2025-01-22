package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.internal.utils.parseDirectionsResponse
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.performance.RouteParsingTracking
import com.mapbox.navigation.core.internal.router.mapToSdkRouterFailureType
import com.mapbox.navigation.navigator.internal.RerouteEventsProvider
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.ForceRerouteReason
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteDetectorInterface
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteErrorType
import com.mapbox.navigator.RerouteObserver
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteOptionsAdapter
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterOrigin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.CopyOnWriteArraySet

internal typealias UpdateRoutes = (List<NavigationRoute>, legIndex: Int) -> Unit

internal class NativeMapboxRerouteController(
    rerouteEventsProvider: RerouteEventsProvider,
    private val rerouteController: RerouteControllerInterface,
    private val rerouteDetector: RerouteDetectorInterface,
    private val getCurrentRoutes: () -> List<NavigationRoute>,
    private val updateRoutes: UpdateRoutes,
    private val scope: CoroutineScope,
    private val parsingDispatcher: CoroutineDispatcher,
    private val routeParsingTracking: RouteParsingTracking,
) : InternalRerouteController() {

    private val observers = CopyOnWriteArraySet<RerouteStateObserver>()
    override var state: RerouteState = RerouteState.Idle
        private set(value) {
            if (field == value) {
                return
            }
            logD(TAG) { "RerouteState: $value" }
            field = value
            observers.forEach { it.onRerouteStateChanged(value) }
        }

    private val nativeRerouteObserver = object : RerouteObserver {
        override fun onRerouteDetected(routeRequest: String): Boolean {
            logD(TAG) { "onRerouteDetected: $routeRequest" }
            state = RerouteState.FetchingRoute
            return true
        }

        override fun onRerouteReceived(
            routeResponse: DataRef,
            routeRequest: String,
            origin: RouterOrigin,
        ) {
            logD(TAG) { "onRerouteReceived: request: $routeRequest" }
            scope.launch {
                logD(TAG) { "Parsing reroute response $routeRequest" }
                when (val result = handleRerouteResponse(routeResponse, routeRequest, origin)) {
                    RerouteResponseParsingResult.Error -> {}
                    is RerouteResponseParsingResult.RoutesAvailable ->
                        updateRoutes(result.newRoutes, result.primaryRouteLegIndex)
                }
            }
        }

        override fun onRerouteCancelled() {
            logD(TAG) { "onRerouteCancelled" }
            state = RerouteState.Interrupted
            state = RerouteState.Idle
        }

        override fun onRerouteFailed(error: RerouteError) {
            setRerouteFailureState("onRerouteFailed", error)
            state = RerouteState.Idle
        }

        override fun onSwitchToAlternative(route: RouteInterface, legIndex: Int) {
            logD(TAG) { "onSwitchToAlternative: ${route.routeId}" }
            val origin = route.routerOrigin.mapToSdkRouteOrigin()
            val routes = getCurrentRoutes().toMutableList()
            val routeToSwitchTo = routes.firstOrNull { it.id == route.routeId } ?: return
            state = RerouteState.FetchingRoute
            routes.remove(routeToSwitchTo)
            routes.add(0, routeToSwitchTo)
            state = RerouteState.RouteFetched(origin)
            updateRoutes(routes, legIndex)
            state = RerouteState.Idle
        }
    }

    init {
        logD(TAG) { "Registering native reroute observer" }
        rerouteEventsProvider.addRerouteObserver(nativeRerouteObserver)
    }

    @Deprecated("native reroute controller interrupts reroute without external help")
    override fun interrupt() {
    }

    @Deprecated("native reroute controller identify reroute without external help")
    override fun rerouteOnDeviation(callback: RoutesCallback) {
    }

    // TODO: https://mapbox.atlassian.net/browse/NAVAND-4496
    // test how route replan stops ongoing request
    override fun rerouteOnParametersChange(callback: RoutesCallback) {
        logI(TAG) { "Forcing reroute because of parameters change" }
        rerouteDetector.forceReroute(ForceRerouteReason.PARAMETERS_CHANGE)
    }

    override fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        rerouteController.setOptionsAdapter(
            rerouteOptionsAdapter?.let {
                RerouteOptionsAdapterWrapper(
                    it,
                )
            },
        )
    }

    override fun reroute(callback: RerouteController.RoutesCallback) {
        logI(TAG) { "Forcing reroute because of user request" }
        state = RerouteState.FetchingRoute
        rerouteDetector.forceReroute(ForceRerouteReason.USER_TRIGGERED) {
            logD(TAG) { "Received force reroute callback with ${it.value}, ${it.error}" }
            it.onValue { value ->
                scope.launch {
                    val parsingResult = handleRerouteResponse(
                        value.routeResponse,
                        value.routeRequest,
                        value.origin,
                    )
                    when (parsingResult) {
                        RerouteResponseParsingResult.Error -> {}
                        is RerouteResponseParsingResult.RoutesAvailable ->
                            callback.onNewRoutes(
                                parsingResult.newRoutes,
                                parsingResult.newRoutes.first().origin,
                            )
                    }
                }
            }.onError { rerouteError ->
                setRerouteFailureState("User triggered force reroute", rerouteError)
                state = RerouteState.Idle
            }
        }
    }

    private fun setRerouteFailureState(tag: String, rerouteError: RerouteError) {
        logE(TAG) {
            "$tag. " +
                "type: ${rerouteError.type}; " +
                "message: ${rerouteError.message}; " +
                "router errors: ${rerouteError.routerErrors}"
        }
        state = if (rerouteError.type == RerouteErrorType.CANCELLED) {
            RerouteState.Interrupted
        } else {
            RerouteState.Failed(
                message = rerouteError.message,
                throwable = null,
                reasons = rerouteError.routerErrors
                    .filter { it.type != RouterErrorType.REQUEST_CANCELLED }
                    .map {
                        RouterFailureFactory.create(
                            url = URL(it.url),
                            routerOrigin = it.routerOrigin.mapToSdkRouteOrigin(),
                            message = it.message,
                            type = it.type.mapToSdkRouterFailureType(),
                            throwable = null,
                            isRetryable = it.isRetryable,
                        )
                    },
            )
        }
    }

    override fun registerRerouteStateObserver(
        rerouteStateObserver: RerouteStateObserver,
    ): Boolean {
        val result = observers.add(rerouteStateObserver)
        rerouteStateObserver.onRerouteStateChanged(state)
        return result
    }

    override fun unregisterRerouteStateObserver(
        rerouteStateObserver: RerouteStateObserver,
    ): Boolean = observers.remove(rerouteStateObserver)

    private suspend fun handleRerouteResponse(
        routeResponse: DataRef,
        routeRequest: String,
        origin: RouterOrigin,
    ): RerouteResponseParsingResult {
        return parseDirectionsResponse(
            parsingDispatcher,
            routeResponse,
            routeRequest,
            origin.mapToSdkRouteOrigin(),
            Time.SystemClockImpl.millis(),
        ).fold(
            {
                logE(TAG) { "error parsing route ${it.message}" }
                state = RerouteState.Failed(
                    "Error parsing route",
                    it,
                )
                state = RerouteState.Idle
                RerouteResponseParsingResult.Error
            },
            {
                state = RerouteState.RouteFetched(it.routes.firstOrNull()?.origin.orEmpty())
                state = RerouteState.Idle
                routeParsingTracking.routeResponseIsParsed(it.meta)
                RerouteResponseParsingResult.RoutesAvailable(it.routes, 0)
            },
        )
    }

    @VisibleForTesting
    internal class RerouteOptionsAdapterWrapper(
        private val origin: RerouteOptionsAdapter,
    ) : RouteOptionsAdapter {
        override fun modifyRouteRequestOptions(urlOptions: String): String {
            val originalUrl = try {
                URL(urlOptions)
            } catch (e: MalformedURLException) {
                logE(TAG) {
                    "modifyRouteRequestOptions: Can't parse $urlOptions to url, ${e.message}"
                }
                return urlOptions
            }
            val routeOptions = try {
                RouteOptions.fromUrl(originalUrl)
            } catch (t: Throwable) {
                logE(TAG) { "Error parsing to platform route options: $originalUrl" }
                return urlOptions
            }
            logI(TAG) {
                "Adopting request url. Original url is ${routeOptions.toUrl("***")}"
            }
            val adoptedRouteOptions = origin.onRouteOptions(routeOptions)
                // NN will override access token with MapboxOptions:accessToken
                .toUrl("***")
                .toString()
            logI(TAG) { "Adopted request url is $adoptedRouteOptions" }
            return adoptedRouteOptions
        }
    }

    private sealed class RerouteResponseParsingResult {
        data class RoutesAvailable(
            val newRoutes: List<NavigationRoute>,
            val primaryRouteLegIndex: Int,
        ) : RerouteResponseParsingResult()

        object Error : RerouteResponseParsingResult()
    }

    companion object {
        private const val TAG = "NativeMapboxRerouteController"
    }
}
