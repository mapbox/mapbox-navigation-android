package com.mapbox.navigation.carbon.examples

import android.animation.Animator
import android.animation.AnimatorSet
import androidx.core.view.animation.PathInterpolatorCompat
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.*
import com.mapbox.maps.plugin.animation.animator.*
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.gesture.OnMoveListener
import com.mapbox.maps.plugin.gesture.GesturePluginImpl
import com.mapbox.maps.plugin.location.LocationComponentPlugin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc
import kotlinx.android.synthetic.main.layout_camera_animations.*
import java.util.*

enum class NavigationMapAnimatorState(val id_string: String) {
    FREE("Free"),
    FOLLOWING("Following"),
    OVERVIEW("Overview"),
    TRANSITION_TO_FOLLOWING("To_following"),
    TRANSITION_TO_OVERVIEW("To_overview")
}

interface NavigationMapAnimatorChangeObserver {
    fun onNavigationMapAnimatorChanged()
}

class NavigationMapAnimator constructor(mapView: MapView): OnMoveListener, RouteProgressObserver {

    var state = NavigationMapAnimatorState.FREE
        private set(value) {
            field = value
            notifyStateChange()
        }
    var edgeInsets = EdgeInsets(20.0, 20.0, 20.0, 20.0)
    var followingPitch = 40.0
    var maxZoom = 17.0

    val currentCameraOptions: CameraOptions?
        get() {
            return mapboxMap.getCameraOptions(null)
        }

    private var mapboxMap = mapView.getMapboxMap()
    private var mapCamera = mapView.getCameraAnimationsPlugin()
    private var locationComponent = mapView.getPlugin(LocationComponentPlugin::class.java)
    private var gesturePlugin = mapView.getPlugin(GesturePluginImpl::class.java)

    var route: DirectionsRoute? = null
        set(value) {
            field = value
            makeRoutePoints()
        }
    private var fullRoutePoints: List<Point> = emptyList()
    private var routeStepPoints: List<List<List<Point>>> = emptyList()

    private var currentRouteRemainingPointsOnStep: List<Point> = emptyList()
    private var currentRouteRemainingPointsOnRoute: List<Point> = emptyList()

    private var observers: MutableList<NavigationMapAnimatorChangeObserver> = emptyList<NavigationMapAnimatorChangeObserver>().toMutableList()

    init {
        mapCamera.addCameraCenterChangeListener(object : CameraCenterAnimator.ChangeListener {
            override fun onChanged(updatedValue: Point) {
                notifyCameraChange()
            }
        })
        mapCamera.addCameraZoomChangeListener(object : CameraZoomAnimator.ChangeListener {
            override fun onChanged(updatedValue: Double) {
                notifyCameraChange()
            }
        })
        mapCamera.addCameraBearingChangeListener(object : CameraBearingAnimator.ChangeListener {
            override fun onChanged(updatedValue: Double) {
                notifyCameraChange()
            }
        })
        mapCamera.addCameraPitchChangeListener(object : CameraPitchAnimator.ChangeListener {
            override fun onChanged(updatedValue: Double) {
                notifyCameraChange()
            }
        })
        mapCamera.addCameraPaddingChangeListener(object : CameraPaddingAnimator.ChangeListener {
            override fun onChanged(updatedValue: EdgeInsets) {
                notifyCameraChange()
            }
        })

        gesturePlugin?.addOnMoveListener(this)
    }

    fun transitionToVehicleFollowing() {
        val location = locationComponent?.lastKnownLocation
        location?.let {
            val points = currentRouteRemainingPointsOnStep
            state = NavigationMapAnimatorState.TRANSITION_TO_FOLLOWING
            val zoomAndCenter = getZoomLevelAndCenterCoordinate(points, it.bearing.toDouble(), followingPitch, edgeInsets)
            val center = Point.fromLngLat(it.longitude, it.latitude)
            val zoomLevel = Math.min(zoomAndCenter.first, maxZoom)
            val yOffset = mapboxMap.getSize().height / 2 - edgeInsets.bottom
            transitionFromLowZoomToHighZoom(center, it.bearing.toDouble(), zoomLevel, followingPitch, ScreenCoordinate(0.0, yOffset)) {
                state = NavigationMapAnimatorState.FOLLOWING
            }
        }
    }

