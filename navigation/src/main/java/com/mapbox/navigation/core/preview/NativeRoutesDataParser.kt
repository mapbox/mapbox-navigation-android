package com.mapbox.navigation.core.preview

import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouteParser
import com.mapbox.navigator.RoutesData
import kotlinx.coroutines.withContext

internal class NativeRoutesDataParser : RoutesDataParser {
    override suspend fun parse(routes: List<NavigationRoute>): RoutesData =
        withContext(ThreadController.DefaultDispatcher) {
            RouteParser.createRoutesData(
                routes.first().nativeRoute(),
                routes.drop(1).map { it.nativeRoute() },
            )
        }
}
