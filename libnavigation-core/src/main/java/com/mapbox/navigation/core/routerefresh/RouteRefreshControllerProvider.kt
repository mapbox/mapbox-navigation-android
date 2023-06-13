package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.RoutesProgressDataProvider
import com.mapbox.navigation.core.RoutesRefreshDataProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.internal.utils.CoroutineUtils
import com.mapbox.navigation.utils.internal.Time
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import java.util.Date

internal object RouteRefreshControllerProvider {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun createRouteRefreshController(
        dispatcher: CoroutineDispatcher,
        immediateDispatcher: CoroutineDispatcher,
        routeRefreshOptions: RouteRefreshOptions,
        directionsSession: DirectionsSession,
        routesProgressDataProvider: RoutesProgressDataProvider,
        evDynamicDataHolder: EVDynamicDataHolder,
        timeProvider: Time
    ): RouteRefreshController {
        val routeRefresher = RouteRefresher(
            RoutesRefreshDataProvider(routesProgressDataProvider),
            EVRefreshDataProvider(evDynamicDataHolder),
            DirectionsRouteDiffProvider(),
            directionsSession,
        )
        val routeRefreshParentJob = SupervisorJob()
        val routeRefresherExecutor = RouteRefresherExecutor(
            routeRefresher,
            routeRefreshOptions.intervalMillis,
            CoroutineUtils.createScope(routeRefreshParentJob, dispatcher),
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
            CoroutineUtils.createScope(routeRefreshParentJob, immediateDispatcher),
            routeRefresherResultProcessor
        )
        val immediateRouteRefreshController = ImmediateRouteRefreshController(
            routeRefresherExecutor,
            stateHolder,
            CoroutineUtils.createScope(routeRefreshParentJob, dispatcher),
            routeRefresherResultProcessor
        )
        return RouteRefreshController(
            routeRefreshParentJob,
            plannedRouteRefreshController,
            immediateRouteRefreshController,
            stateHolder,
            refreshObserversManager,
            routeRefresherResultProcessor
        )
    }
}
