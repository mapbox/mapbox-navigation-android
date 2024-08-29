package com.mapbox.navigation.ui.components.maps

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.tripdata.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.base.installer.findComponent
import com.mapbox.navigation.ui.components.MapboxExtendableButton
import com.mapbox.navigation.ui.components.maps.internal.ui.CameraModeButtonComponent
import com.mapbox.navigation.ui.components.maps.internal.ui.MapboxCameraModeButtonComponentContract
import com.mapbox.navigation.ui.components.maps.internal.ui.MapboxRecenterButtonComponentContract
import com.mapbox.navigation.ui.components.maps.internal.ui.MapboxRoadNameComponentContract
import com.mapbox.navigation.ui.components.maps.internal.ui.RecenterButtonComponent
import com.mapbox.navigation.ui.components.maps.internal.ui.RoadNameComponent
import com.mapbox.navigation.ui.components.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.components.maps.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.internal.ui.NavigationCameraComponent

/**
 * Install component that updates [MapboxRoadNameView] with a road name that matches current device location.
 *
 * The installed component registers itself as a [LocationObserver] and updates the label only
 * if road name information is available.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.roadName(
    mapView: MapView,
    roadNameView: MapboxRoadNameView,
    config: RoadNameConfig.() -> Unit = {},
): Installation {
    val componentConfig = RoadNameConfig().apply(config)
    val contract = MapboxRoadNameComponentContract(mapView.getMapboxMap())
    return components(
        contract,
        RoadNameComponent(
            roadNameView,
            { contract },
            componentConfig.routeShieldApi ?: MapboxRouteShieldApi(),
        ),
    )
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
    config: RecenterButtonConfig.() -> Unit = {},
): Installation {
    val componentConfig = RecenterButtonConfig().apply(config)
    val contract = MapboxRecenterButtonComponentContract(mapView, componentConfig)
    return components(
        contract,
        RecenterButtonComponent(
            recenterButton = button,
            contractProvider = { contract },
        ),
    )
}

/**
 * Install component that toggles [NavigationCamera] between [NavigationCameraState.OVERVIEW] and
 * [NavigationCameraState.FOLLOWING] on [button] click.
 *
 * The installed component will first use the [NavigationCamera] instance from [CameraModeConfig].
 * If not available, it will attempt to use the [NavigationCamera] instance managed by the NavigationCamera component.
 * (see [ComponentInstaller.navigationCamera])
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.cameraModeButton(
    button: MapboxCameraModeButton,
    config: CameraModeConfig.() -> Unit = {},
): Installation {
    val componentConfig = CameraModeConfig().apply(config)
    val contract = MapboxCameraModeButtonComponentContract {
        componentConfig.navigationCamera
            ?: findComponent<NavigationCameraComponent>()?.navigationCamera
    }
    return components(
        contract,
        CameraModeButtonComponent(button) { contract },
    )
}

/**
 * Camera mode button component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class CameraModeConfig internal constructor() {
    /**
     * Instance of [NavigationCamera] to use with this component.
     * Passing `null` will use [NavigationCamera] instance managed by the [ComponentInstaller.navigationCamera] component.
     */
    var navigationCamera: NavigationCamera? = null
}

/**
 * Recenter button component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RecenterButtonConfig internal constructor() {
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
 * Road name component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RoadNameConfig internal constructor() {
    /**
     * A [MapboxRouteShieldApi] instance to use with this component.
     */
    var routeShieldApi: MapboxRouteShieldApi? = null
}
