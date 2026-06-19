package com.mapbox.navigation.core.reroute

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRoutesParser
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.core.internal.router.mapToSdkRouterFailureType
import com.mapbox.navigation.core.reroute.internal.NativeRerouteControllerState
import com.mapbox.navigation.core.utils.ThreadUtils
import com.mapbox.navigation.navigator.internal.MapboxNativeRerouteInterface
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigator.ForceRerouteReason
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteDetectorInterface
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteErrorType
import com.mapbox.navigator.RerouteInfo
import com.mapbox.navigator.RerouteObserver
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteOptionsAdapter
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterOrigin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.CopyOnWriteArraySet

internal typealias UpdateRoutes = (List<NavigationRoute>, legIndex: Int) -> Boolean

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal class NativeMapboxRerouteController(
    private val rerouteInterface: MapboxNativeRerouteInterface,
    private val getCurrentRoutes: () -> List<NavigationRoute>,
    private val updateRoutes: UpdateRoutes,
    private val scope: CoroutineScope,
    private val routeParser: NavigationRoutesParser,
    private val mainThreadAssertion: () -> Unit = ThreadUtils::assertCurrentLooperIsMain,
) : InternalRerouteController() {

    private var activeParsingJob: Job? = null
    private var rerouteOptionsAdapter: RerouteOptionsAdapter? = null
    private var rerouteController = rerouteInterface.getRerouteController()
    private var rerouteDetector = rerouteInterface.getRerouteDetector()
    private val observers = CopyOnWriteArraySet<RerouteStateObserver>()
    private val observersV2 = CopyOnWriteArraySet<RerouteStateV2Observer>()
    private var isEnabled = false

    private fun requireNativeRerouteController(): RerouteControllerInterface {
        return rerouteController
            ?: throw IllegalStateException("Native reroute controller is null")
    }

    private fun requireNativeRerouteDetector(): RerouteDetectorInterface {
        if (rerouteDetector == null) {
            throw IllegalStateException("Native reroute detector is null")
        }
        return rerouteDetector!!
    }

    /**
     * There's a private backing field for [state] so that it can become val
     * so that we don't accidentally update it instead of nativeState.
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
     Backed by `deprecatedState`. Should not be updated directly - all the internal logic should switch to `nativeState`.
    */
    override val state: RerouteState
        get() = deprecatedState

    private val _nativeControllerStateFlow =
        MutableStateFlow<NativeRerouteControllerState>(NativeRerouteControllerState.Idle())

    val nativeControllerStateFlow: StateFlow<NativeRerouteControllerState> =
        _nativeControllerStateFlow.asStateFlow()

    private var nativeState: NativeRerouteControllerState
        get() = _nativeControllerStateFlow.value
        set(value) {
            if (_nativeControllerStateFlow.value == value) {
                return
            }
            val prevV2 = _nativeControllerStateFlow.value.toRerouteStateV2()
            logD(TAG) { "NativeRerouteControllerState: $value" }
            _nativeControllerStateFlow.value = value
            val v2 = value.toRerouteStateV2()
            v2.toRerouteState()?.let {
                deprecatedState = it
            }
            if (prevV2 != v2) {
                observersV2.forEach { it.onRerouteStateChanged(v2) }
            }
        }

    override val stateV2: RerouteStateV2
        get() = nativeState.toRerouteStateV2()

    private val nativeRerouteObserver = object : RerouteObserver {
        override fun onRerouteDetected(routeRequest: String): Boolean {
            mainThreadAssertion()
            logD(TAG) { "onRerouteDetected: $routeRequest" }
            nativeState = NativeRerouteControllerState.WaitingForResponse()
            return true
        }

        override fun onRerouteReceived(
            routeResponse: DataRef,
            routeRequest: String,
            origin: RouterOrigin,
        ) {
            mainThreadAssertion()
            logD(TAG) { "onRerouteReceived: request: $routeRequest" }
            interruptParsingIfAny()
            activeParsingJob = scope.launch {
                logD(TAG) { "Parsing reroute response $routeRequest" }
                nativeState = NativeRerouteControllerState.RouteObjectsParsing()
                when (val result = handleRerouteResponse(routeResponse, routeRequest, origin)) {
                    is RerouteResponseParsingResult.Error -> {
                        nativeState = NativeRerouteControllerState.Failed(
                            "Error parsing route",
                            result.throwable,
                        )
                        nativeState = NativeRerouteControllerState.Idle()
                    }
                    is RerouteResponseParsingResult.RoutesAvailable -> {
                        nativeState = NativeRerouteControllerState.RouteFetched(
                            result.newRoutes.firstOrNull()?.origin.orEmpty(),
                        )
                        val routeAccepted = updateRoutes(
                            result.newRoutes,
                            result.primaryRouteLegIndex,
                        )
                        nativeState = if (routeAccepted) {
                            NativeRerouteControllerState.Deviation.ApplyingRoute()
                        } else {
                            NativeRerouteControllerState.Deviation.RouteIgnored()
                        }
                        nativeState = NativeRerouteControllerState.Idle()
                    }
                }
            }
        }

        override fun onRerouteCancelled() {
            mainThreadAssertion()
            logD(TAG) { "onRerouteCancelled" }
            nativeState = NativeRerouteControllerState.Interrupted()
            nativeState = NativeRerouteControllerState.Idle()
        }

        override fun onRerouteFailed(error: RerouteError) {
            mainThreadAssertion()
            nativeState = NativeRerouteControllerState.WaitingForResponse()
            setRerouteFailureState("onRerouteFailed", error)
            nativeState = NativeRerouteControllerState.Idle()
        }

        override fun onSwitchToAlternative(route: RouteInterface, legIndex: Int) {
            mainThreadAssertion()
            logD(TAG) { "onSwitchToAlternative: ${route.routeId}" }
            val origin = route.routerOrigin.mapToSdkRouteOrigin()
            val routes = getCurrentRoutes().toMutableList()
            val routeToSwitchTo = routes.firstOrNull { it.id == route.routeId } ?: return
            nativeState = NativeRerouteControllerState.WaitingForResponse()
            routes.remove(routeToSwitchTo)
            routes.add(0, routeToSwitchTo)
            nativeState = NativeRerouteControllerState.RouteFetched(origin)
            val routeAccepted = updateRoutes(routes, legIndex)
            nativeState = if (routeAccepted) {
                NativeRerouteControllerState.Deviation.ApplyingRoute()
            } else {
                NativeRerouteControllerState.Deviation.RouteIgnored()
            }
            nativeState = NativeRerouteControllerState.Idle()
        }
    }

    init {
        logD(TAG) { "Registering native reroute observer" }
        rerouteInterface.addRerouteObserver(nativeRerouteObserver)
        isEnabled = true
        // Re-register the reroute observer when the navigator is recreated
        rerouteInterface.addNativeNavigatorRecreationObserver {
            if (isEnabled) {
                logD(TAG) { "Navigator recreated - re-registering native reroute observer" }
                rerouteInterface.addRerouteObserver(nativeRerouteObserver)
            }
            rerouteController = rerouteInterface.getRerouteController()
            rerouteDetector = rerouteInterface.getRerouteDetector()
            rerouteOptionsAdapter?.let {
                setRerouteOptionsAdapter(it)
            }
        }
    }

    /**
     * Native reroute controller is disabled when there is no observers
     */
    override fun setEnabled(enabled: Boolean) {
        mainThreadAssertion()
        val rerouteController = requireNativeRerouteController()
        logD(TAG) { "Set reroute controller enabled = $enabled" }
        if (enabled) {
            // Avoid redundant observers in the case of subsequent calls to enable
            if (!isEnabled) {
                rerouteInterface.addRerouteObserver(nativeRerouteObserver)
            }
        } else {
            rerouteController.cancel()
            rerouteInterface.removeRerouteObserver(nativeRerouteObserver)
        }
        isEnabled = enabled
    }

    @Deprecated("native reroute controller interrupts reroute without external help")
    override fun interrupt() {
    }

    @Deprecated("native reroute controller identify reroute without external help")
    override fun rerouteOnDeviation(callback: DeviationRoutesCallback) {
    }

    override fun rerouteOnParametersChange(callback: RouteReplanRoutesCallback) {
        mainThreadAssertion()
        logI(TAG) { "Forcing reroute because of parameters change" }
        forceRerouteAndParse(ForceRerouteReason.PARAMETERS_CHANGE) { value, parsingResult ->
            callback.onNewRoutes(
                RerouteResult(
                    parsingResult.newRoutes,
                    parsingResult.primaryRouteLegIndex,
                    value.origin.mapToSdkRouteOrigin(),
                ),
            )
        }
    }

    override fun setRerouteOptionsAdapter(rerouteOptionsAdapter: RerouteOptionsAdapter?) {
        mainThreadAssertion()
        val rerouteController = requireNativeRerouteController()
        this.rerouteOptionsAdapter = rerouteOptionsAdapter
        rerouteController.setOptionsAdapter(
            rerouteOptionsAdapter?.let {
                RerouteOptionsAdapterWrapper(
                    it,
                )
            },
        )
    }

    override fun reroute(callback: RerouteController.RoutesCallback) {
        mainThreadAssertion()
        logI(TAG) { "Forcing reroute because of user request" }
        forceRerouteAndParse(ForceRerouteReason.USER_TRIGGERED) { _, parsingResult ->
            callback.onNewRoutes(
                parsingResult.newRoutes,
                parsingResult.newRoutes.first().origin,
            )
        }
    }

    private fun forceRerouteAndParse(
        reason: ForceRerouteReason,
        onRoutesAvailable: (RerouteInfo, RerouteResponseParsingResult.RoutesAvailable) -> Unit,
    ) {
        val rerouteDetector = requireNativeRerouteDetector()
        interruptParsingIfAny()
        nativeState = NativeRerouteControllerState.WaitingForResponse()
        rerouteDetector.forceReroute(reason) {
            logD(TAG) { "Received force reroute ($reason) callback with ${it.value}, ${it.error}" }
            it.onValue { value ->
                activeParsingJob = scope.launch {
                    nativeState = NativeRerouteControllerState.RouteObjectsParsing()
                    val parsingResult = handleRerouteResponse(
                        value.routeResponse,
                        value.routeRequest,
                        value.origin,
                    )
                    when (parsingResult) {
                        is RerouteResponseParsingResult.Error -> {
                            nativeState = NativeRerouteControllerState.Failed(
                                "Error parsing route",
                                parsingResult.throwable,
                            )
                            nativeState = NativeRerouteControllerState.Idle()
                        }

                        is RerouteResponseParsingResult.RoutesAvailable -> {
                            nativeState = NativeRerouteControllerState.RouteFetched(
                                parsingResult.newRoutes.firstOrNull()?.origin.orEmpty(),
                            )
                            nativeState = NativeRerouteControllerState.Idle()
                            onRoutesAvailable(value, parsingResult)
                        }
                    }
                }
            }.onError { rerouteError ->
                setRerouteFailureState("Force reroute ($reason)", rerouteError)
                nativeState = NativeRerouteControllerState.Idle()
            }
        }
    }

    private fun interruptParsingIfAny() {
        val previousJob = activeParsingJob
        activeParsingJob = null
        if (previousJob != null && previousJob.isActive) {
            logI(TAG) {
                "interrupting currently running route response parsing job"
            }
            previousJob.cancel()
            nativeState = NativeRerouteControllerState.Interrupted()
            nativeState = NativeRerouteControllerState.Idle()
        }
    }

    private fun setRerouteFailureState(tag: String, rerouteError: RerouteError) {
        logE(TAG) {
            "$tag. " +
                "type: ${rerouteError.type}; " +
                "message: ${rerouteError.message}; " +
                "router errors: ${rerouteError.routerErrors}"
        }
        nativeState = if (rerouteError.type == RerouteErrorType.CANCELLED) {
            NativeRerouteControllerState.Interrupted()
        } else {
            NativeRerouteControllerState.Failed(
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
            ResponseToParse(
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
