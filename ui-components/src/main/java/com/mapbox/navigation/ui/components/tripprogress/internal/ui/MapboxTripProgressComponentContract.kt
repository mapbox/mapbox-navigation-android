package com.mapbox.navigation.ui.components.tripprogress.internal.ui

import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MapboxTripProgressComponentContract(
    override val previewRoutes: Flow<List<NavigationRoute>> = flowOf(emptyList()),
) : TripProgressComponentContract
