package com.mapbox.androidauto.car

import com.mapbox.androidauto.car.feedback.core.CarFeedbackOptions
import com.mapbox.androidauto.car.feedback.core.CarFeedbackPollProvider
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackOption
import com.mapbox.androidauto.car.navigation.speedlimit.SpeedLimitOptions
import com.mapbox.androidauto.car.preview.CarRouteOptionsInterceptor
import com.mapbox.androidauto.car.search.CarPlaceSearchOptions
import com.mapbox.androidauto.notification.MapboxCarNotification
import com.mapbox.androidauto.notification.MapboxCarNotificationOptions
import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Allows you to define values used by the Mapbox Android Auto Navigation SDK.
 */
class MapboxCarOptionsCustomization {

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
}
