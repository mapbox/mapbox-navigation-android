package com.mapbox.navigation.qa_test_app.utils

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UserNavigationRerouteController(
    private val context: Context,
) : NavigationRerouteController {

    private companion object {
        private const val DEFAULT_DELAY_MILLIS = 4000L
    }

    private val rerouteI: NavigationRoute by lazy {
        val routeAsString = Utils.readRawFileText(context, R.raw.warsaw_reroute_first)
        DirectionsRoute.fromJson(routeAsString).toNavigationRoute(RouterOrigin.Offboard)
    }
    private val rerouteII: NavigationRoute by lazy {
        val routeAsString = Utils.readRawFileText(context, R.raw.warsaw_reroute_second)
        DirectionsRoute.fromJson(routeAsString).toNavigationRoute(RouterOrigin.Offboard)
    }

    private var switcher: Boolean = false

    private val threadController = ThreadController()
    private var rerouteJob: Job? = null

    override fun reroute(callback: NavigationRerouteController.RoutesCallback) {
        rerouteJob?.cancel()
        rerouteJob = threadController.getIOScopeAndRootJob().scope.launch {
            delay(DEFAULT_DELAY_MILLIS)
            callback.onNewRoutes(listOf(takeNextRoute()), RouterOrigin.Offboard)
        }
    }

    override fun reroute(routesCallback: RerouteController.RoutesCallback) {
        reroute { routes, _ ->
            routesCallback.onNewRoutes(routes.toDirectionsRoutes())
        }
    }

    private fun takeNextRoute(): NavigationRoute {
        return if (switcher) {
            rerouteI
        } else {
            rerouteII
        }.also {
            switcher = !switcher
        }
    }

    override val state: RerouteState = RerouteState.Idle

    override fun interrupt() {
        rerouteJob?.let {
            it.cancel()
            rerouteJob = null
        }
    }

    override fun registerRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver
    ): Boolean {
        return false
    }

    override fun unregisterRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver
    ): Boolean {
        return false
    }
}
