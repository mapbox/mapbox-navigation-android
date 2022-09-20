package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.base.route.NavigationRoute

fun calculateRoutesSimilarity(a: NavigationRoute, b: NavigationRoute): Double {
    return if (a.id == b.id) 1.0 else 0.0
}