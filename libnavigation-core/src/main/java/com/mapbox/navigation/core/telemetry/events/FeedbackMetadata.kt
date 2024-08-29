package com.mapbox.navigation.core.telemetry.events

import com.google.gson.Gson
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.toTelemetryLocations
import com.mapbox.navigation.core.telemetry.LocationsCollector

/**
 * It's the wrapper of [FeedbackMetadata] that collect locations after instance of
 * [FeedbackMetadataWrapper] is created and stop collecting in 2 possible cases:
 * - locations buffer is full;
 * - [FeedbackMetadataWrapper.get] is called.
 *
 * Notes:
 * - if you need to serialize Feedback metadata, use [FeedbackMetadata] only (see [FeedbackMetadataWrapper.get]);
 * - to collect as much data as possible preferably call [FeedbackMetadataWrapper.get] just before posting or storing feedback.
 */
@ExperimentalPreviewMapboxNavigationAPI
class FeedbackMetadataWrapper internal constructor(
    private val sessionIdentifier: String,
    private val driverModeIdentifier: String,
    @FeedbackEvent.DriverMode private val driverMode: String,
    private val driverModeStartTime: String,
    private val rerouteCount: Int,
    private val lastLocation: Point?,
    private val locationEngineNameExternal: String,
    private val simulation: Boolean,
    private val percentTimeInPortrait: Int?,
    private val percentTimeInForeground: Int?,
    private val eventVersion: Int,
    private val phoneState: PhoneState,
    private val metricsDirectionsRoute: MetricsDirectionsRoute,
    private val metricsRouteProgress: MetricsRouteProgress,
    private val appMetadata: AppMetadata?,
    private val locationsCollector: LocationsCollector,
) {
    private var bufferFlushed = false

    private val locationsBefore = mutableListOf<Location>()
    private val locationsAfter = mutableListOf<Location>()

    private val locationsBufferListener =
        LocationsCollector.LocationsCollectorListener { preEventLocations, postEventLocations ->
            locationsBefore.addAll(preEventLocations)
            locationsAfter.addAll(postEventLocations)
            bufferFlushed = true
        }

    init {
        locationsCollector.collectLocations(locationsBufferListener)
    }

    /**
     * Provide [FeedbackMetadata] with locations that were collected after creating instance of
     * [FeedbackMetadataWrapper].
     */
    fun get(): FeedbackMetadata {
        if (!bufferFlushed) {
            locationsCollector.flushBufferFor(locationsBufferListener)
        }
        return FeedbackMetadata(
            sessionIdentifier,
            driverModeIdentifier,
            driverMode,
            driverModeStartTime,
            rerouteCount,
            locationsBefore.toTelemetryLocations(),
            locationsAfter.toTelemetryLocations(),
            lastLocation,
            locationEngineNameExternal,
            simulation,
            percentTimeInPortrait,
            percentTimeInForeground,
            eventVersion,
            phoneState,
            metricsDirectionsRoute,
            metricsRouteProgress,
            appMetadata,
        )
    }
}

/**
 * Feedback metadata is used as part of [MapboxNavigation.postUserFeedback] to send deferred feedback.
 * It contains data(like session ids, locations before and after call and so on) from a particular
 * point of time/location when [MapboxNavigation.provideFeedbackMetadataWrapper] is called.
 *
 * Note: [MapboxNavigation.provideFeedbackMetadataWrapper] returns wrapper of
 * [FeedbackMetadata] that collect **locations after** call under the hood.
 */
