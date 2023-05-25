package com.mapbox.navigation.base.options

import android.content.Context
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.common.location.LiveTrackingClient
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions

/**
 * Default navigator approximate prediction in milliseconds
 *
 * This value will be used to offset the time at which the current location was calculated
 * in such a way as to project the location forward along the current trajectory so as to
 * appear more in sync with the users ground-truth location
 */
const val DEFAULT_NAVIGATOR_PREDICTION_MILLIS = 1000L

/**
 * This value will be used to offset the time at which the current location was calculated
 * in such a way as to project the location forward along the current trajectory so as to
 * appear more in sync with the users ground-truth location
 *
 * @param applicationContext the Context of the Android Application
 * @param accessToken [Mapbox Access Token](https://docs.mapbox.com/help/glossary/access-token/)
 * @param locationEngine the mechanism responsible for providing location approximations to navigation
 * @param locationEngineRequest specifies the rate to request locations from the location engine.
 * @param timeFormatType defines time format for calculation remaining trip time
 * @param navigatorPredictionMillis defines approximate navigator prediction in milliseconds
 * @param distanceFormatterOptions [DistanceFormatterOptions] options to format distances showing in notification during navigation
 * @param routingTilesOptions [RoutingTilesOptions] defines routing tiles endpoint and storage configuration.
 * @param isDebugLoggingEnabled Boolean
 * @param deviceProfile [DeviceProfile] defines how navigation data should be interpreted
 * @param eHorizonOptions [EHorizonOptions] defines configuration for the Electronic Horizon
 * @param routeRefreshOptions defines configuration for refreshing routes
 * @param rerouteOptions defines configuration for reroute
 * @param routeAlternativesOptions defines configuration for observing alternatives while navigating
 * @param incidentsOptions defines configuration for live incidents
 * @param historyRecorderOptions defines configuration for recording navigation events
 * @param eventsAppMetadata [EventsAppMetadata] information (optional)
 * @param enableSensors enables sensors for current position calculation (optional)
 * @param copilotOptions defines options for Copilot
 */
