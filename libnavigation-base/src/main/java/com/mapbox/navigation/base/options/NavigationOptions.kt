package com.mapbox.navigation.base.options

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatter

/**
 * Default navigator approximate prediction in milliseconds
 *
 * This value will be used to offset the time at which the current location was calculated
 * in such a way as to project the location forward along the current trajectory so as to
 * appear more in sync with the users ground-truth location
 */
const val DEFAULT_NAVIGATOR_PREDICTION_MILLIS = 1100L

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
 * @param distanceFormatter [DistanceFormatter] for format distances showing in notification during navigation
 * @param onboardRouterOptions [OnboardRouterOptions] defines configuration for the default on-board router
 * @param isFromNavigationUi Boolean *true* if is called from UI, otherwise *false*
 * @param isDebugLoggingEnabled Boolean
 * @param deviceProfile [DeviceProfile] defines how navigation data should be interpretation
 * @param eHorizonOptions [EHorizonOptions] defines configuration for the Electronic Horizon
 */
class NavigationOptions private constructor(
    val applicationContext: Context,
    val accessToken: String?,
    val locationEngine: LocationEngine,
    val locationEngineRequest: LocationEngineRequest,
    @TimeFormat.Type val timeFormatType: Int,
    val navigatorPredictionMillis: Long,
    val distanceFormatter: DistanceFormatter?,
    val onboardRouterOptions: OnboardRouterOptions,
    val isFromNavigationUi: Boolean,
    val isDebugLoggingEnabled: Boolean,
    val deviceProfile: DeviceProfile,
    val eHorizonOptions: EHorizonOptions
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
        distanceFormatter(distanceFormatter)
        onboardRouterOptions(onboardRouterOptions)
        isFromNavigationUi(isFromNavigationUi)
        isDebugLoggingEnabled(isDebugLoggingEnabled)
        deviceProfile(deviceProfile)
        eHorizonOptions(eHorizonOptions)
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
        if (distanceFormatter != other.distanceFormatter) return false
        if (onboardRouterOptions != other.onboardRouterOptions) return false
        if (isFromNavigationUi != other.isFromNavigationUi) return false
        if (isDebugLoggingEnabled != other.isDebugLoggingEnabled) return false
        if (deviceProfile != other.deviceProfile) return false
        if (eHorizonOptions != other.eHorizonOptions) return false

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
        result = 31 * result + (distanceFormatter?.hashCode() ?: 0)
        result = 31 * result + onboardRouterOptions.hashCode()
        result = 31 * result + isFromNavigationUi.hashCode()
        result = 31 * result + isDebugLoggingEnabled.hashCode()
        result = 31 * result + deviceProfile.hashCode()
        result = 31 * result + eHorizonOptions.hashCode()
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
            "distanceFormatter=$distanceFormatter, " +
            "onboardRouterOptions=$onboardRouterOptions, " +
            "isFromNavigationUi=$isFromNavigationUi, " +
            "isDebugLoggingEnabled=$isDebugLoggingEnabled, " +
            "deviceProfile=$deviceProfile, " +
            "eHorizonOptions=$eHorizonOptions" +
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
        private var distanceFormatter: DistanceFormatter? = null
        private var onboardRouterOptions: OnboardRouterOptions =
            OnboardRouterOptions.Builder().build()
        private var isFromNavigationUi: Boolean = false
        private var isDebugLoggingEnabled: Boolean = false
        private var deviceProfile: DeviceProfile = DeviceProfile.Builder().build()
        private var eHorizonOptions: EHorizonOptions = EHorizonOptions.Builder().build()

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
        fun distanceFormatter(distanceFormatter: DistanceFormatter?): Builder =
            apply { this.distanceFormatter = distanceFormatter }

        /**
         * Defines configuration for the default on-board router
         */
        fun onboardRouterOptions(onboardRouterOptions: OnboardRouterOptions): Builder =
            apply { this.onboardRouterOptions = onboardRouterOptions }

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
                distanceFormatter = distanceFormatter,
                onboardRouterOptions = onboardRouterOptions,
                isFromNavigationUi = isFromNavigationUi,
                isDebugLoggingEnabled = isDebugLoggingEnabled,
                deviceProfile = deviceProfile,
                eHorizonOptions = eHorizonOptions
            )
        }
    }
}
