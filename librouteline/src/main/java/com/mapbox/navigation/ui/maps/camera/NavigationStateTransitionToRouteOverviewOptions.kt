package com.mapbox.navigation.ui.maps.camera

import android.animation.Animator
import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets

/**
 * This value will be used for changing camera state to vehicle following.
 *
 * @param vehicleLocation current vehicle location
 * @param remainingPointsOfRoute the points remaining in the current route which will
 *  be displayed within the route overview viewport
 * @param additionalPointsToFrame additional points that will be displayed within the viewport
 * @param pitch use for [com.mapbox.maps.plugin.animation.animator.CameraPitchAnimator]
 * @param padding use for [com.mapbox.maps.plugin.animation.animator.CameraAnchorAnimator]
 * @param lookaheadDistanceForBearingSmoothing use to smooth camera rotations while traversing a route line
 * @param maxZoom the max camera zoom during the transition
 * @param animatorListener callbacks that will be called of different transition animation states
 */
class NavigationStateTransitionToRouteOverviewOptions private constructor(
    val vehicleLocation: Location,
    val remainingPointsOfRoute: List<Point>,
    val additionalPointsToFrame: List<Point>,
    val pitch: Double,
    val padding: EdgeInsets,
    val lookaheadDistanceForBearingSmoothing: Double,
    val maxZoom: Double,
    val animatorListener: Animator.AnimatorListener?
) {
    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(vehicleLocation, remainingPointsOfRoute).apply {
        additionalPointsToFrame(additionalPointsToFrame)
        pitch(pitch)
        padding(padding)
        lookaheadDistanceForBearingSmoothing(lookaheadDistanceForBearingSmoothing)
        maxZoom(maxZoom)
        animatorListener(animatorListener)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationStateTransitionToRouteOverviewOptions

        if (vehicleLocation != other.vehicleLocation) return false
        if (remainingPointsOfRoute != other.remainingPointsOfRoute) return false
        if (additionalPointsToFrame != other.additionalPointsToFrame) return false
        if (pitch != other.pitch) return false
        if (padding != other.padding) return false
        if (lookaheadDistanceForBearingSmoothing != other.lookaheadDistanceForBearingSmoothing) return false
        if (maxZoom != other.maxZoom) return false
        if (animatorListener != other.animatorListener) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vehicleLocation.hashCode()
        result = 31 * result + remainingPointsOfRoute.hashCode()
        result = 31 * result + additionalPointsToFrame.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + padding.hashCode()
        result = 31 * result + lookaheadDistanceForBearingSmoothing.hashCode()
        result = 31 * result + maxZoom.hashCode()
        result = 31 * result + (animatorListener?.hashCode() ?: 0)
        return result
    }

    /**
     * Build a new [NavigationStateTransitionToRouteOverviewOptions]
     */
    class Builder(private var vehicleLocation: Location,
                  private var remainingPointsOfRoute: List<Point>) {
        private var additionalPointsToFrame: List<Point> = emptyList()
        private var pitch: Double = MAPBOX_CAMERA_OPTION_FOLLOWING_PITCH
        private var padding: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        private var lookaheadDistanceForBearingSmoothing: Double = 0.0
        private var maxZoom: Double = MAPBOX_CAMERA_OPTION_MAX_ZOOM
        private var animatorListener: Animator.AnimatorListener? = null

        /**
         * Override [vehicleLocation]
         */
        fun vehicleLocation(vehicleLocation: Location): Builder = apply {
            this.vehicleLocation = vehicleLocation
        }

        /**
         * Override [remainingPointsOfRoute]
         */
        fun remainingPointsOfRoute(remainingPointsOfRoute: List<Point>): Builder = apply {
            this.remainingPointsOfRoute = remainingPointsOfRoute
        }

        /**
         * Override [additionalPointsToFrame]
         */
        fun additionalPointsToFrame(additionalPointsToFrame: List<Point>): Builder = apply {
            this.additionalPointsToFrame = additionalPointsToFrame
        }

        /**
         * Override [pitch]
         */
        fun pitch(pitch: Double): Builder = apply {
            this.pitch = pitch
        }

        /**
         * Override [padding]
         */
        fun padding(padding: EdgeInsets): Builder = apply {
            this.padding = padding
        }

        /**
         * Override [lookaheadDistanceForBearingSmoothing]
         */
        fun lookaheadDistanceForBearingSmoothing(lookaheadDistanceForBearingSmoothing: Double): Builder = apply {
            this.lookaheadDistanceForBearingSmoothing = lookaheadDistanceForBearingSmoothing
        }

        /**
         * Override [maxZoom]
         */
        fun maxZoom(maxZoom: Double): Builder = apply {
            this.maxZoom = maxZoom
        }

        /**
         * Override [animatorListener]
         */
        fun animatorListener(animatorListener: Animator.AnimatorListener?): Builder = apply {
            this.animatorListener = animatorListener
        }

        /**
         * Build a new instance of [NavigationStateTransitionToRouteOverviewOptions]
         * @return NavigationStateTransitionToRouteOverviewOptions
         */
        fun build(): NavigationStateTransitionToRouteOverviewOptions =
            NavigationStateTransitionToRouteOverviewOptions(
                vehicleLocation,
                remainingPointsOfRoute,
                additionalPointsToFrame,
                pitch,
                padding,
                lookaheadDistanceForBearingSmoothing,
                maxZoom,
                animatorListener
            )
    }
}
