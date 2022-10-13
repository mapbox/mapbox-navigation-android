package com.mapbox.androidauto.car

import com.mapbox.androidauto.car.feedback.core.CarFeedbackOptions
import com.mapbox.androidauto.car.feedback.core.CarFeedbackPollProvider
import com.mapbox.androidauto.car.navigation.speedlimit.SpeedLimitOptions
import com.mapbox.androidauto.car.preview.CarRouteOptionsInterceptor
import com.mapbox.androidauto.car.search.CarPlaceSearchOptions
import com.mapbox.androidauto.notification.MapboxCarNotificationOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The options available for customizing Mapbox Android Auto Navigation.
 */
class MapboxCarOptions {
    private val speedLimitOptionsMutable = MutableStateFlow(SpeedLimitOptions.Builder().build())

    /**
     * @see MapboxCarNotificationOptions
     */
    var notificationOptions = MapboxCarNotificationOptions.Builder().build()
        private set

    /**
     * @see CarRouteOptionsInterceptor
     */
    var routeOptionsInterceptor: CarRouteOptionsInterceptor = CarRouteOptionsInterceptor { it }
        private set

    /**
     * @see SpeedLimitOptions
     */
    val speedLimitOptions: StateFlow<SpeedLimitOptions> = speedLimitOptionsMutable.asStateFlow()

    /**
     * @see CarPlaceSearchOptions
     */
    var carPlaceSearchOptions: CarPlaceSearchOptions = CarPlaceSearchOptions.Builder().build()
        private set

    /**
     * @see CarFeedbackOptions
     */
    var carFeedbackOptions: CarFeedbackOptions = CarFeedbackOptions.Builder().build()
        private set

    /**
     * @see CarFeedbackPollProvider
     */
    var feedbackPollProvider = CarFeedbackPollProvider()
        private set

    /**
     * Apply the desired customization.
     */
    fun applyCustomization(customization: MapboxCarOptionsCustomization) {
        customization.notificationOptions?.also { this.notificationOptions = it }
        customization.routeOptionsInterceptor?.also { this.routeOptionsInterceptor = it }
        customization.speedLimitOptions?.also { this.speedLimitOptionsMutable.tryEmit(it) }
        customization.placeSearchOptions?.also { this.carPlaceSearchOptions = it }
        customization.carFeedbackOptions?.also { this.carFeedbackOptions = it }
        customization.feedbackPollProvider?.also { this.feedbackPollProvider = it }
    }
}
