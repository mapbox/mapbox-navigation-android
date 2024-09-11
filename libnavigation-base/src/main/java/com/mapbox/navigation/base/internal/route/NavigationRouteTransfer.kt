package com.mapbox.navigation.base.internal.route

import androidx.annotation.WorkerThread
import com.mapbox.navigation.base.route.NavigationRoute

@WorkerThread
fun NavigationRoute.serialize() = this.serialize()

@WorkerThread
fun deserializeNavigationRouteFrom(value: String) = NavigationRoute.deserializeFrom(value)
