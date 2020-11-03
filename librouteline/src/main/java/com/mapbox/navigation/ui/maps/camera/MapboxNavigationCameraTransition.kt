package com.mapbox.navigation.ui.maps.camera

import android.animation.AnimatorSet
import androidx.core.view.animation.PathInterpolatorCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.maps.plugin.animation.animator.*
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class MapboxNavigationCameraTransition(mapView: MapView) : NavigationCameraTransitionProvider {
    private val mapboxMap = mapView.getMapboxMap()
    private val mapCamera = mapView.getCameraAnimationsPlugin()

    private var edgeInsets = EdgeInsets(20.0, 20.0, 20.0, 20.0)

    override fun transitionFromLowZoomToHighZoom(
        transitionOptions: NavigationCameraZoomTransitionOptions): AnimatorSet {
        val currentMapCameraOptions = mapboxMap.getCameraOptions(null)

        val currentMapCameraCenter = currentMapCameraOptions.center
        var screenDistanceFromMapCenterToLocation = 0.0
        currentMapCameraCenter?.let {
            val currentCenterScreenCoordinate =
                mapboxMap.pixelForCoordinate(Point.fromLngLat(it.longitude(), it.latitude()))
            val locationScreenCoordinate =
                mapboxMap.pixelForCoordinate(transitionOptions.center)
            screenDistanceFromMapCenterToLocation =
                hypot(currentCenterScreenCoordinate.x - locationScreenCoordinate.x,
                    currentCenterScreenCoordinate.y - locationScreenCoordinate.y)
        }
        val centerAnimationRate = 300.0
        val centerDuration = min((screenDistanceFromMapCenterToLocation / centerAnimationRate) * 1000.0, 3000.0)
        val centerAnimator = CameraCenterAnimator.create(transitionOptions.center) {
            duration = centerDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val currentMapCameraZoom = currentMapCameraOptions.zoom
        var zoomDelta = 0.0
        currentMapCameraZoom?.let {
            zoomDelta = abs(transitionOptions.zoom - it)
        }

        val zoomAnimationRate = 2.0
        val zoomDelay = centerDuration * 0.3
        val zoomDuration = min((zoomDelta / zoomAnimationRate) * 1000.0, 3000.0)

        val zoomAnimator = CameraZoomAnimator.create(
            // workaround for https://github.com/mapbox/mapbox-maps-android/issues/785
            AbstractCameraAnimator.StartValue(currentMapCameraOptions.zoom ?: transitionOptions.zoom),
            transitionOptions.zoom
        ) {
            startDelay = zoomDelay.toLong()
            duration = zoomDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        var bearingShortestRotation = transitionOptions.bearing
        currentMapCameraOptions.bearing?.let {
            bearingShortestRotation = it + shortestRotation(it, transitionOptions.bearing)
        }
        val bearingDuration = 1800.0
        val bearingDelay = max(zoomDelay + zoomDuration - bearingDuration, 0.0)
        val bearingAnimator = CameraBearingAnimator.create(bearingShortestRotation) {
            startDelay = bearingDelay.toLong()
            duration = bearingDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val pitchAndAnchorDuration = 1200.0
        val pitchAndAnchorDelay = max(zoomDelay + zoomDuration - pitchAndAnchorDuration + 100, 0.0)
        val pitchAnimator = CameraPitchAnimator.create(transitionOptions.pitch) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        var currentPadding = edgeInsets
        currentMapCameraOptions.padding?.let {
            currentPadding = it
        }
        val endPadding = convertScreenCenterOffsetToEdgeInsets(
            mapboxMap.getSize(), transitionOptions.anchorOffset)
        val anchorAnimator = CameraPaddingAnimator.create(AbstractCameraAnimator.StartValue(currentPadding), endPadding) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)

        val set = AnimatorSet()
        set.playTogether(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        transitionOptions.animatorListener?.let { set.addListener(it) }

        return set
    }

    override fun transitionFromHighZoomToLowZoom(
        transitionOptions: NavigationCameraZoomTransitionOptions): AnimatorSet {
        val currentMapCameraOptions = mapboxMap.getCameraOptions(null)

        var bearingShortestRotation = transitionOptions.bearing
        currentMapCameraOptions.bearing?.let {
            bearingShortestRotation = it + shortestRotation(it, transitionOptions.bearing)
        }

        var currentPadding = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        currentMapCameraOptions.padding?.let {
            currentPadding = it
        }

        val centerAnimator = CameraCenterAnimator.create(transitionOptions.center) {
            startDelay = 800
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val zoomAnimator = CameraZoomAnimator.create(
            // workaround for https://github.com/mapbox/mapbox-maps-android/issues/785
            AbstractCameraAnimator.StartValue(currentMapCameraOptions.zoom ?: transitionOptions.zoom),
            transitionOptions.zoom
        ) {
            startDelay = 0
            duration = 1800
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val bearingAnimator = CameraBearingAnimator.create(bearingShortestRotation) {
            startDelay = 600
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val pitchAnimator = CameraPitchAnimator.create(transitionOptions.pitch) {
            startDelay = 0
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val startPadding = currentPadding
        val endPadding = convertScreenCenterOffsetToEdgeInsets(mapboxMap.getSize(), transitionOptions.anchorOffset)
        val anchorAnimator = CameraPaddingAnimator.create(AbstractCameraAnimator.StartValue(startPadding), endPadding) {
            startDelay = 0
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)

        val set = AnimatorSet()
        set.playTogether(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        transitionOptions.animatorListener?.let { set.addListener(it) }

        return set
    }

    override fun transitionLinear(
        transitionOptions: NavigationCameraLinearTransitionOptions): AnimatorSet {
        val currentMapCamera = mapboxMap.getCameraOptions(null)

        val centerAnimator = CameraCenterAnimator.create(transitionOptions.center) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }
        val zoomAnimator = CameraZoomAnimator.create(
            // workaround for https://github.com/mapbox/mapbox-maps-android/issues/785
            AbstractCameraAnimator.StartValue(currentMapCamera.zoom ?: transitionOptions.zoom),
            transitionOptions.zoom
        ) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }

        var bearingShortestRotation = transitionOptions.bearing
        currentMapCamera.bearing?.let {
            bearingShortestRotation = it + shortestRotation(it, transitionOptions.bearing)
        }
        val bearingAnimator = CameraBearingAnimator.create(bearingShortestRotation) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }
        val pitchAnimator = CameraPitchAnimator.create(transitionOptions.pitch) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }

        var currentPadding = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        currentMapCamera.padding?.let {
            currentPadding = it
        }
        val endPadding = convertScreenCenterOffsetToEdgeInsets(mapboxMap.getSize(), transitionOptions.anchorOffset)
        val anchorAnimator = CameraPaddingAnimator.create(AbstractCameraAnimator.StartValue(currentPadding), endPadding) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        val set = AnimatorSet()
        set.playTogether(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        transitionOptions.animatorListener?.let { set.addListener(it) }

        return set
    }

    private fun shortestRotation(from: Double, to: Double): Double {
        return (to - from + 540) % 360 - 180
    }

    private fun convertScreenCenterOffsetToEdgeInsets(mapSize: Size, centerOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)): EdgeInsets {
        val mapCenterScreenCoordinate = ScreenCoordinate((mapSize.width / 2).toDouble(), (mapSize.height / 2).toDouble())
        val top = mapCenterScreenCoordinate.y + centerOffset.y
        val left = mapCenterScreenCoordinate.x + centerOffset.x
        return getScaledEdgeInsets(EdgeInsets(top, left, mapSize.height - top, mapSize.width - left))
    }

}
