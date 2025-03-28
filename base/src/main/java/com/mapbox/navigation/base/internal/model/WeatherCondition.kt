package com.mapbox.navigation.base.internal.model

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.model.WeatherCondition

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@WeatherCondition.Type
fun com.mapbox.navigator.Weather.toPlatformConditionType(): Int {
    return when (this) {
        com.mapbox.navigator.Weather.FOG -> WeatherCondition.FOG
        com.mapbox.navigator.Weather.RAIN -> WeatherCondition.RAIN
        com.mapbox.navigator.Weather.SNOW -> WeatherCondition.SNOW
        com.mapbox.navigator.Weather.WET_ROAD -> WeatherCondition.WET_ROAD
    }
}
