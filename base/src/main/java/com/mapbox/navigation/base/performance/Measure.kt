package com.mapbox.navigation.base.performance

import android.util.Log
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Logging tool to measure the time between events. Use this tool to help decide how to instrument
 * with the [Trace].
 *
 * @property name The name of the measure instance.
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed interface Measure {

    val name: String

    /**
     * Starts a measurement block and records the start time.
     *
     * @param block The block of code that starts the measure, the result of which is used as a label or event.
     * @return The measured time duration in seconds.
     */
    fun start(block: () -> String): Double = mark(MARK_START, block)

    /**
     * Marks a specific event in the measurement process.
     *
     * @param event The name of the event to be marked.
     * @param block The block of code associated with the event.
     * @return The measured time at the point of the event in seconds.
     */
    fun mark(event: String, block: () -> String): Double

    /**
     * Logs the result of a measurement.
     *
     * @param block The block of code to be executed and logged.
     * @return The time taken to execute the block of code in seconds.
     */
    fun log(block: () -> String): Double

    /**
     * Returns the duration elapsed since a specific event was marked.
     *
     * @param event The event name to measure the elapsed time from.
     * @return The duration elapsed since the event, or `null` if the event doesn't exist.
     */
    fun elapsedSince(event: String): Duration?

    companion object {

        /**
         * Measurements created tag
         */
        const val MARK_CREATED = "created"

        /**
         * Measurements started tag
         */
        const val MARK_START = "start"
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object NoMeasure : Measure {

    override val name: String = "NoMeasure"
    override fun mark(event: String, block: () -> String): Double = 0.0
    override fun log(block: () -> String): Double = 0.0
    override fun elapsedSince(event: String): Duration? = null
}

@OptIn(ExperimentalTime::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class MeasureImpl internal constructor(
    override val name: String,
) : Measure {

    private val timeMap = mutableMapOf<String, TimeSource.Monotonic.ValueTimeMark>()

    init {
        timeMap[Measure.MARK_CREATED] = TimeSource.Monotonic.markNow()
        Log.i(TAG, "${Measure.MARK_CREATED}: $name")
    }

    override fun mark(event: String, block: () -> String): Double {
        val message = block()
        val now = TimeSource.Monotonic.markNow()
        timeMap[event] = now
        val t0: TimeSource.Monotonic.ValueTimeMark = timeMap[Measure.MARK_START] ?: run {
            timeMap[Measure.MARK_START] = now
            now
        }
        val elapsedSeconds = t0.elapsedNow().toDouble(DurationUnit.SECONDS)
        Log.i(TAG, "mark: $name: $event message: $message, elapsed: $elapsedSeconds")
        return elapsedSeconds
    }

    override fun log(block: () -> String): Double {
        val message = block()
        val now = TimeSource.Monotonic.markNow()
        val t0: TimeSource.Monotonic.ValueTimeMark = timeMap[Measure.MARK_START] ?: run {
            timeMap[Measure.MARK_START] = now
            now
        }
        val elapsedSeconds = t0.elapsedNow().toDouble(DurationUnit.SECONDS)
        Log.i(TAG, "log: $name, message: $message, elapsed: $elapsedSeconds")
        return elapsedSeconds
    }

    override fun elapsedSince(event: String): Duration? {
        return timeMap[event]?.elapsedNow()
    }

    private companion object {

        private const val TAG = "Measure"
    }
}
