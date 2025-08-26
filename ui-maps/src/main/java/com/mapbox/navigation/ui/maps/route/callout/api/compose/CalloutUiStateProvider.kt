package com.mapbox.navigation.ui.maps.route.callout.api.compose

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.callout.api.MapboxRouteCalloutsApi
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersObserver
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesSetToRouteLineDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesSetToRouteLineObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Provider of UI state for route callouts.
 *
 * Normally, route callouts are drawn under the hood in NavSDK when this feature is enabled in [MapboxRouteLineApiOptions].
 * However, there might be cases when app wants to only get the callout data from NavSDK and attach the DVA itself.
 * An example of such a case is using Mapbox Maps SDK Compose extensions: attaching a DVA for
 * Compose MapboxMap is done via [compose-specific API](https://docs.mapbox.com/android/maps/examples/compose/dynamic-view-annotations/),
 * which is not currently supported by NavSDK.
 * In this case you may listen to [CalloutUiState] updates and use its information by attach a DVA.
 * Use this class (specifically, [uiStateData] flow) to subscribe.
 *
 * NOTE: after you are done using this object, invoke [destroy] method to clean-up resources and avoid memory leaks.
 */
@ExperimentalPreviewMapboxNavigationAPI
class CalloutUiStateProvider internal constructor(
    private val routesSetToRouteLineDataProvider: RoutesSetToRouteLineDataProvider,
    private val routesAttachedToLayersDataProvider: RoutesAttachedToLayersDataProvider,
    private val routesCalloutsApi: MapboxRouteCalloutsApi = MapboxRouteCalloutsApi(),
) {

    private val calloutsData = callbackFlow {
        val routesSetToRouteLineObserver =
            RoutesSetToRouteLineObserver { routes, alternativeMetadata ->
                logI(TAG) { "Routes set to route line: ${routes.map { it.id }}" }
                trySend(routesCalloutsApi.setNavigationRoutes(routes, alternativeMetadata))
            }
        routesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
            routesSetToRouteLineObserver,
        )
        awaitClose {
            routesSetToRouteLineDataProvider.unregisterRoutesSetToRouteLineObserver(
                routesSetToRouteLineObserver,
            )
        }
    }

    private val routesToLayers = callbackFlow {
        val routesAttachedToLayersObserver = RoutesAttachedToLayersObserver { routesToLayers ->
            logI(TAG) { "Routes are attached to layers: $routesToLayers" }
            trySend(routesToLayers)
        }
        routesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
            routesAttachedToLayersObserver,
        )
        awaitClose {
            routesAttachedToLayersDataProvider.unregisterRoutesAttachedToLayersObserver(
                routesAttachedToLayersObserver,
            )
        }
    }

    /**
     * Flow of [CalloutUiStateData].
     * Subscribe to this flow to retrieve UI data for callouts in case you want to attach the DVA yourself.
     */
    val uiStateData: Flow<CalloutUiStateData> = calloutsData.flatMapLatest { calloutsData ->
        // The fact that routes were set to route line does not mean they were rendered
        // (they were set to MapboxRouteLineApi, not MapboxRouteLineView).
        // So we don't need to take the current routesToLayers value, instead we should wait for the new one to arrive.
        routesToLayers.map { routesToLayers ->
            CalloutUiStateData(
                calloutsData.callouts.mapNotNull { callout ->
                    routesToLayers[callout.route.id]?.let { layerId ->
                        CalloutUiState(callout, layerId)
                    }
                },
            )
        }
    }

    companion object {

        private const val TAG = "CalloutUiStateProvider"

        /**
         * Create [CalloutUiStateData] for the specified route line components.
         * Note: it is important that you pass [MapboxRouteLineApi] and [MapboxRouteLineView] which will be used to draw routes you want to attach your DVAs to.
         * In case you have multiple map instances, multiple instances of [CalloutUiStateProvider] must be created (one per each Map (and per each [MapboxRouteLineView])).
         */
        fun create(
            routeLineApi: MapboxRouteLineApi,
            routeLineView: MapboxRouteLineView,
        ): CalloutUiStateProvider =
            CalloutUiStateProvider(
                MapboxRoutesSetToRouteLineDataProvider(routeLineApi),
                MapboxRoutesAttachedToLayersDataProvider(routeLineView),
            )
    }
}
