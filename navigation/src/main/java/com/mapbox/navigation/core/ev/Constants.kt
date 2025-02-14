package com.mapbox.navigation.core.ev

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Use this key to pass efficiency of the vehicle to [MapboxNavigation.onEVDataUpdated] so that
 * it could be used by EV features which require that value.
 * Efficiency is an [Double] value calculated based on the recent drives and represents how many
 * kilometers could be traveled using 1000 watt-hours (Km/Kwh).
 */
@ExperimentalPreviewMapboxNavigationAPI
const val EV_EFFICIENCY_KEY = "nav-sdk-ev-efficiency"
