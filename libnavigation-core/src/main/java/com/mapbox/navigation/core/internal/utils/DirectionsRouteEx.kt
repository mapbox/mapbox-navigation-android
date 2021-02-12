@file:JvmName("DirectionsRouteEx")

package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute

fun DirectionsRoute?.isSameRoute(compare: DirectionsRoute?): Boolean =
    this?.routeOptions()?.requestUuid() == compare?.routeOptions()?.requestUuid()
