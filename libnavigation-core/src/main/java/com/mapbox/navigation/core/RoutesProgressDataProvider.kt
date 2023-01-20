package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.AlternativeMetadataProvider
import com.mapbox.navigation.core.routealternatives.AlternativeRouteProgressDataProvider

internal data class RoutesProgressData(
    val primaryRoute: NavigationRoute,
    val primaryRouteProgressData: RouteProgressData,
    val alternativeRoutesProgressData: List<Pair<NavigationRoute, RouteProgressData?>>
) {
    val allRoutesProgressData = listOf(primaryRoute to primaryRouteProgressData) +
        alternativeRoutesProgressData
}

internal class RoutesProgressDataProvider(
    private val primaryRouteProgressDataProvider: PrimaryRouteProgressDataProvider,
    private val alternativeMetadataProvider: AlternativeMetadataProvider,
) {

    /**
     * Retrieved progress data for passed routes.
     *
     * @throws IllegalArgumentException if routes re empty
     */
    @Throws(IllegalArgumentException::class)
    suspend fun getRoutesProgressData(
        routes: List<NavigationRoute>
    ): RoutesProgressData {
        if (routes.isEmpty()) {
            throw IllegalArgumentException("Routes must not be empty")
        }
        val primaryRouteProgressData = primaryRouteProgressDataProvider
            .getRouteRefreshRequestDataOrWait()
        val primary = routes.first()
        val alternatives = routes.drop(1)
        val alternativeRouteProgressDatas = alternatives.map { route ->
            val alternativeMetadata = alternativeMetadataProvider.getMetadataFor(route)
            val alternativeRouteProgressData = alternativeMetadata?.let {
                AlternativeRouteProgressDataProvider.getRouteProgressData(
                    primaryRouteProgressData,
                    it
                )
            }
            route to alternativeRouteProgressData
        }
        return RoutesProgressData(primary, primaryRouteProgressData, alternativeRouteProgressDatas)
    }
}
