package com.mapbox.navigation.dropin.tripprogress

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.tripprogress.internal.ui.TripProgressComponentContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted

internal class TripProgressComponentContractImpl(
    scope: CoroutineScope,
    store: Store
) : TripProgressComponentContract {

    override val previewRoutes: Flow<List<NavigationRoute>> =
        store.slice(scope = scope, started = SharingStarted.Eagerly) {
            val routePreviewState = it.previewRoutes
            if (routePreviewState is RoutePreviewState.Ready) {
                routePreviewState.routes
            } else {
                emptyList()
            }
        }
}
