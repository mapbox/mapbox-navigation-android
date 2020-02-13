package com.mapbox.navigation.core.tiles

import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.TileEndpointConfiguration

class NavigationRouterParamsProvider {

    fun provideRouterParams(tilePath: String, routerTilesParams: RouterTilesParams, accessToken: String): RouterParams {
        val endPointConfig = TileEndpointConfiguration(
                routerTilesParams.endpointHost,
                routerTilesParams.endpointVersion,
                accessToken,
                DEFAULT_USER_AGENT,
                "" // will be removed in the next nav-native release
        )

        return RouterParams(
                tilePath,
                null,
                null,
                2,
                endPointConfig
        )
    }

    companion object {
        const val DEFAULT_USER_AGENT = "MapboxNavigationNative"

        val OSM_2020_02_02 = RouterTilesParams(
                "https://api-routing-tiles-staging.tilestream.net",
                "2020_02_02-00_00_11"
        )

        val OSM_2019_04_13 = RouterTilesParams(
                "https://api-routing-tiles-staging.tilestream.net",
                "2019_04_13-00_00_11"
        )

        val ZENRIN_2020_01_14 = RouterTilesParams(
                "https://api-routing-tiles-zenrin-staging.tilestream.net",
                "2020_01_14-03_00_00"
        )
    }
}