    fun transitionToRouteOverview() {
        val location = locationComponent?.lastKnownLocation
        location?.let {
            val points = currentRouteRemainingPointsOnRoute
            state = NavigationMapAnimatorState.TRANSITION_TO_OVERVIEW
            val zoomAndCenter = getZoomLevelAndCenterCoordinate(points, 0.0, 0.0, edgeInsets)
            val center = zoomAndCenter.second
            val zoomLevel = Math.min(zoomAndCenter.first, maxZoom)
            val currentMapCamera = mapboxMap.getCameraOptions(null)
            if (currentMapCamera.zoom ?: 2.0 < zoomAndCenter.first) {
                transitionFromLowZoomToHighZoom(center, 0.0, zoomLevel, 0.0, ScreenCoordinate(0.0, 0.0)) {
                    state = NavigationMapAnimatorState.OVERVIEW
                }
            } else {
                transitionFromHighZoomToLowZoom(center, 0.0, zoomLevel, 0.0, ScreenCoordinate(0.0, 0.0)) {
                    state = NavigationMapAnimatorState.OVERVIEW
                }
            }
        }
    }

    fun recenter() {

    }

    fun registerChangeListener(listener: NavigationMapAnimatorChangeObserver) {
        observers.add(listener)
    }

    private fun notifyStateChange() {
        observers.forEach { it.onNavigationMapAnimatorChanged() }
    }

    private var timeofLastCameraChangeViewUpdate: Date = Date()

    private fun notifyCameraChange() {
        val newDate = Date()
        val timeSinceLastUpdate = newDate.time - timeofLastCameraChangeViewUpdate.time
        if (timeSinceLastUpdate < 16) {
            return
        }
        observers.forEach { it.onNavigationMapAnimatorChanged() }
    }

    private fun makeRoutePoints() {
        route?.let {
            fullRoutePoints = PolylineUtils.decode(it.geometry()!!, 6)
            routeStepPoints = it.legs()!!.map { it.steps()!!.map { PolylineUtils.decode(it.geometry()!!, 6).toList() }}
            currentRouteRemainingPointsOnRoute = routeStepPoints[0].flatten()
            currentRouteRemainingPointsOnStep = routeStepPoints[0][0]

            // This won't work for multi-leg routes but I need to initialize the currentRouteRemaining points
        }
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        if (route != routeProgress.route) {
            route = routeProgress.route
        }
        val currentStepProgress = routeProgress.currentLegProgress?.currentStepProgress
        val currentLegIndex = routeProgress.currentLegProgress?.legIndex ?: 0
        val currentStepIndex = currentStepProgress?.stepIndex ?: 0
        var remainingPointsOnStep: List<Point> = emptyList()
        var remainingPointsAfterStep: List<Point> = emptyList()
        currentStepProgress?.let {
            val fullStepPoints = it.stepPoints ?: emptyList()
            var distanceTraveledOnStepKM = Math.max(it.distanceTraveled / 1000.0, 0.0)
            val fullDistanceOfCurrentStepKM = Math.max((it.distanceRemaining + it.distanceTraveled) / 1000.0, 0.0)
            if (distanceTraveledOnStepKM > fullDistanceOfCurrentStepKM) distanceTraveledOnStepKM = 0.0
            try {
                val remainingLineStringOnStep = TurfMisc.lineSliceAlong(LineString.fromLngLats(fullStepPoints), distanceTraveledOnStepKM.toDouble(), fullDistanceOfCurrentStepKM.toDouble(), TurfConstants.UNIT_KILOMETERS)
                remainingPointsOnStep = remainingLineStringOnStep.coordinates()
            } catch (e: TurfException) {
                return
            }
        }
        val pointsForStepsOnCurrentLeg = routeStepPoints[currentLegIndex]
        val remainingStepsAfterStep = if (currentStepIndex < pointsForStepsOnCurrentLeg.size) pointsForStepsOnCurrentLeg.slice(currentStepIndex + 1..pointsForStepsOnCurrentLeg.size-1) else emptyList()
        remainingPointsAfterStep = remainingStepsAfterStep.flatten()

        val remainingPointsOnRoute = listOf<List<Point>>(remainingPointsOnStep, remainingPointsAfterStep).flatten()

        currentRouteRemainingPointsOnRoute = remainingPointsOnRoute
        currentRouteRemainingPointsOnStep = remainingPointsOnStep

        updateCameraTracking()
    }