@ExperimentalPreviewMapboxNavigationAPI
class FeedbackMetadata internal constructor(
    internal val sessionIdentifier: String? = null,
    internal val driverModeIdentifier: String? = null,
    @FeedbackEvent.DriverMode internal val driverMode: String? = null,
    internal val driverModeStartTime: String? = null,
    internal val rerouteCount: Int = 0,
    internal val locationsBeforeEvent: Array<TelemetryLocation>? = null,
    internal val locationsAfterEvent: Array<TelemetryLocation>? = null,
    internal val lastLocation: Point? = null,
    internal val locationEngineNameExternal: String? = null,
    internal val simulation: Boolean = false,
    internal val percentTimeInPortrait: Int? = null,
    internal val percentTimeInForeground: Int? = null,
    internal val eventVersion: Int,
    internal val phoneState: PhoneState,
    internal val metricsDirectionsRoute: MetricsDirectionsRoute,
    internal val metricsRouteProgress: MetricsRouteProgress,
    internal val appMetadata: AppMetadata? = null,
) {

    companion object {
        /**
         * Create a new instance of [FeedbackMetadata] from json.
         *
         * @throws Throwable if json is not a valid FeedbackMetadata
         */
        @JvmStatic
        @ExperimentalPreviewMapboxNavigationAPI
        fun fromJson(json: String): FeedbackMetadata? =
            Gson().fromJson(json, FeedbackMetadata::class.java)
    }

    /**
     * Serialize [FeedbackMetadata] to json string.
     */
    fun toJson(gson: Gson): String =
        gson.toJson(this)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeedbackMetadata

        if (sessionIdentifier != other.sessionIdentifier) return false
        if (driverModeIdentifier != other.driverModeIdentifier) return false
        if (driverMode != other.driverMode) return false
        if (driverModeStartTime != other.driverModeStartTime) return false
        if (rerouteCount != other.rerouteCount) return false
        if (!locationsBeforeEvent.contentEquals(other.locationsBeforeEvent)) return false
        if (!locationsAfterEvent.contentEquals(other.locationsAfterEvent)) return false
        if (lastLocation != other.lastLocation) return false
        if (locationEngineNameExternal != other.locationEngineNameExternal) return false
        if (percentTimeInPortrait != other.percentTimeInPortrait) return false
        if (percentTimeInForeground != other.percentTimeInForeground) return false
        if (eventVersion != other.eventVersion) return false
        if (phoneState != other.phoneState) return false
        if (metricsDirectionsRoute != other.metricsDirectionsRoute) return false
        if (metricsRouteProgress != other.metricsRouteProgress) return false
        if (appMetadata != other.appMetadata) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = sessionIdentifier.hashCode()
        result = 31 * result + driverModeIdentifier.hashCode()
        result = 31 * result + driverMode.hashCode()
        result = 31 * result + driverModeStartTime.hashCode()
        result = 31 * result + rerouteCount.hashCode()
        result = 31 * result + locationsBeforeEvent.hashCode()
        result = 31 * result + locationsAfterEvent.hashCode()
        result = 31 * result + lastLocation.hashCode()
        result = 31 * result + locationEngineNameExternal.hashCode()
        result = 31 * result + percentTimeInPortrait.hashCode()
        result = 31 * result + percentTimeInForeground.hashCode()
        result = 31 * result + eventVersion.hashCode()
        result = 31 * result + phoneState.hashCode()
        result = 31 * result + metricsDirectionsRoute.hashCode()
        result = 31 * result + metricsRouteProgress.hashCode()
        result = 31 * result + appMetadata.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String =
        "FeedbackMetadata(" +
            "sessionIdentifier='$sessionIdentifier', " +
            "driverModeIdentifier=$driverModeIdentifier, " +
            "driverMode=$driverMode, " +
            "driverModeStartTime=$driverModeStartTime, " +
            "rerouteCount=$rerouteCount, " +
            "locationsBeforeEvent=$locationsBeforeEvent, " +
            "locationsAfterEvent=$locationsAfterEvent, " +
            "lastLocation=$lastLocation, " +
            "locationEngineNameExternal=$locationEngineNameExternal, " +
            "percentTimeInPortrait=$percentTimeInPortrait, " +
            "percentTimeInForeground=$percentTimeInForeground, " +
            "eventVersion=$eventVersion, " +
            "phoneState=$phoneState, " +
            "metricsDirectionsRoute=$metricsDirectionsRoute, " +
            "metricsRouteProgress=$metricsRouteProgress, " +
            "appMetadata=$appMetadata" +
            ")"
}
