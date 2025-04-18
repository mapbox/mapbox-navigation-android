package com.mapbox.navigation.driver.notification

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.driver.notification.traffic.SlowTrafficNotification

/**
 * Abstract base class for driver notifications.
 *
 * This class serves as a foundation for creating specific types of notifications
 * that can be used to inform drivers about various events or updates during navigation.
 *
 * Subclasses of `DriverNotification` can define specific notification types, such as
 * better route suggestions, traffic updates, or other relevant information.
 *
 * @see [SlowTrafficNotification] for an example of a specific notification type.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class DriverNotification
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
constructor()
