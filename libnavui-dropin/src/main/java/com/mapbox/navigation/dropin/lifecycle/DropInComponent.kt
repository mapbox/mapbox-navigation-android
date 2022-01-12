package com.mapbox.navigation.dropin.lifecycle

import androidx.annotation.CallSuper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
open class DropInComponent : MapboxNavigationObserver {

    lateinit var coroutineScope: CoroutineScope

    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        coroutineScope = MainScope()
    }

    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        coroutineScope.cancel()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun MapboxNavigation.flowTripSessionState(): Flow<TripSessionState> = callbackFlow {
    val tripSessionStateObserver = TripSessionStateObserver { trySend(it) }
    registerTripSessionStateObserver(tripSessionStateObserver)
    awaitClose { unregisterTripSessionStateObserver(tripSessionStateObserver) }
}
