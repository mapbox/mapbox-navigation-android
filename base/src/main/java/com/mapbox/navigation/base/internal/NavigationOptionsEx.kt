package com.mapbox.navigation.base.internal

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.mapToNativeRerouteStrategy
import com.mapbox.navigation.base.options.NavigationOptions

@OptIn(ExperimentalMapboxNavigationAPI::class)
fun NavigationOptions.nativeRerouteStrategyForMatchRoute() =
    this.rerouteOptions.rerouteStrategyForMapMatchedRoutes.mapToNativeRerouteStrategy()
