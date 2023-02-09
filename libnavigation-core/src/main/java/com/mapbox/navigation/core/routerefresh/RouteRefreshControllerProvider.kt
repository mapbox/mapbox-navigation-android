package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.PrimaryRouteProgressDataProvider
import com.mapbox.navigation.core.RoutesProgressDataProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.routealternatives.AlternativeMetadataProvider
import com.mapbox.navigation.utils.internal.Time
import kotlinx.coroutines.CoroutineScope
import java.util.Date

internal object RouteRefreshControllerProvider {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun createRouteRefreshController(
        scope: CoroutineScope,
        immediateScope: CoroutineScope,
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        primaryRouteProgressDataProvider: PrimaryRouteProgressDataProvider,
        alternativeMetadataProvider: AlternativeMetadataProvider,
        evDynamicDataHolder: EVDynamicDataHolder,
        timeProvider: Time
    ): RouteRefreshController {
        val routeRefresher = RouteRefresher(
            RoutesProgressDataProvider(
                primaryRouteProgressDataProvider,
                alternativeMetadataProvider
            ),
            EVRefreshDataProvider(evDynamicDataHolder),
            DirectionsRouteDiffProvider(),
            directionsSession,
        )
        val routeRefresherExecutor = RouteRefresherExecutor(
            routeRefresher,
            routeRefreshOptions.intervalMillis
        )
        val stateHolder = RouteRefreshStateHolder()
        val refreshObserversManager = RefreshObserversManager()
        val routeRefresherResultProcessor = RouteRefresherResultProcessor(
            stateHolder,
            refreshObserversManager,
            ExpiringDataRemover { Date() },
            timeProvider,
            routeRefreshOptions.intervalMillis * 3
        )

        val plannedRouteRefreshController = PlannedRouteRefreshController(
            routeRefresherExecutor,
            routeRefreshOptions,
            stateHolder,
            immediateScope,
            routeRefresherResultProcessor
        )
        val immediateRouteRefreshController = ImmediateRouteRefreshController(
            routeRefresherExecutor,
            stateHolder,
            scope,
            routeRefresherResultProcessor
        )
        return RouteRefreshController(
            plannedRouteRefreshController,
            immediateRouteRefreshController,
            stateHolder,
            refreshObserversManager,
            routeRefresherResultProcessor
        )
    }
}
