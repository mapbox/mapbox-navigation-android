package com.mapbox.navigation.ui.maps.camera

import android.animation.AnimatorSet
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import kotlin.math.min

class MapboxNavigationStateTransition(
    mapView: MapView,
    private val navigationCameraTransition: NavigationCameraTransitionProvider
) : NavigationStateTransitionProvider {
    private val mapboxMap = mapView.getMapboxMap()

    override fun transitionToVehicleFollowing(
        transitionOptions: NavigationStateTransitionToFollowingOptions
    ): AnimatorSet {
        val points = listOf(transitionOptions.pointsAheadOfVehicleLocation,
            transitionOptions.additionalPointsToFrame).flatten()
        val zoomAndCenter = getZoomLevelAndCenterCoordinate(points,
            transitionOptions.vehicleLocation.bearing.toDouble(),
            transitionOptions.pitch, transitionOptions.padding)
        val center = Point.fromLngLat(transitionOptions.vehicleLocation.longitude,
            transitionOptions.vehicleLocation.latitude)
        val zoom = min(zoomAndCenter.first, transitionOptions.maxZoom)
        val yOffset = mapboxMap.getSize().height / 2 - transitionOptions.padding.bottom
        return navigationCameraTransition.transitionFromLowZoomToHighZoom(
            NavigationCameraZoomTransitionOptions.Builder(
                center, zoom).apply {
                bearing(transitionOptions.vehicleLocation.bearing.toDouble())
                pitch(transitionOptions.pitch)
                anchorOffset(ScreenCoordinate(0.0, yOffset))
                animatorListener(transitionOptions.animatorListener)
            }.build()
        )
    }

    override fun updateMapFrameForFollowing(transitionOptions: NavigationStateTransitionToFollowingOptions): AnimatorSet {
        val points = listOf(transitionOptions.pointsAheadOfVehicleLocation,
            transitionOptions.additionalPointsToFrame).flatten()
        val bearing = transitionOptions.vehicleLocation.bearing.toDouble()
        val zoomAndCenter = getZoomLevelAndCenterCoordinate(points, bearing, 0.0, transitionOptions.padding)
        val center = Point.fromLngLat(transitionOptions.vehicleLocation.longitude,
            transitionOptions.vehicleLocation.latitude)
        val zoom = min(zoomAndCenter.first, transitionOptions.maxZoom)
        val yOffset = mapboxMap.getSize().height - transitionOptions.padding.bottom
        return navigationCameraTransition.transitionLinear(
            NavigationCameraLinearTransitionOptions.Builder(
                center, zoom).apply {
                bearing(bearing)
                pitch(transitionOptions.pitch)
                anchorOffset(ScreenCoordinate(0.0, yOffset))
                animatorListener(transitionOptions.animatorListener)
            }.build()
        )
    }

    override fun transitionToRouteOverview(
        transitionOptions: NavigationStateTransitionToRouteOverviewOptions
    ): AnimatorSet {
        val points = listOf(transitionOptions.remainingPointsOfRoute,
            transitionOptions.additionalPointsToFrame).flatten()
        val zoomAndCenter = getZoomLevelAndCenterCoordinate(points,
            0.0, 0.0, transitionOptions.padding)
        val center = zoomAndCenter.second
        val zoom = min(zoomAndCenter.first, transitionOptions.maxZoom)
        val currentMapCamera = mapboxMap.getCameraOptions(null)
        return if (currentMapCamera.zoom ?: 2.0 < zoomAndCenter.first)
            navigationCameraTransition.transitionFromLowZoomToHighZoom(
                NavigationCameraZoomTransitionOptions.Builder(
                    center, zoom).apply {
                    animatorListener(transitionOptions.animatorListener)
                }.build())
        else
            navigationCameraTransition.transitionFromHighZoomToLowZoom(
                NavigationCameraZoomTransitionOptions.Builder(
                    center, zoom).apply {
                    animatorListener(transitionOptions.animatorListener)
                }.build())
    }

    override fun updateMapFrameForOverview(
        transitionOptions: NavigationStateTransitionToRouteOverviewOptions
    ): AnimatorSet {
        val points = listOf(transitionOptions.remainingPointsOfRoute,
            transitionOptions.additionalPointsToFrame).flatten()
        val zoomAndCenter = getZoomLevelAndCenterCoordinate(points,
            0.0, 0.0, transitionOptions.padding)
        val center = zoomAndCenter.second
        val zoom = min(zoomAndCenter.first, transitionOptions.maxZoom)

        return navigationCameraTransition.transitionLinear(
            NavigationCameraLinearTransitionOptions.Builder(
                center, zoom).apply {
                animatorListener(transitionOptions.animatorListener)
            }.build())
    }

    private fun getZoomLevelAndCenterCoordinate(points: List<Point>, bearing: Double, pitch: Double, edgeInsets: EdgeInsets): Pair<Double, Point> {
        val cam = mapboxMap.cameraForCoordinates(points, edgeInsets, bearing, pitch)

        if (cam.zoom != null && cam.center != null) {
            return Pair(cam.zoom!!, cam.center!!)
        }
        return Pair(2.0, Point.fromLngLat(0.0, 0.0))
    }
}
