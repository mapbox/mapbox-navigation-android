package com.mapbox.navigation.examples.core.camera

import android.animation.TimeInterpolator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.Path
import android.location.Location
import android.view.animation.PathInterpolator
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.navigation.utils.internal.logD


interface LocationPublisher : LocationProvider {
    fun publish(
        location: Location,
        keyPoints: List<Location> = emptyList(),
        latLngTransitionOptions: (ValueAnimator.() -> Unit)? = null,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)? = null
    )
}

class PuckLocationProvider(
    private val downstreamProvider: LocationPublisher,
) : LocationProvider by downstreamProvider, LocationPublisher {

    override fun publish(
        location: Location,
        keyPoints: List<Location>,
        latLngTransitionOptions: (ValueAnimator.() -> Unit)?,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)?
    ) {
        logD(
            "changePosition keyPoints(${keyPoints.size}) = ${keyPoints.asStr()}; location = ${location.asStr()};",
            "PuckLocationProvider"
        )
        val latLngUpdates = if (keyPoints.isNotEmpty()) {
            keyPoints.map { Point.fromLngLat(it.longitude, it.latitude) }.toTypedArray()
        } else {
            arrayOf(Point.fromLngLat(location.longitude, location.latitude))
        }

        val evaluator = PuckAnimationEvaluator(latLngUpdates)

        val options: (ValueAnimator.() -> Unit) = {
            latLngTransitionOptions?.also { apply(it) }
            interpolator = evaluator
            setEvaluator(evaluator)
        }

        downstreamProvider.publish(
            location,
            keyPoints,
            options,
            bearingTransitionOptions
        )
    }
}

class PuckAnimationEvaluator(
    private val keyPoints: Array<Point>
) : TimeInterpolator, TypeEvaluator<Point> {

    private var interpolator: TimeInterpolator? = null

    override fun getInterpolation(input: Float): Float =
        interpolator?.getInterpolation(input) ?: input

    override fun evaluate(fraction: Float, startValue: Point, endValue: Point): Point {
        if (interpolator == null) {
            // we defer creation of TimeInterpolator until we know startValue
            interpolator = createTimeInterpolator(startValue)
        }
        return POINT.evaluate(fraction, startValue, endValue)
    }

    private fun createTimeInterpolator(startValue: Point): TimeInterpolator {
        val distances = mutableListOf<Double>()
        var total = 0.0
        keyPoints.fold(startValue) { prevPoint, point ->
            val d = prevPoint.distanceSqrTo(point)
            distances.add(d)
            total += d
            point
        }

        if (0 < total) {
            val path = Path()
            val pathDebug = mutableListOf<Pair<Float, Float>>(0.0f to 0.0f)
            val step = 1.0f / keyPoints.size
            var pathTime = 0.0
            keyPoints.forEachIndexed { index, point ->
                val deltaTime = distances[index] / total
                val velocity = distances[index] / deltaTime // this velocity should be constant
                logD(
                    "$index; V = $velocity, dD = ${distances[index]}; dT = ${deltaTime}; point = ${point.asStr()}",
                    "PuckAnimationEvaluator"
                )
                pathTime += deltaTime
                path.lineTo(pathTime.toFloat(), step * (index + 1))
                pathDebug.add(pathTime.toFloat() to step * (index + 1))
            }
            logD("pathInterpolator = $pathDebug", "PuckAnimationEvaluator")
            return PathInterpolator(path)
        }
        return TimeInterpolator { it }
    }

    companion object {
        private val POINT = TypeEvaluator<Point> { fraction, startValue, endValue ->
            Point.fromLngLat(
                startValue.longitude() + fraction * (endValue.longitude() - startValue.longitude()),
                startValue.latitude() + fraction * (endValue.latitude() - startValue.latitude())
            )
        }
    }
}
