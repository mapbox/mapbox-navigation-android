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
    keyPoints: Array<Point>,
) : TimeInterpolator {

    private val innerInterpolator: TimeInterpolator

    init {
        val distances = mutableListOf<Double>()
        var total = 0.0
        keyPoints.fold(startPoint) { prevPoint, point ->
            val d = distance(prevPoint, point)
            if (0.0 < d) {
                distances.add(d)
                total += d
            }
            point
        }

        innerInterpolator = if (0 < total) {
            val path = timingPath(distances, total)
            PathInterpolator(path)
        } else {
            TimeInterpolator { it }
        }
    }

    override fun getInterpolation(input: Float): Float =
        innerInterpolator.getInterpolation(input)

    private fun distance(p1: Point, p2: Point): Double {
        return hypot(p2.latitude() - p1.latitude(), p2.longitude() - p1.longitude())
    }

    private fun timingPath(distances: List<Double>, total: Double): Path {
        val path = Path()
        val step = 1.0 / distances.size
        var pathTime = 0.0
        // NOTE: The Path must start at (0,0) and end at (1,1)
        // To avoid PathInterpolator IllegalArgException, we ignore last keypoint distance value
        // and manually add line to (1,1).
        for (i in 0..distances.size - 2) {
            val deltaTime = distances[i] / total
            pathTime += deltaTime
            if (pathTime > 1.0) {
                pathTime = 1.0
            }
            path.lineTo(pathTime.toFloat(), (step * (i + 1)).toFloat())
        }
        path.lineTo(1f, 1f)
        return path
    }
}
