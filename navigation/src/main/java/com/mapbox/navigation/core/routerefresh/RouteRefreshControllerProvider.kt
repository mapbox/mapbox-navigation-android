package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.RoutesProgressDataProvider
import com.mapbox.navigation.core.RoutesRefreshDataProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.ev.EVDynamicDataHolder
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.internal.utils.CoroutineUtils
import com.mapbox.navigation.core.utils.routeRefresh.RouteRefreshUtils
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
        timeProvider: Time,
        historyRecorder: RouteRefreshHistoryRecorder,
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
        val stateHolder = RouteRefreshStateHolder(historyRecorder)
        val refreshObserversManager = RefreshObserversManager()
        val routeRefresherResultProcessor = RouteRefresherResultProcessor(
            stateHolder,
            refreshObserversManager,
            ExpiringDataRemover { Date() },
            timeProvider,
            routeRefreshOptions.intervalMillis * 3,
        )
        val routeRefreshResultAttemptProcessor = RoutesRefreshAttemptProcessor(
            refreshObserversManager,
        )

        val routeRefreshUtils = RouteRefreshUtils()
        val currentPrimaryRouteIdProvider = { directionsSession.routes.firstOrNull()?.id }
        val plannedRouteRefreshController = PlannedRouteRefreshController(
            routeRefresherExecutor,
            routeRefreshOptions,
            stateHolder,
            CoroutineUtils.createScope(routeRefreshParentJob, immediateDispatcher),
            routeRefresherResultProcessor,
            routeRefreshResultAttemptProcessor,
            timeProvider,
            routeRefreshUtils,
            currentPrimaryRouteIdProvider,
            historyRecorder,
        )
        val immediateRouteRefreshController = ImmediateRouteRefreshController(
            routeRefresherExecutor,
            stateHolder,
            CoroutineUtils.createScope(routeRefreshParentJob, dispatcher),
            routeRefresherResultProcessor,
            routeRefreshResultAttemptProcessor,
            routeRefreshUtils,
            currentPrimaryRouteIdProvider,
        )
        return RouteRefreshController(
            routeRefreshParentJob,
            plannedRouteRefreshController,
            immediateRouteRefreshController,
            stateHolder,
            refreshObserversManager,
            routeRefresherResultProcessor,
            historyRecorder,
        )
    }
}
