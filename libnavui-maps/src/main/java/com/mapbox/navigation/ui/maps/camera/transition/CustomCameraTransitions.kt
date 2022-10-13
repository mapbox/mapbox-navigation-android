package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.animation.Interpolator
import androidx.core.animation.doOnEnd
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

class CustomCameraTransitions(
//    private val positioning: Positioning,
//    private val runtimeConfig: RuntimeConfig,
    private val mapboxMap: MapboxMap,
    private val cameraPlugin: CameraAnimationsPlugin,
    private val linearInterpolator: Interpolator
) : NavigationCameraTransition {

    private val defaultTransitions = MapboxNavigationCameraTransition(mapboxMap, cameraPlugin)

    /**
     * We record running linear transition animations here for the following purpose: In the case that a transition
     * is started while another one is still in flight (which happens regularly, especially with 10Hz position updates)
     * and both animate to the same target, we don't want to reset the animation duration. Instead we calculate the
     * remaining duration of the running transition - using the AnimationStates below - and apply that to the new one,
     * ensuring that the animation speed remains constant.
     */
    private var centerAnimationState: AnimatorState<Point>? = null
    private var zoomAnimationState: AnimatorState<Double>? = null
    private var bearingAnimationState: AnimatorState<Double>? = null
    private var paddingAnimationState: AnimatorState<EdgeInsets>? = null
    private var pitchAnimationState: AnimatorState<Double>? = null

    override fun transitionFromLowZoomToHighZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions
    ) = defaultTransitions.transitionFromLowZoomToHighZoom(cameraOptions, transitionOptions)

    override fun transitionFromHighZoomToLowZoom(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions
    ) = defaultTransitions.transitionFromHighZoomToLowZoom(cameraOptions, transitionOptions)

    @SuppressWarnings("LongMethod", "SpreadOperator")
    override fun transitionLinear(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions
    ): AnimatorSet {
        val animators = mutableListOf<ValueAnimator>()

        cameraOptions.center?.let { center ->
            cameraPlugin.createCenterAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(center) {
                    owner(NavigationCamera.NAVIGATION_CAMERA_OWNER)
                }
            ) {
                configureAnimator(newAnimator = this, center, centerAnimationState, shortTransition = true)
                doOnEnd { if (centerAnimationState?.animator == this) centerAnimationState = null }
                centerAnimationState = AnimatorState(center, this)
                animators.add(this)
            }
        }

        cameraOptions.zoom?.let { zoom ->
            cameraPlugin.createZoomAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(zoom) { owner(NavigationCamera.NAVIGATION_CAMERA_OWNER) }
            ) {
                configureAnimator(newAnimator = this, zoom, zoomAnimationState, shortTransition = false)
                doOnEnd { if (zoomAnimationState?.animator == this) zoomAnimationState = null }
                zoomAnimationState = AnimatorState(zoom, this)
                animators.add(this)
            }
        }

        cameraOptions.bearing?.let { bearing ->
            val bearingShortestRotation = normalizeBearing(mapboxMap.cameraState.bearing, bearing)
            cameraPlugin.createBearingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(bearingShortestRotation) {
                    owner(NavigationCamera.NAVIGATION_CAMERA_OWNER)
                },
                block = {
                    configureAnimator(newAnimator = this, bearing, bearingAnimationState, shortTransition = false)
                    doOnEnd { if (bearingAnimationState?.animator == this) bearingAnimationState = null }
                    bearingAnimationState = AnimatorState(bearing, this)
                    animators.add(this)
                }
            )
        }

        cameraOptions.padding?.let { padding ->
            cameraPlugin.createPaddingAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(padding) { owner(NavigationCamera.NAVIGATION_CAMERA_OWNER) }
            ) {
                configureAnimator(newAnimator = this, padding, paddingAnimationState, shortTransition = false)
                doOnEnd { if (paddingAnimationState?.animator == this) paddingAnimationState = null }
                paddingAnimationState = AnimatorState(padding, this)
                animators.add(this)
            }
        }

        cameraOptions.pitch?.let { pitch ->
            cameraPlugin.createPitchAnimator(
                CameraAnimatorOptions.cameraAnimatorOptions(pitch) { owner(NavigationCamera.NAVIGATION_CAMERA_OWNER) }
            ) {
                configureAnimator(newAnimator = this, pitch, pitchAnimationState, shortTransition = false)
                doOnEnd { if (pitchAnimationState?.animator == this) pitchAnimationState = null }
                pitchAnimationState = AnimatorState(pitch, this)
                animators.add(this)
            }
        }

        return AnimatorSet().apply {
            playTogether(*(animators.toTypedArray()))
        }
    }

    private fun <T> configureAnimator(
        newAnimator: ValueAnimator,
        newTarget: T,
        currentAnimationState: AnimatorState<T>?,
        shortTransition: Boolean
    ) {
        // if we are already animating to [newTarget], use the remaining animation time as duration
        if (currentAnimationState != null && newTarget == currentAnimationState.target) {
            newAnimator.duration = (
                currentAnimationState.animator.duration * (1f - currentAnimationState.animator.animatedFraction)
                ).toLong()
            println("[ddlog] prev-based duration: ${newAnimator.duration}")
        } else if (shortTransition) {
            newAnimator.duration = Random.nextLong(1001)
            println("[ddlog] short duration: ${newAnimator.duration}")
        } else {
            newAnimator.duration = Random.nextLong(1001)
            println("[ddlog] long duration: ${newAnimator.duration}")
        }
        newAnimator.interpolator = linearInterpolator
    }

    /**
     * Returns a bearing change using the shortest path.
     */
    @SuppressWarnings("MagicNumber")
    private fun normalizeBearing(currentBearing: Double, targetBearing: Double): Double {
        /*
        rounding is a workaround for https://github.com/mapbox/mapbox-maps-android/issues/274
        it prevents wrapping to 360 degrees for very small, negative numbers and prevents the camera
        from spinning around unintentionally
        */
        return (currentBearing + shortestRotation(currentBearing, targetBearing)).roundTo(6)
    }

    @SuppressWarnings("MagicNumber")
    private fun shortestRotation(from: Double, to: Double): Double {
        return (to - from + 540) % 360 - 180
    }

    private fun Double.roundTo(numFractionDigits: Int): Double {
        val factor = 10.0.pow(numFractionDigits.toDouble())
        return (this * factor).roundToInt() / factor
    }

    private data class AnimatorState<T>(val target: T, val animator: ValueAnimator)
}
