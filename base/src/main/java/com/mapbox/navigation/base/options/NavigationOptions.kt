package com.mapbox.navigation.base.options

import android.content.Context
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
 * @param locationOptions [LocationOptions] that specify where to take locations from
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
 * @param trafficOverrideOptions defines options for traffic override
 * @param nativeRouteObject defines whether to use native route object
 */
class NavigationOptions
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private constructor(
    val applicationContext: Context,
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
    val locationOptions: LocationOptions,
    @ExperimentalPreviewMapboxNavigationAPI
    val trafficOverrideOptions: TrafficOverrideOptions,
    @ExperimentalPreviewMapboxNavigationAPI
    val roadObjectMatcherOptions: RoadObjectMatcherOptions,
    @ExperimentalPreviewMapboxNavigationAPI
    val nativeRouteObject: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun toBuilder(): Builder = Builder(applicationContext).apply {
        locationOptions(locationOptions)
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
        trafficOverrideOptions(trafficOverrideOptions)
        roadObjectMatcherOptions(roadObjectMatcherOptions)
        nativeRouteObject(nativeRouteObject)
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
        if (locationOptions != other.locationOptions) return false
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
        if (trafficOverrideOptions != other.trafficOverrideOptions) return false
        if (roadObjectMatcherOptions != other.roadObjectMatcherOptions) return false
        if (nativeRouteObject != other.nativeRouteObject) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun hashCode(): Int {
        var result = applicationContext.hashCode()
        result = 31 * result + locationOptions.hashCode()
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
        result = 31 * result + trafficOverrideOptions.hashCode()
        result = 31 * result + roadObjectMatcherOptions.hashCode()
        result = 31 * result + nativeRouteObject.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun toString(): String {
        return "NavigationOptions(" +
            "applicationContext=$applicationContext, " +
            "locationOptions=$locationOptions, " +
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
            "copilotOptions=$copilotOptions, " +
            "trafficOverrideOptions=$trafficOverrideOptions, " +
            "roadObjectMatcherOptions=$roadObjectMatcherOptions, " +
            "nativeRouteObject=$nativeRouteObject" +
            ")"
    }

    /**
     * Build a new [NavigationOptions]
     */
    class Builder(applicationContext: Context) {

        private val applicationContext = applicationContext.applicationContext
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
        private var locationOptions: LocationOptions = LocationOptions.Builder().build()

        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        private var copilotOptions: CopilotOptions = CopilotOptions.Builder().build()

        @ExperimentalPreviewMapboxNavigationAPI
        private var trafficOverrideOptions: TrafficOverrideOptions =
            TrafficOverrideOptions.Builder().build()

        @ExperimentalPreviewMapboxNavigationAPI
        private var roadObjectMatcherOptions: RoadObjectMatcherOptions =
            RoadObjectMatcherOptions.Builder().build()

        @ExperimentalPreviewMapboxNavigationAPI
        private var nativeRouteObject: Boolean = false

        /**
         * Sets location options. See [LocationOptions] for details.
         * By default real location with default location provider will be used.
         * @param locationOptions location options
         * @return the same builder
         */
        fun locationOptions(locationOptions: LocationOptions): Builder =
            apply { this.locationOptions = locationOptions }

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
         * Defines configuration for traffic override
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun trafficOverrideOptions(trafficOverrideOptions: TrafficOverrideOptions): Builder =
            apply { this.trafficOverrideOptions = trafficOverrideOptions }

        /**
         * Defines configuration for traffic override
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun roadObjectMatcherOptions(roadObjectMatcherOptions: RoadObjectMatcherOptions): Builder =
            apply { this.roadObjectMatcherOptions = roadObjectMatcherOptions }

        // TODO: provide better documentation
        // https://mapbox.atlassian.net/browse/NAVAND-6546
        /**
         * Defines whether to use native route object
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun nativeRouteObject(value: Boolean): Builder =
            apply { this.nativeRouteObject = value }

        /**
         * Build a new instance of [NavigationOptions]
         * @return NavigationOptions
         */
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        fun build(): NavigationOptions {
            return NavigationOptions(
                applicationContext = applicationContext,
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
                locationOptions = locationOptions,
                trafficOverrideOptions = trafficOverrideOptions,
                roadObjectMatcherOptions = roadObjectMatcherOptions,
                nativeRouteObject = nativeRouteObject,
            )
        }
    }
}