    private fun updateCameraTracking() {
        if (state == NavigationMapAnimatorState.FOLLOWING) {
            updateMapFrameForFollowing()
        } else if (state == NavigationMapAnimatorState.OVERVIEW) {
            updateMapFrameForOverview()
        }
    }

    private fun updateMapFrameForFollowing() {
        val points = currentRouteRemainingPointsOnStep
        val location = locationComponent?.lastKnownLocation
        location?.let {
            val bearing = it.bearing
            val zoomAndCenter = getZoomLevelAndCenterCoordinate(points, 0.0, 0.0, edgeInsets)
            val zoomLevel = Math.min(zoomAndCenter.first, maxZoom)
            val center = Point.fromLngLat(it.longitude, it.latitude)
            val yOffset = mapboxMap.getSize().height / 2 - edgeInsets.bottom
            transitionLinear(center, bearing.toDouble(), zoomLevel, followingPitch, ScreenCoordinate(0.0, yOffset)) {

            }
        }
    }

    private fun updateMapFrameForOverview() {
        val points = currentRouteRemainingPointsOnRoute
        val zoomAndCenter = getZoomLevelAndCenterCoordinate(points, 0.0, 0.0, edgeInsets)
        val zoomLevel = Math.min(zoomAndCenter.first, maxZoom)
        val center = zoomAndCenter.second
        transitionLinear(center, 0.0, zoomLevel, 0.0, ScreenCoordinate(0.0, 0.0)) {

        }
    }

    private fun shortestRotation(from: Double, to: Double): Double {
        return (to - from + 540) % 360 - 180
    }

    private fun getEdgeInsetsFromScreenCenterOffset(mapSize: Size, centerOffset: ScreenCoordinate = ScreenCoordinate(0.0,0.0)): EdgeInsets {
        val mapCenterScreenCoordinate = ScreenCoordinate((mapSize.width / 2).toDouble(), (mapSize.height / 2).toDouble())
        return EdgeInsets(mapCenterScreenCoordinate.y + centerOffset.y, mapCenterScreenCoordinate.x + centerOffset.x, mapCenterScreenCoordinate.y - centerOffset.y, mapCenterScreenCoordinate.x - centerOffset.x)
    }

    private fun getZoomLevelAndCenterCoordinate(points: List<Point>, bearing: Double, pitch: Double, edgeInsets: EdgeInsets): Pair<Double, Point> {
        val cam = mapboxMap.cameraForCoordinates(points, edgeInsets, bearing, pitch)

        if (cam.zoom != null && cam.center != null) {
            return Pair(cam.zoom!! - 0.2, cam.center!!)
        }
        return Pair(2.0, Point.fromLngLat(0.0,0.0))
    }

