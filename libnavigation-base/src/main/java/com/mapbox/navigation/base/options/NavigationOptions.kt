package com.mapbox.navigation.base.options

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
 * Defines navigation options
 *
 * @param roundingIncrement defines the increment displayed on the instruction view
 * @param timeFormatType defines time format for calculation remaining trip time
 * @param navigatorPredictionMillis defines approximate navigator prediction in milliseconds
 *
 * This value will be used to offset the time at which the current location was calculated
 * in such a way as to project the location forward along the current trajectory so as to
 * appear more in sync with the users ground-truth location
 *
 * @param accessToken [Mapbox Access Token](https://docs.mapbox.com/help/glossary/access-token/)
 * @param distanceFormatter [DistanceFormatter] for format distances showing in notification during navigation
 * @param onboardRouterConfig [MapboxOnboardRouterConfig] defines configuration for the default on-board router
 * @param isFromNavigationUi Boolean *true* if is called from UI, otherwise *false*
 * @param isDebugLoggingEnabled Boolean
 * @param deviceProfile [DeviceProfile] defines how navigation data should be interpretation
 */
data class NavigationOptions constructor(
    val accessToken: String?,
    @TimeFormat.Type val timeFormatType: Int,
    val navigatorPredictionMillis: Long,
    val distanceFormatter: DistanceFormatter?,
    val onboardRouterConfig: MapboxOnboardRouterConfig?,
    val isFromNavigationUi: Boolean = false,
    val isDebugLoggingEnabled: Boolean = false,
    val deviceProfile: DeviceProfile
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder()
        .accessToken(accessToken)
        .timeFormatType(timeFormatType)
        .navigatorPredictionMillis(navigatorPredictionMillis)
        .distanceFormatter(distanceFormatter)
        .onboardRouterConfig(onboardRouterConfig)
        .isFromNavigationUi(isFromNavigationUi)

    /**
     * Build a new [NavigationOptions]
     */
    class Builder {
        private var _accessToken: String? = null
        private var timeFormatType: Int = TimeFormat.NONE_SPECIFIED
        private var navigatorPredictionMillis: Long = DEFAULT_NAVIGATOR_PREDICTION_MILLIS
        private var distanceFormatter: DistanceFormatter? = null
        private var onboardRouterConfig: MapboxOnboardRouterConfig? = null
        private var isFromNavigationUi: Boolean = false
        private var isDebugLoggingEnabled: Boolean = false
        private var deviceProfile: DeviceProfile = HandheldProfile()

        /**
         * Defines [Mapbox Access Token](https://docs.mapbox.com/help/glossary/access-token/)
         */
        fun accessToken(accessToken: String?) =
            apply { this._accessToken = accessToken }

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
        fun onboardRouterConfig(onboardRouterConfig: MapboxOnboardRouterConfig?) =
            apply { this.onboardRouterConfig = onboardRouterConfig }

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
                accessToken = _accessToken,
                timeFormatType = timeFormatType,
                navigatorPredictionMillis = navigatorPredictionMillis,
                distanceFormatter = distanceFormatter,
                onboardRouterConfig = onboardRouterConfig,
                isFromNavigationUi = isFromNavigationUi,
                isDebugLoggingEnabled = isDebugLoggingEnabled,
                deviceProfile = deviceProfile
            )
        }
    }
}
