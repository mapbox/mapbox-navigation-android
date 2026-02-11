package com.mapbox.navigation.navigator.internal

import androidx.annotation.RawRes
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.internal.route.testing.createNavigationRouteForTest
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.navigator.internal.util.readRawFileText
import com.mapbox.navigation.navigator.internal.util.runOnMainSync
import com.mapbox.navigator.BillingProductType
import com.mapbox.navigator.CacheFactory
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigFactory
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.ProfileApplication
import com.mapbox.navigator.ProfilePlatform
import com.mapbox.navigator.RerouteStrategyForMatchRoute
import com.mapbox.navigator.SetRoutesParams
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.SettingsProfile
import com.mapbox.navigator.TilesConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch

class NavigatorTest {

    private companion object {
        @RawRes
        private val DIRECTIONS_RESPONSE =
            com.mapbox.navigation.navigator.test.R.raw.directions_response_two_leg_route
    }

    @Test(timeout = 5_000)
    fun successfulSetRoutesToNN() {
        val cdl = CountDownLatch(1)
        runOnMainSync {
            val navigator = provideNavigator()
            val routes = provideDirectionsRouteAndRouteOptions()
            navigator.setRoutes(
                SetRoutesParams(
                    routes.map { it.nativeRoute() }.first(),
                    0,
                    routes.map { it.nativeRoute() }.drop(1),
                ),
                SetRoutesReason.NEW_ROUTE,
            ) { expected ->
                assertTrue(expected.isValue)
                cdl.countDown()
            }
        }
        cdl.await()
    }

    private fun provideDirectionsRouteAndRouteOptions(): List<NavigationRoute> =
        createNavigationRouteForTest(
            DirectionsResponse.fromJson(
                readRawFileText(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    DIRECTIONS_RESPONSE,
                ),
            ),
            RouteOptions.builder()
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(-77.063888, 38.798979),
                        Point.fromLngLat(-77.078234, 38.894377),
                        Point.fromLngLat(-77.028263, 38.962309),
                    ),
                )
                .waypointNamesList(
                    listOf(
                        "",
                        "North Quinn Street",
                        "",
                    ),
                )
                .build(),
            RouterOrigin.ONLINE,
        )

    private fun provideNavigator(
        config: ConfigHandle = providesConfigHandle(),
    ): Navigator {
        return Navigator(
            config,
            provideCacheHandle(configHandle = config),
            null,
        )
    }

    private fun providesConfigHandle(): ConfigHandle =
        ConfigFactory.build(
            SettingsProfile(ProfileApplication.MOBILE, ProfilePlatform.ANDROID),
            NavigatorConfig(
                null,
                null,
                null,
                null,
                null,
                null,
                RerouteStrategyForMatchRoute.REROUTE_DISABLED,
                null,
            ),
            "",
        )

    private fun provideCacheHandle(
        tilesConfig: TilesConfig = provideTilesConfig(),
        configHandle: ConfigHandle,
    ): CacheHandle =
        CacheFactory.build(tilesConfig, configHandle, null, BillingProductType.CF)

    private fun provideTilesConfig(): TilesConfig = TilesConfig(
        InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null)!!.absolutePath,
        null,
        null,
        null,
        null,
        null,
    )
}