    private fun transitionFromLowZoomToHighZoom(center: Point, bearing: Double, zoomLevel: Double, pitch: Double, anchorOffset: ScreenCoordinate, onComplete: () -> Unit) {
        val currentMapCamera = mapboxMap.getCameraOptions(null)
        val currentMapCameraCenter = currentMapCamera.center
        var screenDistanceFromMapCenterToLocation = 0.0
        currentMapCameraCenter?.let {
            val currentCenterScreenCoordinate = mapboxMap.pixelForCoordinate(Point.fromLngLat(it.longitude(), it.latitude()))
            val locationScreenCoordinate = mapboxMap.pixelForCoordinate(center)
            screenDistanceFromMapCenterToLocation = Math.hypot(currentCenterScreenCoordinate.x - locationScreenCoordinate.x, currentCenterScreenCoordinate.y - locationScreenCoordinate.y)
        }

        var bearingShortestRotation = bearing
        currentMapCamera.bearing?.let {
            bearingShortestRotation = it + shortestRotation(it, bearing)
        }

        val currentMapCameraZoomLevel = currentMapCamera.zoom
        var zoomLevelDelta = 0.0
        currentMapCameraZoomLevel?.let {
            zoomLevelDelta = Math.abs(zoomLevel - it)
        }

        var currentPadding = edgeInsets
        currentMapCamera.padding?.let {
            currentPadding = it
        }

        val centerAnimationRate = 300.0
        val centerDuration = Math.min((screenDistanceFromMapCenterToLocation / centerAnimationRate) * 1000.0, 3000.0)

        val zoomAnimationRate = 2.0
        val zoomDelay = centerDuration * 0.3
        val zoomDuration = Math.min((zoomLevelDelta / zoomAnimationRate) * 1000.0, 3000.0)

        val bearingAnimationRate = 100.0
        val bearingDuration = 1800.0
        val bearingDelay = Math.max(zoomDelay + zoomDuration - bearingDuration , 0.0)

        val pitchAndAnchorDuration = 1200.0
        val pitchAndAnchorDelay = Math.max(zoomDelay + zoomDuration - pitchAndAnchorDuration + 100 , 0.0)

        val centerAnimator = CameraCenterAnimator.create(center) {
            duration = centerDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val zoomAnimator = CameraZoomAnimator.create(zoomLevel) {
            startDelay = zoomDelay.toLong()
            duration = zoomDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val bearingAnimator = CameraBearingAnimator.create(bearingShortestRotation) {
            startDelay = bearingDelay.toLong()
            duration = bearingDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val pitchAnimator = CameraPitchAnimator.create(pitch) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val startPadding = currentPadding
        val endPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), anchorOffset)

        val anchorAnimator = CameraPaddingAnimator.create(CameraAnimator.StartValue(startPadding), endPadding) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        val set = AnimatorSet()
        set.playTogether(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        set.start()
        set.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                onComplete()
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }

        })
    }

    private fun transitionFromHighZoomToLowZoom(center: Point, bearing: Double, zoomLevel: Double, pitch: Double, anchorOffset: ScreenCoordinate, onComplete: () -> Unit) {
        val currentMapCamera = mapboxMap.getCameraOptions(null)

        var bearingShortestRotation = bearing
        currentMapCamera.bearing?.let {
            bearingShortestRotation = it + shortestRotation(it, bearing)
        }

        var currentPadding = EdgeInsets(0.0,0.0,0.0,0.0)
        currentMapCamera.padding?.let {
            currentPadding = it
        }

        val centerAnimator = CameraCenterAnimator.create(center) {
            startDelay = 800
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val zoomAnimator = CameraZoomAnimator.create(zoomLevel) {
            startDelay = 0
            duration = 1800
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val bearingAnimator = CameraBearingAnimator.create(bearingShortestRotation) {
            startDelay = 600
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val pitchAnimator = CameraPitchAnimator.create(pitch) {
            startDelay = 0
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val startPadding = currentPadding
        val endPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), anchorOffset)

        val anchorAnimator = CameraPaddingAnimator.create(CameraAnimator.StartValue(startPadding), endPadding) {
            startDelay = 0
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        mapCamera.addCameraCenterChangeListener(object : CameraCenterAnimator.ChangeListener {
            override fun onChanged(updatedValue: Point) {
                notifyCameraChange()
            }
        })
        val set = AnimatorSet()
        set.playTogether(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        set.start()
        set.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                onComplete()
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }

        })
    }

    private fun transitionLinear(center: Point, bearing: Double, zoomLevel: Double, pitch: Double, anchorOffset: ScreenCoordinate, onComplete: () -> Unit) {
        val currentMapCamera = mapboxMap.getCameraOptions(null)

        var bearingShortestRotation = bearing
        currentMapCamera.bearing?.let {
            bearingShortestRotation = it + shortestRotation(it, bearing)
        }

        var currentPadding = EdgeInsets(0.0,0.0,0.0,0.0)
        currentMapCamera.padding?.let {
            currentPadding = it
        }

        val centerAnimator = CameraCenterAnimator.create(center) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }
        val zoomAnimator = CameraZoomAnimator.create(zoomLevel) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }
        val bearingAnimator = CameraBearingAnimator.create(bearingShortestRotation) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }
        val pitchAnimator = CameraPitchAnimator.create(pitch) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }

        val startPadding = currentPadding
        val endPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), anchorOffset)

        val anchorAnimator = CameraPaddingAnimator.create(CameraAnimator.StartValue(startPadding), endPadding) {
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0f, 0f, 1f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        val set = AnimatorSet()
        set.playTogether(centerAnimator, zoomAnimator, bearingAnimator, pitchAnimator, anchorAnimator)
        set.start()
    }

    override fun onMove(detector: MoveGestureDetector) {

    }

    override fun onMoveBegin(detector: MoveGestureDetector) {
        state = NavigationMapAnimatorState.FREE
    }

    override fun onMoveEnd(detector: MoveGestureDetector) {

    }
}
