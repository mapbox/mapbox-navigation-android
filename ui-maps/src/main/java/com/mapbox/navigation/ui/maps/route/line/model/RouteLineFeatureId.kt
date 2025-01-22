package com.mapbox.navigation.ui.maps.route.line.model

@JvmInline
internal value class RouteLineFeatureId(private val featureId: String?) {
    fun id(): String? = featureId
}
