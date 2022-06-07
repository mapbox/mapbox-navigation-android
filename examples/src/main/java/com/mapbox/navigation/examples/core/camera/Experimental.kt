package com.mapbox.navigation.examples.core.camera

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.Path
import android.location.Location
import android.location.LocationManager
import android.view.animation.PathInterpolator
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.utils.internal.logD
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.pow

//region Synced Puck

fun NavigationLocationProvider.syncedWith(
    viewportDataSource: MapboxNavigationViewportDataSource
): LocationProvider {
    val downstream = PuckLocationProvider(NavigationLocationProvider().asPublisher())
    val provider = DeferredLocationProvider(this, downstream)
    viewportDataSource.registerUpdateObserver {
        provider.publishLocation()
    }
    return provider
}

fun NavigationLocationProvider.syncedWith(
    viewportDataSource: MapboxNavigationViewportDataSource,
    animationRecorder: AnimRecorder
): LocationProvider {
    val downstream = PuckLocationProvider(NavigationLocationProvider().asPublisher())
    val recorder = RecordingLocationProvider(downstream, animationRecorder)
    val provider = DeferredLocationProvider(this, recorder)
    viewportDataSource.registerUpdateObserver {
        provider.publishLocation()
    }
    return provider
}

interface LocationPublisher : LocationProvider {
    fun publish(
        location: Location,
        keyPoints: List<Location> = emptyList(),
        latLngTransitionOptions: (ValueAnimator.() -> Unit)? = null,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)? = null
    )
}

fun NavigationLocationProvider.asPublisher(): LocationPublisher {
    return object : LocationPublisher, LocationProvider by this {
        override fun publish(
            location: Location,
            keyPoints: List<Location>,
            latLngTransitionOptions: (ValueAnimator.() -> Unit)?,
            bearingTransitionOptions: (ValueAnimator.() -> Unit)?
        ) {
            changePosition(location, keyPoints, latLngTransitionOptions, bearingTransitionOptions)
        }
    }
}

class DeferredLocationProvider(
    private val upstreamProvider: NavigationLocationProvider,
    private val downstreamProvider: LocationPublisher
) : LocationProvider by downstreamProvider {

    init {
        upstreamProvider.onFirstLocation {
            downstreamProvider.publish(it)
        }
    }

    fun publishLocation() {
        upstreamProvider.lastLocation?.also {
            downstreamProvider.publish(it, upstreamProvider.lastKeyPoints)
        }
    }

    private fun NavigationLocationProvider.onFirstLocation(action: (Location) -> Unit) {
        lastLocation?.also {
            action(it)
            return
        }
        registerLocationConsumer(object : AbstractLocationConsumer() {
            override fun onLocationUpdated(
                vararg location: Point,
                options: (ValueAnimator.() -> Unit)?
            ) {
                location.firstOrNull()?.also {
                    action(Location(LocationManager.PASSIVE_PROVIDER).apply {
                        latitude = it.latitude()
                        longitude = it.longitude()
                    })
                }
                unRegisterLocationConsumer(this)
            }
        })
    }

    private abstract class AbstractLocationConsumer : LocationConsumer {
        override fun onBearingUpdated(
            vararg bearing: Double,
            options: (ValueAnimator.() -> Unit)?
        ) = Unit

        override fun onLocationUpdated(
            vararg location: Point,
            options: (ValueAnimator.() -> Unit)?
        ) = Unit

        override fun onPuckBearingAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) =
            Unit

        override fun onPuckLocationAnimatorDefaultOptionsUpdated(options: ValueAnimator.() -> Unit) =
            Unit
    }
}

//endregion

