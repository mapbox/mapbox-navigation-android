package com.mapbox.navigation.ui.maps.camera

import android.animation.Animator
import com.mapbox.geojson.Point
import com.mapbox.maps.ScreenCoordinate

class NavigationCameraZoomTransitionOptions private constructor(
    val center: Point,
    val zoom: Double,
    val bearing: Double,
    val pitch: Double,
    val anchorOffset: ScreenCoordinate,
    val animatorListener: Animator.AnimatorListener?
) {
    fun toBuilder(): Builder = Builder(center, zoom).apply {
        bearing(bearing)
        pitch(pitch)
        anchorOffset(anchorOffset)
        animatorListener(animatorListener)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationCameraZoomTransitionOptions

        if (center != other.center) return false
        if (zoom != other.zoom) return false
        if (bearing != other.bearing) return false
        if (pitch != other.pitch) return false
        if (anchorOffset != other.anchorOffset) return false
        if (animatorListener != other.animatorListener) return false

        return true
    }

    override fun hashCode(): Int {
        var result = center.hashCode()
        result = 31 * result + zoom.hashCode()
        result = 31 * result + bearing.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + anchorOffset.hashCode()
        result = 31 * result + (animatorListener?.hashCode() ?: 0)
        return result
    }

    class Builder(private var center: Point,
                  private var zoom: Double) {
        private var bearing: Double = 0.0
        private var pitch: Double = MAPBOX_CAMERA_OPTION_FOLLOWING_PITCH
        private var anchorOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)
        private var animatorListener: Animator.AnimatorListener? = null

        fun center(center: Point): Builder = apply {
            this.center = center
        }

        fun zoom(zoom: Double): Builder = apply {
            this.zoom = zoom
        }

        fun bearing(bearing: Double): Builder = apply {
            this.bearing = bearing
        }

        fun pitch(pitch: Double): Builder = apply {
            this.pitch = pitch
        }

        fun anchorOffset(anchorOffset: ScreenCoordinate): Builder = apply {
            this.anchorOffset = anchorOffset
        }

        fun animatorListener(animatorListener: Animator.AnimatorListener?): Builder = apply {
            this.animatorListener = animatorListener
        }

        fun build(): NavigationCameraZoomTransitionOptions =
            NavigationCameraZoomTransitionOptions(
                center, zoom, bearing, pitch, anchorOffset, animatorListener
            )
    }
}
