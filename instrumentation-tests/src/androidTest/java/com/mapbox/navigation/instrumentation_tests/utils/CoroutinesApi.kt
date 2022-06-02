package com.mapbox.navigation.instrumentation_tests.utils

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun MapboxNavigation.routesUpdates(): Flow<RoutesUpdatedResult> {
    val navigation = this
    return callbackFlow {
        val observer = RoutesObserver {
            trySend(it)
        }
        navigation.registerRoutesObserver(observer)
        awaitClose {
            navigation.unregisterRoutesObserver(observer)
        }
    }
}

fun MapboxNavigation.routeProgressUpdates(): Flow<RouteProgress> {
    val navigation = this
    return callbackFlow {
        val observer = RouteProgressObserver {
            trySend(it)
        }
        navigation.registerRouteProgressObserver(observer)
        awaitClose {
            navigation.unregisterRouteProgressObserver(observer)
        }
    }
}