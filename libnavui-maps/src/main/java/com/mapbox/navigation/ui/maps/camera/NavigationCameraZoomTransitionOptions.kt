package com.mapbox.navigation.ui.maps.camera

import android.animation.Animator
import com.mapbox.geojson.Point
import com.mapbox.maps.ScreenCoordinate

/**
 * This value will be used for navigation camera zoom transition use.
 *
 * @param center use for [com.mapbox.maps.plugin.animation.animator.CameraCenterAnimator]
 * @param zoom use for [com.mapbox.maps.plugin.animation.animator.CameraZoomAnimator]
 * @param bearing use for [com.mapbox.maps.plugin.animation.animator.CameraBearingAnimator]
 * @param pitch use for [com.mapbox.maps.plugin.animation.animator.CameraPitchAnimator]
 * @param anchorOffset use for [com.mapbox.maps.plugin.animation.animator.CameraAnchorAnimator]
 * @param animatorListener callbacks that will be called of different transition animation states
 */
class NavigationCameraZoomTransitionOptions private constructor(
    val center: Point,
    val zoom: Double,
    val bearing: Double,
    val pitch: Double,
    val anchorOffset: ScreenCoordinate,
    val animatorListener: Animator.AnimatorListener?
) {
    /**
     * Get a builder to customize a subset of current options.
     */
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

    /**
     * Build a new [NavigationCameraZoomTransitionOptions]
     */
    class Builder(private var center: Point,
                  private var zoom: Double) {
        private var bearing: Double = 0.0
        private var pitch: Double = 0.0
        private var anchorOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)
        private var animatorListener: Animator.AnimatorListener? = null

        /**
         * Override [center] that use for [com.mapbox.maps.plugin.animation.animator.CameraCenterAnimator]
         */
        fun center(center: Point): Builder = apply {
            this.center = center
        }

        /**
         * Override [zoom] that use for [com.mapbox.maps.plugin.animation.animator.CameraZoomAnimator]
         */
        fun zoom(zoom: Double): Builder = apply {
            this.zoom = zoom
        }

        /**
         * Override [bearing] that use for [com.mapbox.maps.plugin.animation.animator.CameraBearingAnimator]
         */
        fun bearing(bearing: Double): Builder = apply {
            this.bearing = bearing
        }

        /**
         * Override [pitch] that use for [com.mapbox.maps.plugin.animation.animator.CameraPitchAnimator]
         */
        fun pitch(pitch: Double): Builder = apply {
            this.pitch = pitch
        }

        /**
         * Override [anchorOffset] that use for [com.mapbox.maps.plugin.animation.animator.CameraAnchorAnimator]
         */
        fun anchorOffset(anchorOffset: ScreenCoordinate): Builder = apply {
            this.anchorOffset = anchorOffset
        }

        /**
         * Override [animatorListener]
         */
        fun animatorListener(animatorListener: Animator.AnimatorListener?): Builder = apply {
            this.animatorListener = animatorListener
        }

        /**
         * Build a new instance of [NavigationCameraZoomTransitionOptions]
         * @return NavigationCameraZoomTransitionOptions
         */
        fun build(): NavigationCameraZoomTransitionOptions =
            NavigationCameraZoomTransitionOptions(
                center, zoom, bearing, pitch, anchorOffset, animatorListener
            )
    }
}
