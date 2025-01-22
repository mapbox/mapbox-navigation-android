package com.mapbox.navigation.ui.maps

import android.content.Context
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.maps.building.api.MapboxBuildingsApi
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions
import com.mapbox.navigation.ui.maps.building.view.MapboxBuildingView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.internal.ui.BuildingHighlightComponent
import com.mapbox.navigation.ui.maps.internal.ui.LocationComponent
import com.mapbox.navigation.ui.maps.internal.ui.LocationPuckComponent
import com.mapbox.navigation.ui.maps.internal.ui.NavigationCameraComponent
import com.mapbox.navigation.ui.maps.internal.ui.NavigationCameraGestureComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewDynamicOptionsBuilderBlock
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Install component that renders [LocationPuck].
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.locationPuck(
    mapView: MapView,
    config: LocationPuckConfig.() -> Unit = {},
): Installation {
    val componentConfig = LocationPuckConfig().apply(config)
    val locationPuck = componentConfig.locationPuck ?: LocationPuck2D(
        bearingImage = ImageHolder.from(R.drawable.mapbox_navigation_puck_icon),
    )
    val locationProvider = componentConfig.locationProvider ?: NavigationLocationProvider()
    val locationPuckComponent = LocationPuckComponent(
        mapView.location,
        locationPuck,
        locationProvider,
    )

    return if (componentConfig.enableLocationUpdates) {
        components(
            LocationComponent(locationProvider),
            locationPuckComponent,
        )
    } else {
        component(locationPuckComponent)
    }
}

/**
 * Install component that renders route line on the map.
 *
 * The installed component:
 * - renders route lines returned by [RoutesObserver]
 * - vanishes the traveled portion of the route line (if enabled via [MapboxRouteLineOptions] in the configuration)
 * - selects alternative route on map click
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.routeLine(
    mapView: MapView,
    config: RouteLineConfig.() -> Unit = {},
): Installation {
    val componentConfig = RouteLineConfig(mapView.context).apply(config)
    return component(
        RouteLineComponent(
            mapView.getMapboxMap(),
            mapView,
            componentConfig.apiOptions,
            componentConfig.viewOptions,
            viewOptionsUpdatesFlow = componentConfig.viewOptionsUpdates,
        ),
    )
}

/**
 * Install component that renders route line arrows on the map.
 *
 * The installed component registers itself as a [RouteProgressObserver] and
 * renders upcoming maneuver arrows on the map.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.routeArrow(
    mapView: MapView,
    config: RouteArrowConfig.() -> Unit = {},
): Installation {
    val componentConfig = RouteArrowConfig(mapView.context).apply(config)
    return component(RouteArrowComponent(mapView.getMapboxMap(), componentConfig.options))
}

/**
 * Install NavigationCamera component.
 *
 * The installed components register itself as both [LocationObserver] and [RouteProgressObserver]
 * and drives [NavigationCamera] by updating its [MapboxNavigationViewportDataSource].
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.navigationCamera(
    mapView: MapView,
    config: NavigationCameraConfig.() -> Unit = {},
): Installation {
    val componentConfig = NavigationCameraConfig().apply(config)
    val viewportDataSource =
        componentConfig.viewportDataSource ?: MapboxNavigationViewportDataSource(
            mapboxMap = mapView.getMapboxMap(),
        )
    val navigationCamera = componentConfig.navigationCamera ?: NavigationCamera(
        mapboxMap = mapView.getMapboxMap(),
        cameraPlugin = mapView.camera,
        viewportDataSource = viewportDataSource,
    )

    val cameraComponent = NavigationCameraComponent(viewportDataSource, navigationCamera)
    return if (componentConfig.switchToIdleOnMapGesture) {
        val gestureHandler = NavigationCameraGestureComponent(mapView, navigationCamera)
        components(gestureHandler, cameraComponent)
    } else {
        components(cameraComponent)
    }
}

/**
 * Install component that highlights building upon arrival.
 *
 * The installed component registers itself as a [ArrivalObserver] and [RoutesObserver].
 * It highlights building on [ArrivalObserver.onFinalDestinationArrival] callback and
 * un-highlights when the next route leg is started or when a list of routes is cleared.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.buildingHighlight(
    mapView: MapView,
    config: BuildingHighlightConfig.() -> Unit = {},
): Installation {
    val componentConfig = BuildingHighlightConfig().apply(config)
    val map = mapView.getMapboxMap()
    val api = componentConfig.buildingsApi ?: MapboxBuildingsApi(map)
    val view = componentConfig.buildingView ?: MapboxBuildingView()
    return component(
        BuildingHighlightComponent(map, componentConfig.options, api, view),
    )
}

/**
 * NavigationCamera component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class NavigationCameraConfig internal constructor() {
    /**
     * Instance of [MapboxNavigationViewportDataSource] to use with the [NavigationCamera].
     * This value is ignored when [navigationCamera] is set.
     */
    var viewportDataSource: MapboxNavigationViewportDataSource? = null

    /**
     * Instance of [NavigationCamera] to use with this component.
     */
    var navigationCamera: NavigationCamera? = null

    /**
     * Set if [NavigationCamera] should switch to [NavigationCameraState.IDLE] state
     * whenever any map gesture input is registered.
     */
    var switchToIdleOnMapGesture: Boolean = false
}

/**
 * Route line component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteLineConfig internal constructor(context: Context) {
    /**
     * Options used to create [MapboxRouteLineApi] instance.
     */
    var apiOptions = MapboxRouteLineApiOptions.Builder().build()

    /**
     * Options used to create [MapboxRouteLineView] instance.
     */
    var viewOptions = MapboxRouteLineViewOptions.Builder(context).build()

    /**
     * A flow of [MapboxRouteLineViewDynamicOptionsBuilderBlock].
     * Use this if you want to change [MapboxRouteLineViewOptions] in runtime.
     * Whenever a new value is emitted, a set of options configured by [MapboxRouteLineViewDynamicOptionsBuilderBlock]
     * is updated on the fly.
     */
    var viewOptionsUpdates: Flow<MapboxRouteLineViewDynamicOptionsBuilderBlock> = flowOf()
}

/**
 * Route arrow component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteArrowConfig internal constructor(context: Context) {
    /**
     * Options used to create [MapboxRouteArrowView] instance.
     */
    var options = RouteArrowOptions.Builder(context).build()
}

/**
 * Location puck component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class LocationPuckConfig internal constructor() {
    /**
     * An instance of either [LocationPuck2D] or [LocationPuck3D] to use with this component.
     */
    var locationPuck: LocationPuck? = null

    /**
     * An instance of [NavigationLocationProvider] to use with this component.
     */
    var locationProvider: NavigationLocationProvider? = null

    /**
     * Set whether the [locationProvider] should register as a MapboxNavigation [LocationObserver].
     */
    var enableLocationUpdates: Boolean = true
}

/**
 * Building Highlight component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class BuildingHighlightConfig internal constructor() {
    /**
     * Options used by [MapboxBuildingView] to highlight a building.
     */
    var options: MapboxBuildingHighlightOptions = MapboxBuildingHighlightOptions.Builder().build()

    /**
     * An instance of [MapboxBuildingsApi] to use with this component.
     */
    var buildingsApi: MapboxBuildingsApi? = null

    /**
     * An instance of [MapboxBuildingView] to use with this component.
     */
    var buildingView: MapboxBuildingView? = null
}
