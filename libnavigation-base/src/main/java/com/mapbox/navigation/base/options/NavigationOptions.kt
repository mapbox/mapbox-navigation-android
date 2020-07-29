package com.mapbox.navigation.base.options

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
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
 * @param timeFormatType defines time format for calculation remaining trip time
 * @param navigatorPredictionMillis defines approximate navigator prediction in milliseconds
 * @param distanceFormatter [DistanceFormatter] for format distances showing in notification during navigation
 * @param onboardRouterOptions [OnboardRouterOptions] defines configuration for the default on-board router
 * @param isFromNavigationUi Boolean *true* if is called from UI, otherwise *false*
 * @param isDebugLoggingEnabled Boolean
 * @param deviceProfile [DeviceProfile] defines how navigation data should be interpretation
 */
class NavigationOptions private constructor(
    val applicationContext: Context,
    val accessToken: String?,
    val locationEngine: LocationEngine,
    @TimeFormat.Type val timeFormatType: Int,
    val navigatorPredictionMillis: Long,
    val distanceFormatter: DistanceFormatter?,
    val onboardRouterOptions: OnboardRouterOptions,
    val isFromNavigationUi: Boolean,
    val isDebugLoggingEnabled: Boolean,
    val deviceProfile: DeviceProfile
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder(context = applicationContext).apply {
        accessToken(accessToken)
        locationEngine(locationEngine)
        timeFormatType(timeFormatType)
        navigatorPredictionMillis(navigatorPredictionMillis)
        distanceFormatter(distanceFormatter)
        onboardRouterOptions(onboardRouterOptions)
        isFromNavigationUi(isFromNavigationUi)
        isDebugLoggingEnabled(isDebugLoggingEnabled)
        deviceProfile(deviceProfile)
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
        if (timeFormatType != other.timeFormatType) return false
        if (navigatorPredictionMillis != other.navigatorPredictionMillis) return false
        if (distanceFormatter != other.distanceFormatter) return false
        if (onboardRouterOptions != other.onboardRouterOptions) return false
        if (isFromNavigationUi != other.isFromNavigationUi) return false
        if (isDebugLoggingEnabled != other.isDebugLoggingEnabled) return false
        if (deviceProfile != other.deviceProfile) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = applicationContext.hashCode()
        result = 31 * result + (accessToken?.hashCode() ?: 0)
        result = 31 * result + locationEngine.hashCode()
        result = 31 * result + timeFormatType
        result = 31 * result + navigatorPredictionMillis.hashCode()
        result = 31 * result + (distanceFormatter?.hashCode() ?: 0)
        result = 31 * result + onboardRouterOptions.hashCode()
        result = 31 * result + isFromNavigationUi.hashCode()
        result = 31 * result + isDebugLoggingEnabled.hashCode()
        result = 31 * result + deviceProfile.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationOptions(applicationContext=$applicationContext, accessToken=$accessToken, locationEngine=$locationEngine, timeFormatType=$timeFormatType, navigatorPredictionMillis=$navigatorPredictionMillis, distanceFormatter=$distanceFormatter, onboardRouterOptions=$onboardRouterOptions, isFromNavigationUi=$isFromNavigationUi, isDebugLoggingEnabled=$isDebugLoggingEnabled, deviceProfile=$deviceProfile)"
    }

    /**
     * Build a new [NavigationOptions]
     */
    class Builder(context: Context) {
        private val applicationContext = context.applicationContext
        private var accessToken: String? = null
        private var locationEngine: LocationEngine? = null // Default is created when built
        private var timeFormatType: Int = TimeFormat.NONE_SPECIFIED
        private var navigatorPredictionMillis: Long = DEFAULT_NAVIGATOR_PREDICTION_MILLIS
        private var distanceFormatter: DistanceFormatter? = null
        private var onboardRouterOptions: OnboardRouterOptions = OnboardRouterOptions.Builder().build()
        private var isFromNavigationUi: Boolean = false
        private var isDebugLoggingEnabled: Boolean = false
        private var deviceProfile: DeviceProfile = DeviceProfile.Builder().build()

        /**
         * Defines [Mapbox Access Token](https://docs.mapbox.com/help/glossary/access-token/)
         */
        fun accessToken(accessToken: String?) =
            apply { this.accessToken = accessToken }

        /**
         * Override the mechanism responsible for providing location approximations to navigation
         */
        fun locationEngine(locationEngine: LocationEngine) =
            apply { this.locationEngine = locationEngine }

        /**
         * Defines the type of device creating localization data
         */
        fun deviceProfile(deviceProfile: DeviceProfile) =
            apply { this.deviceProfile = deviceProfile }

        /**
         * Defines time format for calculation remaining trip time
         */
        fun timeFormatType(type: Int) =
            apply { this.timeFormatType = type }

        /**
         * Defines approximate navigator prediction in milliseconds
         */
        fun navigatorPredictionMillis(predictionMillis: Long) =
            apply { navigatorPredictionMillis = predictionMillis }

        /**
         *  Defines format distances showing in notification during navigation
         */
        fun distanceFormatter(distanceFormatter: DistanceFormatter?) =
            apply { this.distanceFormatter = distanceFormatter }

        /**
         * Defines configuration for the default on-board router
         */
        fun onboardRouterOptions(onboardRouterOptions: OnboardRouterOptions) =
            apply { this.onboardRouterOptions = onboardRouterOptions }

        /**
         * Defines if the builder instance is created from the Navigation UI
         */
        fun isFromNavigationUi(flag: Boolean) =
            apply { this.isFromNavigationUi = flag }

        /**
         * Defines if debug logging is enabled
         */
        fun isDebugLoggingEnabled(flag: Boolean) =
            apply { this.isDebugLoggingEnabled = flag }

        /**
         * Build a new instance of [NavigationOptions]
         * @return NavigationOptions
         */
        fun build(): NavigationOptions {
            return NavigationOptions(
                applicationContext = applicationContext,
                accessToken = accessToken,
                locationEngine = locationEngine ?: LocationEngineProvider.getBestLocationEngine(applicationContext),
                timeFormatType = timeFormatType,
                navigatorPredictionMillis = navigatorPredictionMillis,
                distanceFormatter = distanceFormatter,
                onboardRouterOptions = onboardRouterOptions,
                isFromNavigationUi = isFromNavigationUi,
                isDebugLoggingEnabled = isDebugLoggingEnabled,
                deviceProfile = deviceProfile
            )
        }
    }
}
