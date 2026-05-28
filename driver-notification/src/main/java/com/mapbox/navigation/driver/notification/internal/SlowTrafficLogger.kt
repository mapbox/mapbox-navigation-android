package com.mapbox.navigation.driver.notification.internal

import android.os.SystemClock
import androidx.annotation.RestrictTo
import com.mapbox.navigation.utils.internal.logD
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object SlowTrafficLogger {

    private var interval: Duration? = null
    private var nextLogAt = 0L
    private var maxSegments: Int? = null
    private var maxEvents: Int? = null
    private var segmentsLogged = 0
    private var eventsLogged = 0
    private var lastRouteUuid: String? = null
    private var noEventsLoggedThisInterval = false

    fun setInterval(interval: Duration) {
        this.interval = interval
    }

    fun setMaxSegments(max: Int) {
        maxSegments = max
    }

    fun setMaxEvents(max: Int) {
        maxEvents = max
    }

    internal fun shouldLogNow(): Boolean {
        val interval = interval ?: return false
        val now = SystemClock.elapsedRealtime()
        if (now < nextLogAt) return false
        nextLogAt = now + interval.inWholeMilliseconds
        segmentsLogged = 0
        eventsLogged = 0
        noEventsLoggedThisInterval = false
        return true
    }

    internal fun logRouteChanged(routeUuid: String?) {
        if (routeUuid == lastRouteUuid) return
        lastRouteUuid = routeUuid
        logD(TAG) { "Route changed: uuid=${routeUuid ?: "n/a"}" }
    }

    internal fun logSegment(
        segmentIndex: Int,
        segment: SlowTrafficSegment,
        isNew: Boolean,
        eventIndex: Int,
    ) {
        val max = maxSegments
        if (max != null && segmentsLogged >= max) return
        segmentsLogged++
        logD(TAG) {
            val duration = segment.duration.inWholeSeconds.seconds
            val freeFlowDuration = segment.freeFlowDuration.inWholeSeconds.seconds
            "  $segmentIndex: [${if (isNew) "NEW  " else "MERGE"} into Event#$eventIndex] " +
                "impact=${duration - freeFlowDuration}, " +
                "duration=$duration, " +
                "freeFlowDuration=$freeFlowDuration, " +
                "distanceFromRouteStart=${segment.distanceFromRouteStartMeters.toInt()}m, " +
                "length=${segment.lengthMeters.toInt()}m, " +
                "congestion=${segment.congestionRange}, " +
                "segment[legIndex=${segment.legIndex} geometryRange=${segment.geometryRange}]"
        }
    }

    internal fun logSummary(
        index: Int,
        summary: SlowTrafficSegmentsSummary,
        nullFreeFlowSegments: Int,
        coveredSegments: Int,
        minFreeFlowSpeed: Int?,
        maxFreeFlowSpeed: Int?,
    ) {
        val max = maxEvents
        if (max != null && eventsLogged >= max) return
        eventsLogged++
        logD(TAG) {
            val (durationSec, freeFlowSec, totalLength) =
                summary.traits.fold(Triple(0L, 0L, 0.0)) { (duration, freeFlow, len), trait ->
                    Triple(
                        duration + trait.duration.inWholeSeconds,
                        freeFlow + trait.freeFlowDuration.inWholeSeconds,
                        len + trait.lengthMeters,
                    )
                }
            val totalDuration = durationSec.seconds
            val totalFreeFlowDuration = freeFlowSec.seconds
            val totalSegments = coveredSegments + nullFreeFlowSegments
            val freeFlowRange = when {
                minFreeFlowSpeed == null || maxFreeFlowSpeed == null -> "null"
                minFreeFlowSpeed == maxFreeFlowSpeed -> "$minFreeFlowSpeed km/h"
                else -> "$minFreeFlowSpeed..$maxFreeFlowSpeed km/h"
            }
            "  => Event#$index finalized: " +
                "impact=${totalDuration - totalFreeFlowDuration}, " +
                "duration=$totalDuration, " +
                "freeFlowDuration=$totalFreeFlowDuration, " +
                "distanceFromRouteStart=${summary.distanceFromRouteStartMeters.toInt()}m, " +
                "length=${totalLength.toInt()}m, " +
                "nullFreeFlowSegments=$nullFreeFlowSegments/$totalSegments, " +
                "freeFlowSpeed=$freeFlowRange"
        }
    }

    internal fun logNoEvents(routeUuid: String?, targetCongestionsRanges: List<IntRange>) {
        if (noEventsLoggedThisInterval) return
        noEventsLoggedThisInterval = true
        logD(TAG) {
            "  => no events matched ranges=$targetCongestionsRanges " +
                "(route=${routeUuid ?: "n/a"})"
        }
    }

    private const val TAG = "SlowTrafficSegments"
}
