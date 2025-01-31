package com.mapbox.navigation.base.internal.trip.notification

import com.mapbox.navigation.base.trip.notification.TripNotificationInterceptor

/**
 * This is an internal class that gives [MapboxNavigation] the ability to pass the
 * [TripNotificationInterceptor] to the [MapboxTripNotification]
 */
class TripNotificationInterceptorOwner {
    var interceptor: TripNotificationInterceptor? = null
}
