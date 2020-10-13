package com.mapbox.navigation.ui.maps.camera

import android.animation.Animator
import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets

class NavigationStateTransitionOptionsToRouteOverview private constructor(
    val vehicleLocation: Location,
    val remainingPointsOfRoute: List<Point>,
    val additionalPointsToFrame: List<Point>,
    val pitch: Double,
    val padding: EdgeInsets,
    val bearingAdjustment: Double,
    val maxZoom: Double,
    val animatorListener: Animator.AnimatorListener?
) {
    fun toBuilder(): Builder = Builder(vehicleLocation, remainingPointsOfRoute).apply {
        additionalPointsToFrame(additionalPointsToFrame)
        pitch(pitch)
        padding(padding)
        bearingAdjustment(bearingAdjustment)
        maxZoom(maxZoom)
        animatorListener(animatorListener)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationStateTransitionOptionsToRouteOverview

        if (vehicleLocation != other.vehicleLocation) return false
        if (remainingPointsOfRoute != other.remainingPointsOfRoute) return false
        if (additionalPointsToFrame != other.additionalPointsToFrame) return false
        if (pitch != other.pitch) return false
        if (padding != other.padding) return false
        if (bearingAdjustment != other.bearingAdjustment) return false
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
        result = 31 * result + bearingAdjustment.hashCode()
        result = 31 * result + maxZoom.hashCode()
        result = 31 * result + (animatorListener?.hashCode() ?: 0)
        return result
    }


    class Builder(private var vehicleLocation: Location,
                  private var remainingPointsOfRoute: List<Point>) {
        private var additionalPointsToFrame: List<Point> = emptyList()
        private var pitch: Double = MAPBOX_CAMERA_OPTION_FOLLOWING_PITCH
        private var padding: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        private var bearingAdjustment: Double = 0.0
        private var maxZoom: Double = MAPBOX_CAMERA_OPTION_MAX_ZOOM
        private var animatorListener: Animator.AnimatorListener? = null

        fun vehicleLocation(vehicleLocation: Location): Builder = apply {
            this.vehicleLocation = vehicleLocation
        }

        fun remainingPointsOfRoute(remainingPointsOfRoute: List<Point>): Builder = apply {
            this.remainingPointsOfRoute = remainingPointsOfRoute
        }

        fun additionalPointsToFrame(additionalPointsToFrame: List<Point>): Builder = apply {
            this.additionalPointsToFrame = additionalPointsToFrame
        }

        fun pitch(pitch: Double): Builder = apply {
            this.pitch = pitch
        }

        fun padding(padding: EdgeInsets): Builder = apply {
            this.padding = padding
        }

        fun bearingAdjustment(bearingAdjustment: Double): Builder = apply {
            this.bearingAdjustment = bearingAdjustment
        }

        fun maxZoom(maxZoom: Double): Builder = apply {
            this.maxZoom = maxZoom
        }

        fun animatorListener(animatorListener: Animator.AnimatorListener?): Builder = apply {
            this.animatorListener = animatorListener
        }

        fun build(): NavigationStateTransitionOptionsToRouteOverview =
            NavigationStateTransitionOptionsToRouteOverview(
                vehicleLocation,
                remainingPointsOfRoute,
                additionalPointsToFrame,
                pitch,
                padding,
                bearingAdjustment,
                maxZoom,
                animatorListener
            )
    }
}
