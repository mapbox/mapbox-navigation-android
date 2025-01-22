package com.mapbox.navigation.ui.maps.internal.location

import android.animation.TimeInterpolator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import com.mapbox.geojson.Point

/**
 * A time and value interpolator for puck's animator. It uses [ConstantVelocityInterpolator] to
 * animate at constant velocity between key points.
 *
 * Use [PuckAnimationEvaluator.installIn] to install it in [ValueAnimator].
 * Use [PuckAnimationEvaluator.reset] to reset TimeInterpolator values for re-use with
 * next ValueAnimator.
 */
internal class PuckAnimationEvaluator(
    private val keyPoints: Array<Point>,
) : TimeInterpolator, TypeEvaluator<Point> {

    private var interpolator: TimeInterpolator? = null

    override fun getInterpolation(input: Float): Float =
        interpolator?.getInterpolation(input) ?: input

    override fun evaluate(fraction: Float, startValue: Point, endValue: Point): Point {
        if (interpolator == null) {
            // we defer creation of TimeInterpolator until we know startValue
            interpolator = ConstantVelocityInterpolator(startValue, keyPoints)
        }
        return POINT.evaluate(fraction, startValue, endValue)
    }

    /**
     * Reset TimeInterpolator values for re-use with next ValueAnimator.
     */
    fun reset() {
        interpolator = null
    }

    /**
     * Set this evaluator as [animator] TimeInterpolator and TypeEvaluator.
     */
    fun installIn(animator: ValueAnimator) {
        reset()
        animator.interpolator = this
        animator.setEvaluator(this)
    }

    companion object {
        private val POINT = TypeEvaluator<Point> { fraction, startValue, endValue ->
            Point.fromLngLat(
                startValue.longitude() + fraction * (endValue.longitude() - startValue.longitude()),
                startValue.latitude() + fraction * (endValue.latitude() - startValue.latitude()),
            )
        }
    }
}
