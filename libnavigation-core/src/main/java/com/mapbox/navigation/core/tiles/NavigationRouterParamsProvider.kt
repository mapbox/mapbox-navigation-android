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
    }
}
