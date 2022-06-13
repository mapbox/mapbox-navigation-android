package com.mapbox.navigation.ui.maps.internal.location

import android.animation.TimeInterpolator
import android.graphics.Path
import android.view.animation.PathInterpolator
import com.mapbox.geojson.Point
import kotlin.math.hypot

/**
 * A time interpolator that animates between key points at constant velocity.
 */
class ConstantVelocityInterpolator(
    startPoint: Point,
    keyPoints: Array<Point>
) : TimeInterpolator {

    private val innerInterpolator: TimeInterpolator

    init {
        val distances = mutableListOf<Double>()
        var total = 0.0
        keyPoints.fold(startPoint) { prevPoint, point ->
            val d = distanceTo(prevPoint, point)
            distances.add(d)
            total += d
            point
        }

        innerInterpolator = if (0 < total) {
            val path = Path()
            val step = 1.0f / keyPoints.size
            var pathTime = 0.0
            keyPoints.forEachIndexed { index, _ ->
                // simplified from (distances[index] / velocity) where (velocity = total / duration) and duration = 1.0
                val deltaTime = distances[index] / total
                pathTime += deltaTime
                path.lineTo(pathTime.toFloat(), step * (index + 1))
            }
            PathInterpolator(path)
        } else {
            TimeInterpolator { it }
        }
    }

    override fun getInterpolation(input: Float): Float =
        innerInterpolator.getInterpolation(input)

    private fun distanceTo(p1: Point, p2: Point): Double {
        return hypot(p2.latitude() - p1.latitude(), p2.longitude() - p1.longitude())
    }
}
