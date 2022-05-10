package com.mapbox.androidauto.car.navigation

import android.graphics.Rect
import android.location.Location
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions

@OptIn(MapboxExperimental::class)
class CarLocationsOverviewCamera(
    val mapboxNavigation: MapboxNavigation,
    private val initialCameraOptions: CameraOptions = CameraOptions.Builder()
        .zoom(DEFAULT_INITIAL_ZOOM)
        .build()
) : MapboxCarMapObserver {

    internal var mapboxCarMapSurface: MapboxCarMapSurface? = null
        private set
    internal lateinit var navigationCamera: NavigationCamera
        private set
    internal lateinit var viewportDataSource: MapboxNavigationViewportDataSource
        private set
    internal var isLocationInitialized = false
        private set
    private var latestLocation: Location? = null

    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            // Initialize the camera at the current location. The next location will
            // transition into the overview mode.
            latestLocation = locationMatcherResult.enhancedLocation
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()
            if (!isLocationInitialized) {
                isLocationInitialized = true
                val instantTransition = NavigationCameraTransitionOptions.Builder()
                    .maxDuration(0)
                    .build()

                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = instantTransition
                )
            }
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onAttached(mapboxCarMapSurface)
        this.mapboxCarMapSurface = mapboxCarMapSurface
        logAndroidAuto("LocationsOverviewCamera loaded $mapboxCarMapSurface")

        val mapboxMap = mapboxCarMapSurface.mapSurface.getMapboxMap().also {
            it.setCamera(initialCameraOptions)
        }
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxCarMapSurface.mapSurface.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            mapboxMap,
            mapboxCarMapSurface.mapSurface.camera,
            viewportDataSource
        )

        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        super.onVisibleAreaChanged(visibleArea, edgeInsets)
        logAndroidAuto("LocationsOverviewCamera visibleAreaChanged $visibleArea $edgeInsets")

        viewportDataSource.overviewPadding = EdgeInsets(
            edgeInsets.top + OVERVIEW_PADDING,
            edgeInsets.left + OVERVIEW_PADDING,
            edgeInsets.bottom + OVERVIEW_PADDING,
            edgeInsets.right + OVERVIEW_PADDING
        )

        viewportDataSource.evaluate()
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onDetached(mapboxCarMapSurface)
        logAndroidAuto("LocationsOverviewCamera detached $mapboxCarMapSurface")

        mapboxNavigation.unregisterLocationObserver(locationObserver)
        this.mapboxCarMapSurface = null
        isLocationInitialized = false
    }

    fun updateWithLocations(points: List<Point>) {
        if (points.isNotEmpty()) {
            logAndroidAuto("LocationsOverviewCamera updateWithLocations")
            viewportDataSource.additionalPointsToFrameForOverview(points)
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    private companion object {
        private const val OVERVIEW_PADDING = 15
        const val DEFAULT_INITIAL_ZOOM = 15.0
    }
}
