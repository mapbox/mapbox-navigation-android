package com.mapbox.navigation.ui.androidauto

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.action.MapboxScreenActionStripProvider
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackOptions
import com.mapbox.navigation.ui.androidauto.feedback.core.CarFeedbackPollProvider
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackOption
import com.mapbox.navigation.ui.androidauto.navigation.speedlimit.SpeedLimitOptions
import com.mapbox.navigation.ui.androidauto.notification.MapboxCarNotification
import com.mapbox.navigation.ui.androidauto.notification.MapboxCarNotificationOptions
import com.mapbox.navigation.ui.androidauto.preview.CarRouteOptionsInterceptor
import com.mapbox.navigation.ui.androidauto.search.CarPlaceSearchOptions
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
     * @see MapboxScreenActionStripProvider
     */
    @ExperimentalPreviewMapboxNavigationAPI
    var actionStripProvider = MapboxScreenActionStripProvider()
        private set

    /**
     * Apply the desired customization.
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun applyCustomization(customization: Customization) {
        customization.notificationOptions?.also { this.notificationOptions = it }
        customization.routeOptionsInterceptor?.also { this.routeOptionsInterceptor = it }
        customization.speedLimitOptions?.also { this.speedLimitOptionsMutable.tryEmit(it) }
        customization.placeSearchOptions?.also { this.carPlaceSearchOptions = it }
        customization.carFeedbackOptions?.also { this.carFeedbackOptions = it }
        customization.feedbackPollProvider?.also { this.feedbackPollProvider = it }
        customization.actionsStripProvider?.also { this.actionStripProvider = it }
    }

    /**
     * Allows you to define values used by the Mapbox Android Auto Navigation SDK.
     */
    class Customization {

        /**
         * Modify behavior of the [MapboxCarNotification].
         */
        var notificationOptions: MapboxCarNotificationOptions? = null

        /**
         * Modify [RouteOptions] used for requesting a route.
         */
        var routeOptionsInterceptor: CarRouteOptionsInterceptor? = null

        /**
         * Modify car place search.
         */
        var placeSearchOptions: CarPlaceSearchOptions? = null

        /**
         * Modify behavior of the speed limit widget.
         */
        var speedLimitOptions: SpeedLimitOptions? = null

        /**
         * Modify how the car feedback is sent.
         */
        var carFeedbackOptions: CarFeedbackOptions? = null

        /**
         * Modify the selectable [CarFeedbackOption].
         */
        var feedbackPollProvider: CarFeedbackPollProvider? = null

        /**
         * Modify the action buttons for any screen.
         */
        @ExperimentalPreviewMapboxNavigationAPI
        var actionsStripProvider: MapboxScreenActionStripProvider? = null
    }
}
