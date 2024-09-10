package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.navigation.core.internal.congestions.model.MetersPerSecond
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal fun interface AheadDistanceCalculator {
    operator fun invoke(speed: MetersPerSecond): Int
}

internal class PredictedTimeAheadDistanceCalculator(
    private val aheadTime: Duration = DEFAULT_TIME,
) : AheadDistanceCalculator {
    override fun invoke(speed: MetersPerSecond): Int =
        minOf((speed.value * aheadTime.inWholeSeconds).roundToInt(), MAX_LENGTH_IN_METERS)

    companion object {
        private val DEFAULT_TIME = 2.minutes
        private const val MAX_LENGTH_IN_METERS = 6000
    }
}
