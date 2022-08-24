package com.mapbox.navigation.ui.maps.installer

import android.content.Context
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.base.installer.findComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.internal.ui.CameraModeButtonComponent
import com.mapbox.navigation.ui.maps.internal.ui.MapboxCameraModeButtonComponentContract
import com.mapbox.navigation.ui.maps.internal.ui.MapboxRecenterButtonComponentContract
import com.mapbox.navigation.ui.maps.internal.ui.NavigationCameraComponent
import com.mapbox.navigation.ui.maps.internal.ui.NavigationCameraGestureComponent
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton

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
    config: RouteLineComponentConfig.() -> Unit = {}
): Installation {
    val componentConfig = RouteLineComponentConfig(mapView.context).apply(config)
    return component(RouteLineComponent(mapView.getMapboxMap(), mapView, componentConfig.options))
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
    config: RouteArrowComponentConfig.() -> Unit = {}
): Installation {
    val componentConfig = RouteArrowComponentConfig(mapView.context).apply(config)
    return component(RouteArrowComponent(mapView.getMapboxMap(), componentConfig.options))
}

/**
 * Install component that re-centers the [mapView] to device location on [button] click.
 *
 * The installed components registers itself as a [LocationObserver] and updates camera position
 * only if location data is available.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.recenterButton(
    mapView: MapView,
    button: MapboxExtendableButton,
    config: RecenterButtonComponentConfig.() -> Unit = {}
): Installation {
    val componentConfig = RecenterButtonComponentConfig().apply(config)
    val contract = MapboxRecenterButtonComponentContract(mapView, componentConfig)
    return components(
        contract,
        RecenterButtonComponent(
            recenterButton = button,
            contractProvider = { contract }
        )
    )
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
    config: NavigationCameraComponentConfig.() -> Unit = {}
): Installation {
    val componentConfig = NavigationCameraComponentConfig().apply(config)
    val viewportDataSource =
        componentConfig.viewportDataSource ?: MapboxNavigationViewportDataSource(
            mapboxMap = mapView.getMapboxMap()
        )
    val navigationCamera = componentConfig.navigationCamera ?: NavigationCamera(
        mapboxMap = mapView.getMapboxMap(),
        cameraPlugin = mapView.camera,
        viewportDataSource = viewportDataSource
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
 * Install component that toggles [NavigationCamera] between [NavigationCameraState.OVERVIEW] and
 * [NavigationCameraState.FOLLOWING] on [button] click.
 *
 * The installed component will first use the [NavigationCamera] instance from [CameraModeComponentConfig].
 * If not available, it will attempt to use the [NavigationCamera] instance managed by the NavigationCamera component.
 * (see [ComponentInstaller.navigationCamera])
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.cameraModeButton(
    button: MapboxCameraModeButton,
    config: CameraModeComponentConfig.() -> Unit = {}
): Installation {
    val componentConfig = CameraModeComponentConfig().apply(config)
    val contract = MapboxCameraModeButtonComponentContract {
        componentConfig.navigationCamera
            ?: findComponent<NavigationCameraComponent>()?.navigationCamera
    }
    return components(
        contract,
        CameraModeButtonComponent(button, { contract })
    )
}

/**
 * Camera mode button component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class CameraModeComponentConfig internal constructor() {
    /**
     * Instance of [NavigationCamera] to use with this component.
     * Passing `null` will use [NavigationCamera] instance managed by the [ComponentInstaller.navigationCamera] component.
     */
    var navigationCamera: NavigationCamera? = null
}

/**
 * NavigationCamera component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class NavigationCameraComponentConfig internal constructor() {
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
 * Recenter button component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RecenterButtonComponentConfig internal constructor() {
    /**
     * Options for camera re-center request.
     */
    var cameraOptions: CameraOptions = CameraOptions.Builder()
        .zoom(15.0)
        .build()

    /**
     * Options for re-center camera animation.
     */
    var animationOptions: MapAnimationOptions? = null
}

/**
 * Route line component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteLineComponentConfig internal constructor(context: Context) {
    /**
     * Options used to create [MapboxRouteLineApi] and [MapboxRouteLineView] instance.
     */
    var options = MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .build()
}

/**
 * Route arrow component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteArrowComponentConfig internal constructor(context: Context) {
    /**
     * Options used to create [MapboxRouteArrowView] instance.
     */
    var options = RouteArrowOptions.Builder(context).build()
}
