package com.mapbox.navigation.base.internal.route

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouteRefreshMetadata

@ExperimentalMapboxNavigationAPI
fun createRouteRefreshMetadata(
    isUpToDate: Boolean,
) = RouteRefreshMetadata(
    isUpToDate = isUpToDate,
)
