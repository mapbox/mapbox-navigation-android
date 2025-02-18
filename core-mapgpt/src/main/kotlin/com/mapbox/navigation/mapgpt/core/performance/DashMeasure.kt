package com.mapbox.navigation.mapgpt.core.performance

import android.util.Log
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Logging tool to measure the time between events. Use this tool to help decide how to instrument
 * with the [DashTrace].
 */
sealed interface DashMeasure {
    val name: String
    fun start(block: () -> String): Double = mark(MARK_START, block)
    fun mark(event: String, block: () -> String): Double
    fun log(block: () -> String): Double
    fun elapsedSince(event: String): Duration?

    companion object {
        const val MARK_CREATED = "created"
        const val MARK_START = "start"
    }
}

internal object NoDashMeasure : DashMeasure {
    override val name: String = "NoDashMeasure"
    override fun mark(event: String, block: () -> String): Double = 0.0
    override fun log(block: () -> String): Double = 0.0
    override fun elapsedSince(event: String): Duration? = null
}


@OptIn(ExperimentalTime::class)
internal class DashMeasureImpl internal constructor(
    override val name: String,
) : DashMeasure {
    private val timeMap = mutableMapOf<String, TimeSource.Monotonic.ValueTimeMark>()

    init {
        timeMap[DashMeasure.MARK_CREATED] = TimeSource.Monotonic.markNow()
        Log.i(TAG,"${DashMeasure.MARK_CREATED}: $name")
    }

    override fun mark(event: String, block: () -> String): Double {
        val message = block()
        val now = TimeSource.Monotonic.markNow()
        timeMap[event] = now
        val t0: TimeSource.Monotonic.ValueTimeMark = timeMap[DashMeasure.MARK_START] ?: run {
            timeMap[DashMeasure.MARK_START] = now
            now
        }
        val elapsedSeconds = (now - t0).toDouble(DurationUnit.SECONDS)
        Log.i(TAG, "mark: $name: $event message: $message, elapsed: $elapsedSeconds")
        return elapsedSeconds
    }

    override fun log(block: () -> String): Double {
        val message = block()
        val now = TimeSource.Monotonic.markNow()
        val t0: TimeSource.Monotonic.ValueTimeMark = timeMap[DashMeasure.MARK_START] ?: run {
            timeMap[DashMeasure.MARK_START] = now
            now
        }
        val elapsedSeconds = (now - t0).toDouble(DurationUnit.SECONDS)
        Log.i(TAG, "log: $name, message: $message, elapsed: $elapsedSeconds")
        return elapsedSeconds
    }

    override fun elapsedSince(event: String): Duration? {
        return timeMap[event]?.elapsedNow()
    }

    private companion object {
        private const val TAG = "DashMeasure"
    }
}