class PuckLocationProvider(
    private val downstreamProvider: LocationPublisher,
) : LocationProvider by downstreamProvider, LocationPublisher {

    override fun publish(
        location: Location,
        keyPoints: List<Location>,
        latLngTransitionOptions: (ValueAnimator.() -> Unit)?,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)?
    ) {
        logD("", "PuckLocationProvider")
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
            var rs = 0.0
            val rr = keyPoints.mapIndexed { index, point ->
                val r = distances[index] / total
                rs += r
                path.lineTo(rs.toFloat(), step * (index + 1))
                pathDebug.add(rs.toFloat() to step * (index + 1))
                r to point.asStr()
            }
            logD("ratios = $rr; pathInterpolator = $pathDebug", "PuckAnimationEvaluator")
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

private fun Point.distanceSqrTo(p: Point): Double {
    return (p.latitude() - latitude()).pow(2) + (p.longitude() - longitude()).pow(2)
}

private fun Location.asStr() = "(${latitude},${longitude})"
private fun List<Location>.asStr() =
    map { it.asStr() }.joinToString(prefix = "[", postfix = "]")

private fun Point.asStr() = "(${latitude()},${longitude()})"

// --

class RecordingLocationProvider(
    private val downstreamProvider: LocationPublisher,
    private val animationRecorder: AnimRecorder
) : LocationProvider by downstreamProvider, LocationPublisher {

    override fun publish(
        location: Location,
        keyPoints: List<Location>,
        latLngTransitionOptions: (ValueAnimator.() -> Unit)?,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)?
    ) {
        val options: (ValueAnimator.() -> Unit) = {
            latLngTransitionOptions?.also { apply(it) }
            animationRecorder.attachTo(this)
        }

        downstreamProvider.publish(
            location,
            keyPoints,
            options,
            bearingTransitionOptions
        )
    }
}

class AnimRecorder : Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {

    var isRecording = false
        private set

    val recordings = mutableListOf<Recording>()

    private val sampleRate: Long = 100 // milliseconds
    private var lastSampleTime: Long = 0
    private var listeners = ConcurrentLinkedQueue<Listener>()
    private var currentRecording: Recording? = null
    private var attachedTo: ValueAnimator? = null

    fun attachTo(valueAnimator: ValueAnimator) {
        if (attachedTo != valueAnimator) {
            detach()
        }
        attachedTo = valueAnimator
        valueAnimator.addListener(this)
        valueAnimator.addUpdateListener(this)
    }

    fun start() {
        isRecording = true
    }

    fun stop() {
        isRecording = false
        detach()
        currentRecording = null
    }

    fun detach() {
        attachedTo?.also {
            it.removeUpdateListener(this)
            it.removeListener(this)
        }
        attachedTo = null
    }

    fun addListener(l: Listener) {
        listeners.add(l)
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    override fun onAnimationStart(animation: Animator) {
        if (!isRecording || animation !is ValueAnimator) return
        currentRecording = Recording.from(animation)
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        if (!isRecording) return
        System.currentTimeMillis().also { now ->
            if (sampleRate <= now - lastSampleTime) {
                currentRecording?.pushFrame(animation)
                lastSampleTime = now
            }
        }
    }

    override fun onAnimationEnd(animation: Animator) {
        if (!isRecording || animation !is ValueAnimator) return
        currentRecording?.ended = true
        currentRecording?.endValue = animation.animatedValue
        saveRecording()
    }

    override fun onAnimationCancel(animation: Animator) {
        if (!isRecording) return
        currentRecording?.cancelled = true
        saveRecording()
    }

    override fun onAnimationRepeat(animation: Animator) = Unit

    private fun saveRecording() {
        currentRecording?.also {
            recordings.add(it)
            notifyRecordingSaved(it)
        }
        currentRecording = null
    }

    private fun notifyRecordingSaved(r: Recording) {
        listeners.forEach { it.onRecordingSaved(r) }
    }

    class Recording(
        val duration: Long,
        var startValue: Any
    ) {
        var endValue: Any? = null
        val frames = mutableListOf<Frame>()
        var ended = false
        var cancelled = false

        fun pushFrame(animator: ValueAnimator) {
            frames.add(Frame.from(animator))
        }

        companion object {
            fun from(animator: ValueAnimator): Recording {
                return Recording(animator.duration, animator.animatedValue)
            }
        }
    }

    data class Frame(
        val currentPlayTime: Long,
        val animatedFraction: Float,
        val animatedValue: Any,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        companion object {
            fun from(animator: ValueAnimator) =
                Frame(
                    animator.currentPlayTime,
                    animator.animatedFraction,
                    animator.animatedValue,
                )
        }
    }

    interface Listener {
        fun onRecordingSaved(recording: Recording)
    }
}
