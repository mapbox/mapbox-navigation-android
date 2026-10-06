package com.mapbox.navigation.ui.androidauto.navigation

import androidx.annotation.UiThread
import androidx.car.app.Screen
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.action.MapboxMapActionStrip
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.location.CarLocationRenderer
import com.mapbox.navigation.ui.androidauto.navigation.roadlabel.CarRoadLabelRenderer
import com.mapbox.navigation.ui.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.navigation.ui.androidauto.preview.CarRouteLineRenderer
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A single map screen for free drive, route preview, and active guidance.
 *
 * Route preview uses [MapWithContentTemplate]. Free drive and active guidance use
 * [NavigationTemplate], with active guidance displaying the host's native travel-estimate card.
 * The screen requires Car API 7 and keeps the map and its renderers attached while navigation
 * state changes.
 */
@RequiresCarApi(7)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxNavigationScreen @UiThread constructor(
    private val mapboxCarContext: MapboxCarContext,
) : Screen(mapboxCarContext.carContext) {

    private val routesProvider = MapboxNavigationRoutesProvider()
    private val carRouteLineRenderer = CarRouteLineRenderer(
        carRoutesProvider = routesProvider,
        options = mapboxCarContext.options.routeLineRendererOptions,
        onRouteClick = ::onRouteClicked,
    )
    private val carLocationRenderer = CarLocationRenderer()
    private val carSpeedLimitRenderer = CarSpeedLimitRenderer(mapboxCarContext)
    private val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = CarCameraMode.OVERVIEW,
        carRoutesProvider = routesProvider,
    )
    private val carRoadLabelRenderer = CarRoadLabelRenderer()
    private val carMarkers = CarActiveGuidanceMarkers(routesProvider)
    private val navigationInfoProvider = CarNavigationInfoProvider()
        .invalidateOnChange(this)
    private val mapActionStripBuilder = MapboxMapActionStrip(this, carNavigationCamera)
    private val carArrivalTrigger = CarArrivalTrigger()
    private val mapObservers: List<MapboxCarMapObserver> = listOf(
        carLocationRenderer,
        carRoadLabelRenderer,
        carSpeedLimitRenderer,
        carNavigationCamera,
        carRouteLineRenderer,
        carMarkers,
        navigationInfoProvider,
    )
    private var isResumed = false
    private var routeClickRefresh: Job? = null

    init {
        logAndroidAuto("MapboxNavigationScreen constructor")
        addBackPressedHandler {
            val isPreview = routesProvider.state.value.screenState ==
                MapboxNavigationScreenState.ROUTE_PREVIEW
            if (isPreview) {
                routesProvider.clearPreview()
            } else {
                mapboxCarContext.mapboxScreenManager.goBack()
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    routesProvider.state
                        .map { it.screenState }
                        .distinctUntilChanged()
                        .collect { screenState ->
                            if (isResumed) {
                                carNavigationCamera.updateCameraMode(
                                    screenState.preferredCameraMode(),
                                )
                            }
                        }
                }
                routesProvider.state
                    .distinctUntilChangedBy {
                        it.screenState.hashCode() +
                            it.routesPreview?.originalRoutesList.hashCode()
                    }
                    .collect {
                        invalidate()
                    }
            }
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    MapboxNavigationApp.registerObserver(routesProvider)
                }

                override fun onResume(owner: LifecycleOwner) {
                    logAndroidAuto("MapboxNavigationScreen onResume")
                    mapObservers.forEach(mapboxCarContext.mapboxCarMap::registerObserver)
                    mapboxCarContext.mapboxCarMap.setGestureHandler(
                        carNavigationCamera.gestureHandler,
                    )
                    MapboxNavigationApp.registerObserver(carArrivalTrigger)
                    isResumed = true
                    carNavigationCamera.updateCameraMode(
                        routesProvider.state.value.screenState.preferredCameraMode(),
                    )
                }

                override fun onPause(owner: LifecycleOwner) {
                    logAndroidAuto("MapboxNavigationScreen onPause")
                    isResumed = false
                    mapObservers.forEach(mapboxCarContext.mapboxCarMap::unregisterObserver)
                    mapboxCarContext.mapboxCarMap.setGestureHandler(null)
                    MapboxNavigationApp.unregisterObserver(carArrivalTrigger)
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    MapboxNavigationApp.unregisterObserver(routesProvider)
                }
            },
        )
    }

    private fun onRouteClicked(route: NavigationRoute) {
        val state = routesProvider.state.value
        if (state.screenState != MapboxNavigationScreenState.ROUTE_PREVIEW) return
        val routesPreview = state.routesPreview ?: return
        if (routesPreview.originalRoutesList.none { it.id == route.id }) return
        if (routesPreview.primaryRoute.id == route.id) return
        routesProvider.selectRoute(route.id)
        routeClickRefresh?.cancel()
        routeClickRefresh = lifecycleScope.launch {
            routesProvider.state.first { it.routesPreview?.primaryRoute?.id == route.id }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val state = routesProvider.state.value
        logAndroidAuto("MapboxNavigationScreen onGetTemplate ${state.screenState}")
        return when (state.screenState) {
            MapboxNavigationScreenState.FREE_DRIVE -> buildFreeDriveTemplate()
            MapboxNavigationScreenState.ACTIVE_GUIDANCE -> buildActiveGuidanceTemplate()
            MapboxNavigationScreenState.ROUTE_PREVIEW -> buildRoutePreviewMapTemplate(state)
        }
    }

    private fun buildRoutePreviewMapTemplate(
        state: MapboxNavigationRoutesState,
    ): MapWithContentTemplate {
        return MapWithContentTemplate.Builder()
            .setContentTemplate(buildRoutePreviewTemplate(state))
            .setMapController(
                MapController.Builder()
                    .setMapActionStrip(mapActionStripBuilder.build())
                    .build(),
            )
            .setActionStrip(
                mapboxCarContext.options.actionStripProvider.getActionStrip(
                    this,
                    MapboxScreen.ROUTE_PREVIEW,
                ),
            )
            .build()
    }

    private fun buildFreeDriveTemplate(): NavigationTemplate {
        return CarNavigationTemplates.freeDrive(
            actionStrip = mapboxCarContext.options.actionStripProvider.getActionStrip(
                this,
                MapboxScreen.FREE_DRIVE,
            ),
            mapActionStrip = mapActionStripBuilder.build(),
        )
    }

    private fun buildRoutePreviewTemplate(state: MapboxNavigationRoutesState): ListTemplate {
        val routesPreview = requireNotNull(state.routesPreview)
        return CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = carContext.getString(R.string.car_action_preview_title),
            navigateActionTitle = carContext.getString(
                R.string.car_action_preview_navigate_button,
            ),
            formatDistance = CarDistanceFormatter::formatDistance,
            onRouteSelected = routesProvider::selectRoute,
            onNavigate = routesProvider::startNavigation,
            navigateActionIcon = CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext,
                    R.drawable.ic_recenter_right_24,
                ),
            ).build(),
        )
    }

    private fun buildActiveGuidanceTemplate(): NavigationTemplate {
        return CarNavigationTemplates.activeGuidance(
            navigationInfo = navigationInfoProvider.carNavigationInfo.value,
            actionStrip = mapboxCarContext.options.actionStripProvider.getActionStrip(
                this,
                MapboxScreen.ACTIVE_GUIDANCE,
            ),
            mapActionStrip = mapActionStripBuilder.build(),
        )
    }

    private fun MapboxNavigationScreenState.preferredCameraMode() = when (this) {
        MapboxNavigationScreenState.ROUTE_PREVIEW -> CarCameraMode.OVERVIEW
        MapboxNavigationScreenState.FREE_DRIVE,
        MapboxNavigationScreenState.ACTIVE_GUIDANCE,
        -> CarCameraMode.FOLLOWING
    }
}
