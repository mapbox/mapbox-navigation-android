package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.NavigationRoutesParser
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.core.internal.router.mapToSdkRouterFailureType
import com.mapbox.navigation.navigator.internal.RerouteEventsProvider
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.CopyOnWriteArraySet

internal typealias UpdateRoutes = (List<NavigationRoute>, legIndex: Int) -> Boolean

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal class NativeMapboxRerouteController(
    rerouteEventsProvider: RerouteEventsProvider,
    private val rerouteController: RerouteControllerInterface,
    private val rerouteDetector: RerouteDetectorInterface,
    private val getCurrentRoutes: () -> List<NavigationRoute>,
    private val updateRoutes: UpdateRoutes,
    private val scope: CoroutineScope,
    private val routeParser: NavigationRoutesParser,
) : InternalRerouteController() {

    private val observers = CopyOnWriteArraySet<RerouteStateObserver>()
    private val observersV2 = CopyOnWriteArraySet<RerouteStateV2Observer>()

    /**
     * There's a private backing field for [state] so that it can become val
     * so that we don't accidentally update it instead of stateV2.
     */
    private var deprecatedState: RerouteState = RerouteState.Idle
        set(value) {
            if (field == value) {
                return
            }
            logD(TAG) { "RerouteState: $value" }
            field = value
            observers.forEach { it.onRerouteStateChanged(value) }
        }

    /*
     Backed by `deprecatedState`. Should not be updated directly - all the internal logic should switch to `stateV2`.
    */
    override val state: RerouteState
        get() = deprecatedState

    override var stateV2: RerouteStateV2 = RerouteStateV2.Idle()
        private set(value) {
            if (field == value) {
                return
            }
            logD(TAG) { "RerouteState: $value" }
            field = value
            value.toRerouteState()?.let {
                deprecatedState = it
            }
            observersV2.forEach { it.onRerouteStateChanged(value) }
        }

    private val nativeRerouteObserver = object : RerouteObserver {
        override fun onRerouteDetected(routeRequest: String): Boolean {
            logD(TAG) { "onRerouteDetected: $routeRequest" }
            stateV2 = RerouteStateV2.FetchingRoute()
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
                    is RerouteResponseParsingResult.Error -> {
                        stateV2 = RerouteStateV2.Failed(
                            "Error parsing route",
                            result.throwable,
                        )
                        stateV2 = RerouteStateV2.Idle()
                    }
                    is RerouteResponseParsingResult.RoutesAvailable -> {
                        stateV2 = RerouteStateV2.RouteFetched(
                            result.newRoutes.firstOrNull()?.origin.orEmpty(),
                        )
                        val routeAccepted = updateRoutes(
                            result.newRoutes,
                            result.primaryRouteLegIndex,
                        )
                        stateV2 = if (routeAccepted) {
                            RerouteStateV2.Deviation.ApplyingRoute()
                        } else {
                            RerouteStateV2.Deviation.RouteIgnored()
                        }
                        stateV2 = RerouteStateV2.Idle()
                    }
                }
            }
        }

        override fun onRerouteCancelled() {
            logD(TAG) { "onRerouteCancelled" }
            stateV2 = RerouteStateV2.Interrupted()
            stateV2 = RerouteStateV2.Idle()
        }

        override fun onRerouteFailed(error: RerouteError) {
            setRerouteFailureState("onRerouteFailed", error)
            stateV2 = RerouteStateV2.Idle()
        }

        override fun onSwitchToAlternative(route: RouteInterface, legIndex: Int) {
            logD(TAG) { "onSwitchToAlternative: ${route.routeId}" }
            val origin = route.routerOrigin.mapToSdkRouteOrigin()
            val routes = getCurrentRoutes().toMutableList()
            val routeToSwitchTo = routes.firstOrNull { it.id == route.routeId } ?: return
            stateV2 = RerouteStateV2.FetchingRoute()
            routes.remove(routeToSwitchTo)
            routes.add(0, routeToSwitchTo)
            stateV2 = RerouteStateV2.RouteFetched(origin)
            val routeAccepted = updateRoutes(routes, legIndex)
            stateV2 = if (routeAccepted) {
                RerouteStateV2.Deviation.ApplyingRoute()
            } else {
                RerouteStateV2.Deviation.RouteIgnored()
            }
            stateV2 = RerouteStateV2.Idle()
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
    override fun rerouteOnDeviation(callback: DeviationRoutesCallback) {
    }

    // TODO: https://mapbox.atlassian.net/browse/NAVAND-4496
    // test how route replan stops ongoing request
    override fun rerouteOnParametersChange(callback: RouteReplanRoutesCallback) {
        logI(TAG) { "Forcing reroute because of parameters change" }
        stateV2 = RerouteStateV2.FetchingRoute()
        rerouteDetector.forceReroute(ForceRerouteReason.PARAMETERS_CHANGE) {
            logD(TAG) {
                "Received force reroute on parameters change callback with ${it.value}, ${it.error}"
            }
            it.onValue { value ->
                scope.launch {
                    val parsingResult = handleRerouteResponse(
                        value.routeResponse,
                        value.routeRequest,
                        value.origin,
                    )
                    when (parsingResult) {
                        is RerouteResponseParsingResult.Error -> {
                            stateV2 = RerouteStateV2.Failed(
                                "Error parsing route",
                                parsingResult.throwable,
                            )
                            stateV2 = RerouteStateV2.Idle()
                        }
                        is RerouteResponseParsingResult.RoutesAvailable -> {
                            stateV2 = RerouteStateV2.RouteFetched(
                                parsingResult.newRoutes.firstOrNull()?.origin.orEmpty(),
                            )
                            stateV2 = RerouteStateV2.Idle()
                            callback.onNewRoutes(
                                RerouteResult(
                                    parsingResult.newRoutes,
                                    parsingResult.primaryRouteLegIndex,
                                    value.origin.mapToSdkRouteOrigin(),
                                ),
                            )
                        }
                    }
                }
            }.onError { rerouteError ->
                setRerouteFailureState("User triggered force reroute", rerouteError)
                stateV2 = RerouteStateV2.Idle()
            }
        }
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
        stateV2 = RerouteStateV2.FetchingRoute()
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
                        is RerouteResponseParsingResult.Error -> {
                            stateV2 = RerouteStateV2.Failed(
                                "Error parsing route",
                                parsingResult.throwable,
                            )
                            stateV2 = RerouteStateV2.Idle()
                        }
                        is RerouteResponseParsingResult.RoutesAvailable -> {
                            stateV2 = RerouteStateV2.RouteFetched(
                                parsingResult.newRoutes.firstOrNull()?.origin.orEmpty(),
                            )
                            stateV2 = RerouteStateV2.Idle()
                            callback.onNewRoutes(
                                parsingResult.newRoutes,
                                parsingResult.newRoutes.first().origin,
                            )
                        }
                    }
                }
            }.onError { rerouteError ->
                setRerouteFailureState("User triggered force reroute", rerouteError)
                stateV2 = RerouteStateV2.Idle()
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
        stateV2 = if (rerouteError.type == RerouteErrorType.CANCELLED) {
            RerouteStateV2.Interrupted()
        } else {
            RerouteStateV2.Failed(
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

    override fun unregisterRerouteStateV2Observer(
        rerouteStateObserver: RerouteStateV2Observer,
    ): Boolean = observersV2.remove(rerouteStateObserver)

    override fun registerRerouteStateV2Observer(
        rerouteStateObserver: RerouteStateV2Observer,
    ): Boolean {
        val result = observersV2.add(rerouteStateObserver)
        rerouteStateObserver.onRerouteStateChanged(stateV2)
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
        return routeParser.parseDirectionsResponse(
            DirectionsResponseToParse(
                routeResponse,
                routeRequest,
                origin.mapToSdkRouteOrigin(),
                responseOriginAPI = ResponseOriginAPI.DIRECTIONS_API,
            ),
        ).map {
            RerouteResponseParsingResult.RoutesAvailable(
                it.routes,
                0,
            )
        }.getOrElse {
            logE(TAG) { "error parsing route ${it.message}" }
            RerouteResponseParsingResult.Error(it)
        }
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

        data class Error(val throwable: Throwable) : RerouteResponseParsingResult()
    }

    companion object {
        private const val TAG = "NativeMapboxRerouteController"
    }
}