class NavigationOptions
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private constructor(
    val applicationContext: Context,
    val accessToken: String?,
    val locationEngine: LiveTrackingClient?,
    val locationEngineRequest: LocationEngineRequest,
    @TimeFormat.Type val timeFormatType: Int,
    val navigatorPredictionMillis: Long,
    val distanceFormatterOptions: DistanceFormatterOptions,
    val routingTilesOptions: RoutingTilesOptions,
    val isDebugLoggingEnabled: Boolean,
    val deviceProfile: DeviceProfile,
    val eHorizonOptions: EHorizonOptions,
    val routeRefreshOptions: RouteRefreshOptions,
    val rerouteOptions: RerouteOptions,
    val routeAlternativesOptions: RouteAlternativesOptions,
    val incidentsOptions: IncidentsOptions,
    val historyRecorderOptions: HistoryRecorderOptions,
    val eventsAppMetadata: EventsAppMetadata?,
    val enableSensors: Boolean,
    @ExperimentalPreviewMapboxNavigationAPI
    val copilotOptions: CopilotOptions,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun toBuilder(): Builder = Builder(applicationContext).apply {
        accessToken(accessToken)
        locationEngine(locationEngine)
        locationEngineRequest(locationEngineRequest)
        timeFormatType(timeFormatType)
        navigatorPredictionMillis(navigatorPredictionMillis)
        distanceFormatterOptions(distanceFormatterOptions)
        routingTilesOptions(routingTilesOptions)
        isDebugLoggingEnabled(isDebugLoggingEnabled)
        deviceProfile(deviceProfile)
        eHorizonOptions(eHorizonOptions)
        routeRefreshOptions(routeRefreshOptions)
        rerouteOptions(rerouteOptions)
        routeAlternativesOptions(routeAlternativesOptions)
        incidentsOptions(incidentsOptions)
        historyRecorderOptions(historyRecorderOptions)
        eventsAppMetadata(eventsAppMetadata)
        enableSensors(enableSensors)
        copilotOptions(copilotOptions)
    }

    /**
     * Regenerate whenever a change is made
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationOptions

        if (applicationContext != other.applicationContext) return false
        if (accessToken != other.accessToken) return false
        if (locationEngine != other.locationEngine) return false
        if (locationEngineRequest != other.locationEngineRequest) return false
        if (timeFormatType != other.timeFormatType) return false
        if (navigatorPredictionMillis != other.navigatorPredictionMillis) return false
        if (distanceFormatterOptions != other.distanceFormatterOptions) return false
        if (routingTilesOptions != other.routingTilesOptions) return false
        if (isDebugLoggingEnabled != other.isDebugLoggingEnabled) return false
        if (deviceProfile != other.deviceProfile) return false
        if (eHorizonOptions != other.eHorizonOptions) return false
        if (routeRefreshOptions != other.routeRefreshOptions) return false
        if (rerouteOptions != other.rerouteOptions) return false
        if (routeAlternativesOptions != other.routeAlternativesOptions) return false
        if (incidentsOptions != other.incidentsOptions) return false
        if (historyRecorderOptions != other.historyRecorderOptions) return false
        if (eventsAppMetadata != other.eventsAppMetadata) return false
        if (enableSensors != other.enableSensors) return false
        if (copilotOptions != other.copilotOptions) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun hashCode(): Int {
        var result = applicationContext.hashCode()
        result = 31 * result + accessToken.hashCode()
        result = 31 * result + locationEngine.hashCode()
        result = 31 * result + locationEngineRequest.hashCode()
        result = 31 * result + timeFormatType
        result = 31 * result + navigatorPredictionMillis.hashCode()
        result = 31 * result + distanceFormatterOptions.hashCode()
        result = 31 * result + routingTilesOptions.hashCode()
        result = 31 * result + isDebugLoggingEnabled.hashCode()
        result = 31 * result + deviceProfile.hashCode()
        result = 31 * result + eHorizonOptions.hashCode()
        result = 31 * result + routeRefreshOptions.hashCode()
        result = 31 * result + rerouteOptions.hashCode()
        result = 31 * result + routeAlternativesOptions.hashCode()
        result = 31 * result + incidentsOptions.hashCode()
        result = 31 * result + historyRecorderOptions.hashCode()
        result = 31 * result + eventsAppMetadata.hashCode()
        result = 31 * result + enableSensors.hashCode()
        result = 31 * result + copilotOptions.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun toString(): String {
        return "NavigationOptions(" +
            "applicationContext=$applicationContext, " +
            "accessToken=$accessToken, " +
            "locationEngine=$locationEngine, " +
            "locationEngineRequest=$locationEngineRequest, " +
            "timeFormatType=$timeFormatType, " +
            "navigatorPredictionMillis=$navigatorPredictionMillis, " +
            "distanceFormatterOptions=$distanceFormatterOptions, " +
            "routingTilesOptions=$routingTilesOptions, " +
            "isDebugLoggingEnabled=$isDebugLoggingEnabled, " +
            "deviceProfile=$deviceProfile, " +
            "eHorizonOptions=$eHorizonOptions, " +
            "routeRefreshOptions=$routeRefreshOptions, " +
            "rerouteOptions=$rerouteOptions, " +
            "routeAlternativesOptions=$routeAlternativesOptions, " +
            "incidentsOptions=$incidentsOptions, " +
            "historyRecorderOptions=$historyRecorderOptions, " +
            "eventsAppMetadata=$eventsAppMetadata, " +
            "enableSensors=$enableSensors, " +
            "copilotOptions=$copilotOptions" +
            ")"
    }

    /**
     * Build a new [NavigationOptions]
     */
    class Builder(applicationContext: Context) {

        private val applicationContext = applicationContext.applicationContext
        private var accessToken: String? = null
        private var locationEngine: LiveTrackingClient? = null
        private var locationEngineRequest = LocationEngineRequest
            .Builder(1000L)
            .setFastestInterval(500L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        private var timeFormatType: Int = TimeFormat.NONE_SPECIFIED
        private var navigatorPredictionMillis: Long = DEFAULT_NAVIGATOR_PREDICTION_MILLIS
        private var distanceFormatterOptions: DistanceFormatterOptions =
            DistanceFormatterOptions.Builder(applicationContext).build()
        private var routingTilesOptions: RoutingTilesOptions =
            RoutingTilesOptions.Builder().build()
        private var isDebugLoggingEnabled: Boolean = false
        private var deviceProfile: DeviceProfile = DeviceProfile.Builder().build()
        private var eHorizonOptions: EHorizonOptions = EHorizonOptions.Builder().build()
        private var routeRefreshOptions: RouteRefreshOptions = RouteRefreshOptions.Builder().build()
        private var rerouteOptions: RerouteOptions = RerouteOptions.Builder().build()
        private var routeAlternativesOptions: RouteAlternativesOptions =
            RouteAlternativesOptions.Builder().build()
        private var incidentsOptions: IncidentsOptions = IncidentsOptions.Builder().build()
        private var historyRecorderOptions: HistoryRecorderOptions =
            HistoryRecorderOptions.Builder().build()
        private var eventsAppMetadata: EventsAppMetadata? = null
        private var enableSensors: Boolean = false

        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        private var copilotOptions: CopilotOptions = CopilotOptions.Builder().build()

        /**
         * Defines [Mapbox Access Token](https://docs.mapbox.com/help/glossary/access-token/)
         */
        fun accessToken(accessToken: String?): Builder =
            apply { this.accessToken = accessToken }

        /**
         * Override the mechanism responsible for providing location approximations to navigation
         */
        fun locationEngine(locationEngine: LiveTrackingClient?): Builder =
            apply { this.locationEngine = locationEngine }

        /**
         * Override the rate to request locations from the location engine.
         */
        fun locationEngineRequest(locationEngineRequest: LocationEngineRequest): Builder =
            apply { this.locationEngineRequest = locationEngineRequest }

        /**
         * Defines the type of device creating localization data
         */
        fun deviceProfile(deviceProfile: DeviceProfile): Builder =
            apply { this.deviceProfile = deviceProfile }

        /**
         * Defines time format for calculation remaining trip time
         */
        fun timeFormatType(type: Int): Builder =
            apply { this.timeFormatType = type }

        /**
         * Defines approximate navigator prediction in milliseconds
         */
        fun navigatorPredictionMillis(predictionMillis: Long): Builder =
            apply { navigatorPredictionMillis = predictionMillis }

        /**
         *  Defines format distances showing in notification during navigation
         */
        fun distanceFormatterOptions(distanceFormatterOptions: DistanceFormatterOptions): Builder =
            apply { this.distanceFormatterOptions = distanceFormatterOptions }

        /**
         * Defines configuration for the default on-board router
         */
        fun routingTilesOptions(routingTilesOptions: RoutingTilesOptions): Builder =
            apply { this.routingTilesOptions = routingTilesOptions }

        /**
         * Defines if debug logging is enabled
         */
        fun isDebugLoggingEnabled(flag: Boolean): Builder =
            apply { this.isDebugLoggingEnabled = flag }

        /**
         * Defines configuration for the Electronic Horizon
         */
        fun eHorizonOptions(eHorizonOptions: EHorizonOptions): Builder =
            apply { this.eHorizonOptions = eHorizonOptions }

        /**
         * Defines configuration for route refresh
         */
        fun routeRefreshOptions(routeRefreshOptions: RouteRefreshOptions): Builder =
            apply { this.routeRefreshOptions = routeRefreshOptions }

        /**
         * Defines configuration for reroute
         */
        fun rerouteOptions(rerouteOptions: RerouteOptions): Builder =
            apply { this.rerouteOptions = rerouteOptions }

        /**
         * Defines configuration for route refresh
         */
        fun routeAlternativesOptions(routeAlternativesOptions: RouteAlternativesOptions): Builder =
            apply { this.routeAlternativesOptions = routeAlternativesOptions }

        /**
         * Defines configuration for live incidents
         */
        fun incidentsOptions(incidentsOptions: IncidentsOptions): Builder =
            apply { this.incidentsOptions = incidentsOptions }

        /**
         * Defines configuration history recording
         */
        fun historyRecorderOptions(historyRecorderOptions: HistoryRecorderOptions): Builder =
            apply { this.historyRecorderOptions = historyRecorderOptions }

        /**
         * Defines [EventsAppMetadata] information
         */
        fun eventsAppMetadata(eventsAppMetadata: EventsAppMetadata?): Builder =
            apply { this.eventsAppMetadata = eventsAppMetadata }

        /**
         * Enables analyzing data from sensors for better location prediction in case of a weak
         *  GPS signal, for example in tunnel. Usage of sensors can increase battery consumption.
         * Warning: don't enable sensors if you emulate location updates. The SDK ignores
         *  location updates which don't match data from sensors.
         */
        fun enableSensors(value: Boolean): Builder =
            apply { this.enableSensors = value }

        /**
         * Defines configuration for Copilot
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun copilotOptions(copilotOptions: CopilotOptions): Builder =
            apply { this.copilotOptions = copilotOptions }

        /**
         * Build a new instance of [NavigationOptions]
         * @return NavigationOptions
         */
        fun build(): NavigationOptions {
            return NavigationOptions(
                applicationContext = applicationContext,
                accessToken = accessToken,
                locationEngine = locationEngine,
                locationEngineRequest = locationEngineRequest,
                timeFormatType = timeFormatType,
                navigatorPredictionMillis = navigatorPredictionMillis,
                distanceFormatterOptions = distanceFormatterOptions,
                routingTilesOptions = routingTilesOptions,
                isDebugLoggingEnabled = isDebugLoggingEnabled,
                deviceProfile = deviceProfile,
                eHorizonOptions = eHorizonOptions,
                routeRefreshOptions = routeRefreshOptions,
                rerouteOptions = rerouteOptions,
                routeAlternativesOptions = routeAlternativesOptions,
                incidentsOptions = incidentsOptions,
                historyRecorderOptions = historyRecorderOptions,
                eventsAppMetadata = eventsAppMetadata,
                enableSensors = enableSensors,
                copilotOptions = copilotOptions,
            )
        }
    }
}
