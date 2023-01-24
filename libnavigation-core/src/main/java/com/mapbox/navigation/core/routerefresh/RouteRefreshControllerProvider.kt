package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.PrimaryRouteProgressDataProvider
import com.mapbox.navigation.core.RoutesProgressDataProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.routealternatives.AlternativeMetadataProvider
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import java.util.Date

internal object RouteRefreshControllerProvider {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun createRouteRefreshController(
        threadController: ThreadController,
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        primaryRouteProgressDataProvider: PrimaryRouteProgressDataProvider,
        alternativeMetadataProvider: AlternativeMetadataProvider,
        evDynamicDataHolder: EVDynamicDataHolder,
    ): RouteRefreshControllerImpl {
        val scope = threadController.getMainScopeAndRootJob().scope
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
            scope,
            routeRefreshOptions.intervalMillis
        )
        val stateHolder = RouteRefreshStateHolder()
        val refreshObserversManager = RefreshObserversManager()
        val routeRefresherResultProcessor = RouteRefresherResultProcessor(
            refreshObserversManager,
            ExpiringDataRemover { Date() },
            Time.SystemImpl,
            routeRefreshOptions.intervalMillis * (PlannedRouteRefreshController.MAX_RETRY_COUNT + 1)
        )

        val plannedRouteRefreshController = PlannedRouteRefreshController(
            routeRefresherExecutor,
            routeRefreshOptions,
            stateHolder,
            scope,
            routeRefresherResultProcessor
        )
        val immediateRouteRefreshController = ImmediateRouteRefreshController(
            routeRefresherExecutor,
            stateHolder,
            routeRefresherResultProcessor
        )
        return RouteRefreshControllerImpl(
            plannedRouteRefreshController,
            immediateRouteRefreshController,
            stateHolder,
            refreshObserversManager,
            routeRefresherResultProcessor
        )
    }
}
