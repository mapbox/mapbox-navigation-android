package com.mapbox.navigation.ui.androidauto.navigation

import androidx.annotation.UiThread
import androidx.car.app.Screen
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.constraints.ConstraintManager
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
import com.mapbox.navigation.ui.androidauto.internal.extensions.contentLimit
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.search.FavoritesApi
import com.mapbox.navigation.ui.androidauto.location.CarLocationRenderer
import com.mapbox.navigation.ui.androidauto.navigation.roadlabel.CarRoadLabelRenderer
import com.mapbox.navigation.ui.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapManager
import com.mapbox.navigation.ui.androidauto.placeslistonmap.buildListTemplate
import com.mapbox.navigation.ui.androidauto.preview.CarRouteLineRenderer
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenFactory
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.screenmanager.factories.FavoritesScreenFactory
import com.mapbox.navigation.ui.androidauto.search.ResumedRoutePreviewCallback
import com.mapbox.navigation.ui.androidauto.search.showSearchToast
import com.mapbox.navigation.ui.androidauto.settings.CarSettingsMode
import com.mapbox.navigation.ui.androidauto.settings.buildCarSettingsTemplate
import com.mapbox.search.ServiceProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A single map screen for free drive, route preview, and active guidance.
 *
 * Route preview and inline panels use [MapWithContentTemplate]. Free drive and active guidance use
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
        options = mapboxCarContext.options.routeLineRendererOptions,
        carRoutesProvider = routesProvider,
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

    internal var isShowingSettings = false
        private set
    internal var isShowingFavorites = false
        private set
    private val isShowingInlineContent: Boolean
        get() = isShowingSettings || isShowingFavorites

    private val favoritesManager by lazy {
        PlacesListOnMapManager(
            FavoritesApi(ServiceProvider.INSTANCE.favoritesDataProvider()),
        ).apply { showsInListTemplate = true }
    }
    private var favoritesJob: Job? = null
    private var isFramingFavorites = false

    private val favoritesRouteCallback by lazy {
        ResumedRoutePreviewCallback(
            lifecycle = lifecycle,
            mapboxScreenManager = mapboxCarContext.mapboxScreenManager,
            routePreviewRequest = mapboxCarContext.routePreviewRequest,
            onPreviewReady = {
                hideInlineContent()
                routesProvider.showPreview()
                invalidate()
            },
        ) { messageRes -> carContext.showSearchToast(messageRes) }
    }

    init {
        logAndroidAuto("MapboxNavigationScreen constructor")
        addBackPressedHandler {
            if (isShowingInlineContent) {
                hideInlineContent()
                invalidate()
                return@addBackPressedHandler
            }
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
                    navigationInfoProvider.carNavigationInfo.collect {
                        if (!isShowingInlineContent &&
                            routesProvider.state.value.screenState ==
                            MapboxNavigationScreenState.ACTIVE_GUIDANCE
                        ) {
                            invalidate()
                        }
                    }
                }
                launch {
                    routesProvider.state
                        .map { it.screenState }
                        .distinctUntilChanged()
                        .collect { screenState ->
                            if (isShowingInlineContent) {
                                hideInlineContent()
                                invalidate()
                            }
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
                    if (isShowingInlineContent) {
                        hideInlineContent()
                        invalidate()
                    }
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
        if (isShowingInlineContent) return
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
        return when {
            isShowingFavorites -> {
                buildMapWithContentTemplate(
                    favoritesManager.buildListTemplate(carContext),
                    state.screenState,
                )
            }
            isShowingSettings -> {
                buildMapWithContentTemplate(
                    mapboxCarContext.options.settingsTemplateProvider
                        ?.getSettingsTemplate(this, state.screenState.settingsMode())
                        ?: mapboxCarContext.buildCarSettingsTemplate(),
                    state.screenState,
                )
            }
            else -> when (state.screenState) {
                MapboxNavigationScreenState.FREE_DRIVE -> buildFreeDriveTemplate()
                MapboxNavigationScreenState.ACTIVE_GUIDANCE -> buildActiveGuidanceTemplate()
                MapboxNavigationScreenState.ROUTE_PREVIEW -> buildMapWithContentTemplate(
                    buildRoutePreviewTemplate(state),
                    state.screenState,
                )
            }
        }
    }

    internal fun showSettings() {
        hideInlineContent()
        isShowingSettings = true
        invalidate()
    }

    @Suppress("DEPRECATION")
    internal fun showFavorites() {
        val screenManager = mapboxCarContext.mapboxScreenManager
        if (MapboxScreen.FAVORITES in screenManager &&
            screenManager.requireScreenFactory<MapboxScreenFactory>(MapboxScreen.FAVORITES)
            !is FavoritesScreenFactory
        ) {
            MapboxScreenManager.push(MapboxScreen.FAVORITES)
            return
        }
        if (isShowingFavorites) return
        hideInlineContent()
        isShowingFavorites = true
        isFramingFavorites = mapboxCarContext.options.frameFavoritesOnMap
        favoritesJob = lifecycleScope.launch {
            launch { favoritesManager.state.collect { invalidate() } }
            if (isFramingFavorites) {
                launch {
                    favoritesManager.placeRecords.collect { places ->
                        carNavigationCamera.updateOverviewPoints(
                            places.mapNotNull { it.coordinate },
                        )
                    }
                }
            }
            favoritesManager.placeClicks.collect { place ->
                mapboxCarContext.routePreviewRequest.request(place, favoritesRouteCallback)
            }
        }
        mapboxCarContext.mapboxCarMap.registerObserver(favoritesManager)
        invalidate()
    }

    private fun hideInlineContent() {
        isShowingSettings = false
        favoritesJob?.cancel()
        favoritesJob = null
        if (isFramingFavorites) {
            carNavigationCamera.updateOverviewPoints(emptyList())
            isFramingFavorites = false
        }
        if (isShowingFavorites) {
            mapboxCarContext.routePreviewRequest.cancelRequest(favoritesRouteCallback)
            mapboxCarContext.mapboxCarMap.unregisterObserver(favoritesManager)
        }
        isShowingFavorites = false
    }

    private fun buildMapWithContentTemplate(
        contentTemplate: ListTemplate,
        screenState: MapboxNavigationScreenState,
    ): MapWithContentTemplate {
        return MapWithContentTemplate.Builder()
            .setContentTemplate(contentTemplate)
            .setMapController(
                MapController.Builder()
                    .setMapActionStrip(mapActionStripBuilder.build())
                    .build(),
            )
            .setActionStrip(
                mapboxCarContext.options.actionStripProvider.getActionStrip(
                    this,
                    screenState.screenKey(),
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
            maxRoutes = carContext.contentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST),
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

    private fun MapboxNavigationScreenState.screenKey() = when (this) {
        MapboxNavigationScreenState.FREE_DRIVE -> MapboxScreen.FREE_DRIVE
        MapboxNavigationScreenState.ROUTE_PREVIEW -> MapboxScreen.ROUTE_PREVIEW
        MapboxNavigationScreenState.ACTIVE_GUIDANCE -> MapboxScreen.ACTIVE_GUIDANCE
    }

    private fun MapboxNavigationScreenState.settingsMode() = when (this) {
        MapboxNavigationScreenState.FREE_DRIVE -> CarSettingsMode.FREE_DRIVE
        MapboxNavigationScreenState.ROUTE_PREVIEW -> CarSettingsMode.ROUTE_PREVIEW
        MapboxNavigationScreenState.ACTIVE_GUIDANCE -> CarSettingsMode.ACTIVE_GUIDANCE
    }
}
