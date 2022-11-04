package com.mapbox.navigation.core.preview

import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RoutesData
import io.mockk.every
import io.mockk.mockk

internal class RouteDataParserStub : RoutesDataParser {
    override suspend fun parse(routes: List<NavigationRoute>): RoutesData {
        return object : RoutesData {

            private var alternativeIdCounter = 1

            override fun primaryRoute() = routes.first().nativeRoute()

            override fun alternativeRoutes(): MutableList<RouteAlternative> =
                routes.drop(1).map { navigationRoute ->
                    val nextId = alternativeIdCounter++
                    mockk<RouteAlternative>(relaxed = true) {
                        every { id } returns nextId
                        every { route } returns navigationRoute.nativeRoute()
                    }
                }.toMutableList()
        }
    }
}
