package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.TravelEstimate

/**
 * Contains data needed to populate the [NavigationTemplate].
 *
 * @see [CarNavigationInfoProvider]
 */
class CarNavigationInfo internal constructor(
    /**
     * Contains the current [NavigationTemplate.NavigationInfo] that is used with the
     * [NavigationTemplate].
     */
    val navigationInfo: NavigationTemplate.NavigationInfo? = null,
    /**
     * Contains the current [TravelEstimate] that is used with the [NavigationTemplate].
     */
    val destinationTravelEstimate: TravelEstimate? = null,
)
