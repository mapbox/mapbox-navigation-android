package com.mapbox.navigation.base.internal.extensions

import com.mapbox.navigation.base.internal.trip.model.RouteIndices
import com.mapbox.navigation.base.trip.model.RouteProgress

fun RouteProgress.internalAlternativeRouteIndices(): Map<String, RouteIndices> =
    alternativeRoutesIndices
