package com.mapbox.navigation.navigator.internal

import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.CacheFactory
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigFactory
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.ProfileApplication
import com.mapbox.navigator.ProfilePlatform
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouterFactory
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterType
import com.mapbox.navigator.SettingsProfile
import com.mapbox.navigator.TilesConfig

/**
 * This class is expected to gain more responsibility as we define [customConfig].
 * The custom config can be exposed through the [DeviceProfile]
 */
object NavigatorLoader {

    internal fun createNavigator(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?,
        router: RouterInterface,
    ): NativeComponents {

        val config = ConfigFactory.build(
            settingsProfile(deviceProfile),
            navigatorConfig,
            customConfig(deviceProfile)
        )
        val historyRecorder = buildHistoryRecorder(historyDir, config)
        val cache = CacheFactory.build(tilesConfig, config, historyRecorder)
        val navigator = Navigator(
            config,
            cache,
            historyRecorder,
            router,
        )
        val graphAccessor = GraphAccessor(cache)
        val roadObjectMatcher = RoadObjectMatcher(cache)

        return NativeComponents(
            navigator,
            historyRecorder,
            graphAccessor,
            cache,
            roadObjectMatcher,
            router,
            navigator.routeAlternativesController
        )
    }

    fun createNativeRouterInterface(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyRecorder: HistoryRecorderHandle?,
    ): RouterInterface {

        val config = ConfigFactory.build(
            settingsProfile(deviceProfile),
            navigatorConfig,
            customConfig(deviceProfile)
        )

        val cache = CacheFactory.build(tilesConfig, config, historyRecorder)
        return RouterFactory.build(
            RouterType.HYBRID,
            cache,
            config,
            historyRecorder,
        )
    }

    private fun buildHistoryRecorder(
        historyDir: String?,
        config: ConfigHandle
    ): HistoryRecorderHandle? {
        return if (historyDir != null) {
            val historyRecorderHandle = HistoryRecorderHandle.build(historyDir, config)
            if (historyRecorderHandle == null) {
                logE(
                    "Could not create directory directory to write events",
                    "NavigatorLoader"
                )
            }
            historyRecorderHandle
        } else {
            null
        }
    }

    // TODO Remove after NN enable internal reroute by default
    private fun customConfig(deviceProfile: DeviceProfile): String =
        deviceProfile.customConfig.customConfigEnableNativeRerouteInterface()

    private fun settingsProfile(deviceProfile: DeviceProfile): SettingsProfile {
        return when (deviceProfile.deviceType) {
            DeviceType.HANDHELD -> {
                SettingsProfile(ProfileApplication.MOBILE, ProfilePlatform.ANDROID)
            }
            DeviceType.AUTOMOBILE -> {
                SettingsProfile(ProfileApplication.AUTO, ProfilePlatform.ANDROID)
            }
            else -> throw NotImplementedError("Unknown device profile")
        }
    }

    internal data class NativeComponents(
        val navigator: Navigator,
        val historyRecorderHandle: HistoryRecorderHandle?,
        val graphAccessor: GraphAccessor,
        val cache: CacheHandle,
        val roadObjectMatcher: RoadObjectMatcher,
        val router: RouterInterface,
        val routeAlternativesController: RouteAlternativesControllerInterface,
    )
}
