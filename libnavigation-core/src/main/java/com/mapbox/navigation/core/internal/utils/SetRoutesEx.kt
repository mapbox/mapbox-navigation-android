package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesExtra

@RoutesExtra.RoutesUpdateReason
internal fun SetRoutes.mapToReason(): String =
    when (this) {
        is SetRoutes.Alternatives -> RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE
        SetRoutes.CleanUp -> RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
        is SetRoutes.NewRoutes -> RoutesExtra.ROUTES_UPDATE_REASON_NEW
        is SetRoutes.RefreshRoutes -> RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
        is SetRoutes.Reroute -> RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
    }

internal fun SetRoutes.initialLegIndex(): Int =
    when (this) {
        is SetRoutes.Alternatives -> legIndex
        SetRoutes.CleanUp -> MapboxDirectionsSession.DEFAULT_INITIAL_LEG_INDEX
        is SetRoutes.NewRoutes -> legIndex
        is SetRoutes.RefreshRoutes -> routeProgressData.legIndex
        is SetRoutes.Reroute -> legIndex
    }
