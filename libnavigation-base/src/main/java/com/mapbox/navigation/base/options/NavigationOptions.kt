package com.mapbox.navigation.base.options

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
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
 * @param isFromNavigationUi Boolean *true* if is called from UI, otherwise *false*
 * @param isDebugLoggingEnabled Boolean
 * @param deviceProfile [DeviceProfile] defines how navigation data should be interpreted
 * @param eHorizonOptions [EHorizonOptions] defines configuration for the Electronic Horizon
 * @param routeRefreshOptions defines configuration for refreshing routes
 * @param routeAlternativesOptions defines configuration for observing alternatives while navigating
 * @param incidentsOptions defines configuration for live incidents
 * @param historyRecorderOptions defines configuration for recording navigation events
 * @param eventsAppMetadata [EventsAppMetadata] information (optional)
 */
class NavigationOptions private constructor(
    val applicationContext: Context,
    val accessToken: String?,
    val locationEngine: LocationEngine,
    val locationEngineRequest: LocationEngineRequest,
    @TimeFormat.Type val timeFormatType: Int,
    val navigatorPredictionMillis: Long,
    val distanceFormatterOptions: DistanceFormatterOptions,
    val routingTilesOptions: RoutingTilesOptions,
    val isFromNavigationUi: Boolean,
    val isDebugLoggingEnabled: Boolean,
    val deviceProfile: DeviceProfile,
    val eHorizonOptions: EHorizonOptions,
    val routeRefreshOptions: RouteRefreshOptions,
    val routeAlternativesOptions: RouteAlternativesOptions,
    val incidentsOptions: IncidentsOptions,
    val historyRecorderOptions: HistoryRecorderOptions,
    val eventsAppMetadata: EventsAppMetadata?,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(applicationContext).apply {
        accessToken(accessToken)
        locationEngine(locationEngine)
        locationEngineRequest(locationEngineRequest)
        timeFormatType(timeFormatType)
        navigatorPredictionMillis(navigatorPredictionMillis)
        distanceFormatterOptions(distanceFormatterOptions)
        routingTilesOptions(routingTilesOptions)
        isFromNavigationUi(isFromNavigationUi)
        isDebugLoggingEnabled(isDebugLoggingEnabled)
        deviceProfile(deviceProfile)
        eHorizonOptions(eHorizonOptions)
        routeRefreshOptions(routeRefreshOptions)
        routeAlternativesOptions(routeAlternativesOptions)
        incidentsOptions(incidentsOptions)
        historyRecorderOptions(historyRecorderOptions)
        eventsAppMetadata(eventsAppMetadata)
    }

    /**
     * Regenerate whenever a change is made
     */
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
        if (isFromNavigationUi != other.isFromNavigationUi) return false
        if (isDebugLoggingEnabled != other.isDebugLoggingEnabled) return false
        if (deviceProfile != other.deviceProfile) return false
        if (eHorizonOptions != other.eHorizonOptions) return false
        if (routeRefreshOptions != other.routeRefreshOptions) return false
        if (routeAlternativesOptions != other.routeAlternativesOptions) return false
        if (incidentsOptions != other.incidentsOptions) return false
        if (historyRecorderOptions != other.historyRecorderOptions) return false
        if (eventsAppMetadata != other.eventsAppMetadata) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = applicationContext.hashCode()
        result = 31 * result + (accessToken?.hashCode() ?: 0)
        result = 31 * result + locationEngine.hashCode()
        result = 31 * result + locationEngineRequest.hashCode()
        result = 31 * result + timeFormatType
        result = 31 * result + navigatorPredictionMillis.hashCode()
        result = 31 * result + distanceFormatterOptions.hashCode()
        result = 31 * result + routingTilesOptions.hashCode()
        result = 31 * result + isFromNavigationUi.hashCode()
        result = 31 * result + isDebugLoggingEnabled.hashCode()
        result = 31 * result + deviceProfile.hashCode()
        result = 31 * result + eHorizonOptions.hashCode()
        result = 31 * result + routeRefreshOptions.hashCode()
        result = 31 * result + routeAlternativesOptions.hashCode()
        result = 31 * result + incidentsOptions.hashCode()
        result = 31 * result + historyRecorderOptions.hashCode()
        result = 31 * result + (eventsAppMetadata?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
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
            "isFromNavigationUi=$isFromNavigationUi, " +
            "isDebugLoggingEnabled=$isDebugLoggingEnabled, " +
            "deviceProfile=$deviceProfile, " +
            "eHorizonOptions=$eHorizonOptions, " +
            "routeRefreshOptions=$routeRefreshOptions, " +
            "routeAlternativesOptions=$routeAlternativesOptions, " +
            "incidentsOptions=$incidentsOptions, " +
            "historyRecorderOptions=$historyRecorderOptions, " +
            "eventsAppMetadata=$eventsAppMetadata" +
            ")"
    }

    /**
     * Build a new [NavigationOptions]
     */
    class Builder(applicationContext: Context) {

        private val applicationContext = applicationContext.applicationContext
        private var accessToken: String? = null
        private var locationEngine: LocationEngine? = null // Default is created when built
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
        private var isFromNavigationUi: Boolean = false
        private var isDebugLoggingEnabled: Boolean = false
        private var deviceProfile: DeviceProfile = DeviceProfile.Builder().build()
        private var eHorizonOptions: EHorizonOptions = EHorizonOptions.Builder().build()
        private var routeRefreshOptions: RouteRefreshOptions = RouteRefreshOptions.Builder().build()
        private var routeAlternativesOptions: RouteAlternativesOptions =
            RouteAlternativesOptions.Builder().build()
        private var incidentsOptions: IncidentsOptions = IncidentsOptions.Builder().build()
        private var historyRecorderOptions: HistoryRecorderOptions =
            HistoryRecorderOptions.Builder().build()
        private var eventsAppMetadata: EventsAppMetadata? = null

        /**
         * Defines [Mapbox Access Token](https://docs.mapbox.com/help/glossary/access-token/)
         */
        fun accessToken(accessToken: String?): Builder =
            apply { this.accessToken = accessToken }

        /**
         * Override the mechanism responsible for providing location approximations to navigation
         */
        fun locationEngine(locationEngine: LocationEngine): Builder =
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
         * Defines if the builder instance is created from the Navigation UI
         */
        fun isFromNavigationUi(flag: Boolean): Builder =
            apply { this.isFromNavigationUi = flag }

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
         * Build a new instance of [NavigationOptions]
         * @return NavigationOptions
         */
        fun build(): NavigationOptions {
            return NavigationOptions(
                applicationContext = applicationContext,
                accessToken = accessToken,
                locationEngine = locationEngine
                    ?: LocationEngineProvider.getBestLocationEngine(applicationContext),
                locationEngineRequest = locationEngineRequest,
                timeFormatType = timeFormatType,
                navigatorPredictionMillis = navigatorPredictionMillis,
                distanceFormatterOptions = distanceFormatterOptions,
                routingTilesOptions = routingTilesOptions,
                isFromNavigationUi = isFromNavigationUi,
                isDebugLoggingEnabled = isDebugLoggingEnabled,
                deviceProfile = deviceProfile,
                eHorizonOptions = eHorizonOptions,
                routeRefreshOptions = routeRefreshOptions,
                routeAlternativesOptions = routeAlternativesOptions,
                incidentsOptions = incidentsOptions,
                historyRecorderOptions = historyRecorderOptions,
                eventsAppMetadata = eventsAppMetadata,
            )
        }
    }
}
