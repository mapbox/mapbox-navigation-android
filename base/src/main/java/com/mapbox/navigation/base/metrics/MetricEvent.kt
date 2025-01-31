package com.mapbox.navigation.base.metrics

import androidx.annotation.StringDef
import com.google.gson.Gson

/**
 * An interface with types of metrics events that the SDK would send via Telemetry
 */
interface MetricEvent {

    /**
     * [MetricEvent] names scope
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        DirectionsMetrics.ROUTE_RETRIEVAL,
        NavigationMetrics.ARRIVE,
        NavigationMetrics.CANCEL_SESSION,
        NavigationMetrics.DEPART,
        NavigationMetrics.REROUTE,
        NavigationMetrics.FEEDBACK,
        NavigationMetrics.INITIAL_GPS,
        NavigationMetrics.FASTER_ROUTE,
        NavigationMetrics.FREE_DRIVE,
        NavigationMetrics.CUSTOM_EVENT,
    )
    annotation class Metric

    /**
     * Name of [MetricEvent]
     */
    @Metric
    val metricName: String

    /**
     * Present [MetricEvent] as Json string
     *
     * @param gson Gson
     * @return String
     */
    fun toJson(gson: Gson): String
}

/**
 * Navigation [MetricEvent] names holder
 */
object NavigationMetrics {

    /**
     * Navigation Event "Arrive" name
     */
    const val ARRIVE = "navigation.arrive"

    /**
     * Navigation Event "Cancel Session" name
     */
    const val CANCEL_SESSION = "navigation.cancel"

    /**
     * Navigation Event "Depart" name
     */
    const val DEPART = "navigation.depart"

    /**
     * Navigation Event "Reroute" name
     */
    const val REROUTE = "navigation.reroute"

    /**
     * Navigation Event "Feedback" name
     */
    const val FEEDBACK = "navigation.feedback"

    /**
     * Navigation Event "Initial GPS" name
     */
    const val INITIAL_GPS = "initial_gps_event"

    /**
     * Navigation Event "On Faster Route" name
     */
    const val FASTER_ROUTE = "navigation.fasterRoute"

    /**
     * Navigation Event "FreeDrive" name
     */
    const val FREE_DRIVE = "navigation.freeDrive"

    /**
     * Navigation Event "Custom" name
     */
    const val CUSTOM_EVENT = "navigation.customEvent"
}

/**
 * Directions [MetricEvent] names holder
 */
object DirectionsMetrics {

    /**
     * Directions Event "Route retrieval" name
     */
    const val ROUTE_RETRIEVAL = "route_retrieval_event"
}
