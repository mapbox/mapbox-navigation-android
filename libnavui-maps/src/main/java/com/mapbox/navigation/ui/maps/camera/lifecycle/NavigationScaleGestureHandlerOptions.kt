package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.content.Context
import android.graphics.RectF
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

/**
 * Defines customizable options for [NavigationScaleGestureHandler].
 */
class NavigationScaleGestureHandlerOptions private constructor(
    /* fixme we shouldn't hold on to context,
     * we need to change the BuilderTest to unlock this use-case
     */
    private val context: Context,
    /**
     * When in [NavigationCameraState.FOLLOWING], sets minimum single pointer movement (map pan)
     * in pixels required to transition [NavigationCamera] to [NavigationCameraState.IDLE].
     */
    val followingInitialMoveThreshold: Float,
    /**
     * When in [NavigationCameraState.FOLLOWING], sets minimum multi pointer movement (map pan)
     * in pixels required to transition [NavigationCamera] to [NavigationCameraState.IDLE]
     * (for example during pinch scale gesture).
     */
    val followingMultiFingerMoveThreshold: Float,
    /**
     * When in [NavigationCameraState.FOLLOWING], sets protected multi pointer gesture area.
     *
     * Any multi finger gesture with focal point inside the provided screen coordinate rectangle
     * is not going to transition [NavigationCamera] to [NavigationCameraState.IDLE].
     *
     * Best paired with the [followingMultiFingerMoveThreshold] set to 0 or a relatively small value
     * to not interfere with gestures outside of the defined rectangle.
     */
    val followingMultiFingerProtectedMoveArea: RectF?,
    /**
     * When in [NavigationCameraState.FOLLOWING], sets minimum rotation angle in degrees required
     * to start rotation gesture.
     */
    val followingRotationAngleThreshold: Float,
) {

    /**
     * Rebuilds the options.
     */
    fun toBuilder(): Builder = Builder(context)
        .followingInitialMoveThreshold(followingInitialMoveThreshold)
        .followingMultiFingerMoveThreshold(followingMultiFingerMoveThreshold)
        .followingMultiFingerProtectedMoveArea(followingMultiFingerProtectedMoveArea)
        .followingRotationAngleThreshold(followingRotationAngleThreshold)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationScaleGestureHandlerOptions

        if (context != other.context) return false
        if (followingInitialMoveThreshold != other.followingInitialMoveThreshold) return false
        if (followingMultiFingerMoveThreshold != other.followingMultiFingerMoveThreshold) {
            return false
        }
        if (followingMultiFingerProtectedMoveArea != other.followingMultiFingerProtectedMoveArea) {
            return false
        }
        if (followingRotationAngleThreshold != other.followingRotationAngleThreshold) {
            return false
        }

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + followingInitialMoveThreshold.hashCode()
        result = 31 * result + followingMultiFingerMoveThreshold.hashCode()
        result = 31 * result + followingMultiFingerProtectedMoveArea.hashCode()
        result = 31 * result + followingRotationAngleThreshold.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationScaleGestureHandlerOptions(" +
            "context=$context, " +
            "followingInitialMoveThreshold=$followingInitialMoveThreshold, " +
            "followingMultiFingerMoveThreshold=$followingMultiFingerMoveThreshold, " +
            "followingMultiFingerProtectedMoveArea=$followingMultiFingerProtectedMoveArea, " +
            "followingRotationAngleThreshold=$followingRotationAngleThreshold" +
            ")"
    }

    /**
     * Builds [NavigationScaleGestureHandlerOptions].
     */
    class Builder(private val context: Context) {
        private var followingInitialMoveThreshold: Float =
            context.resources.getDimension(
                R.dimen.mapbox_navigationCamera_trackingInitialMoveThreshold,
            )
        private var followingMultiFingerMoveThreshold: Float =
            context.resources.getDimension(
                R.dimen.mapbox_navigationCamera_trackingMultiFingerMoveThreshold,
            )
        private var followingMultiFingerProtectedMoveArea: RectF? = null
        private var followingRotationAngleThreshold: Float = 5.0f

        /**
         * When in [NavigationCameraState.FOLLOWING], sets minimum single pointer movement (map pan)
         * in pixels required to transition [NavigationCamera] to [NavigationCameraState.IDLE].
         *
         * Defaults to 25dp.
         */
        fun followingInitialMoveThreshold(followingInitialMoveThreshold: Float): Builder = apply {
            this.followingInitialMoveThreshold = followingInitialMoveThreshold
        }

        /**
         * When in [NavigationCameraState.FOLLOWING], sets minimum multi pointer movement (map pan)
         * in pixels required to transition [NavigationCamera] to [NavigationCameraState.IDLE]
         * (for example during pinch scale gesture).
         *
         * Defaults to 400dp.
         */
        fun followingMultiFingerMoveThreshold(followingMultiFingerMoveThreshold: Float): Builder =
            apply {
                this.followingMultiFingerMoveThreshold = followingMultiFingerMoveThreshold
            }

        /**
         * When in [NavigationCameraState.FOLLOWING], sets protected multi pointer gesture area.
         *
         * Any multi finger gesture with focal point inside the provided screen coordinate rectangle
         * is not going to transition [NavigationCamera] to [NavigationCameraState.IDLE].
         *
         * Best paired with the [followingMultiFingerMoveThreshold] set to 0 or a relatively small value
         * to not interfere with gestures outside of the defined rectangle.
         *
         * Defaults to `null`.
         */
        fun followingMultiFingerProtectedMoveArea(
            followingMultiFingerProtectedMoveArea: RectF?,
        ): Builder = apply {
            this.followingMultiFingerProtectedMoveArea = followingMultiFingerProtectedMoveArea
        }

        /**
         * When in [NavigationCameraState.FOLLOWING], sets minimum rotation angle in degrees required
         * to start rotation gesture.
         *
         * Default to 5.0f.
         */
        fun followingRotationAngleThreshold(
            followingRotationAngleThreshold: Float,
        ): Builder = apply {
            this.followingRotationAngleThreshold = followingRotationAngleThreshold
        }

        /**
         * Builds [NavigationScaleGestureHandlerOptions].
         */
        fun build() = NavigationScaleGestureHandlerOptions(
            context,
            followingInitialMoveThreshold,
            followingMultiFingerMoveThreshold,
            followingMultiFingerProtectedMoveArea,
            followingRotationAngleThreshold,
        )
    }
}
