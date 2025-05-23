package com.mapbox.navigation.ui.maps.internal.location

import android.animation.TimeInterpolator
import android.animation.TypeEvaluator
import com.mapbox.geojson.Point

/**
 * A time and value interpolator for puck's animator. It uses [ConstantVelocityInterpolator] to
 * animate at constant velocity between key points.
 */
internal class PuckAnimationEvaluatorInterpolator(
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

    companion object {
        private val POINT = TypeEvaluator<Point> { fraction, startValue, endValue ->
            Point.fromLngLat(
                startValue.longitude() + fraction * (endValue.longitude() - startValue.longitude()),
                startValue.latitude() + fraction * (endValue.latitude() - startValue.latitude()),
            )
        }
    }
}
