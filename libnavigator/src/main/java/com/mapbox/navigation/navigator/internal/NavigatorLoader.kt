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

    fun createConfig(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
    ): ConfigHandle =
        ConfigFactory.build(
            settingsProfile(deviceProfile),
            navigatorConfig,
            deviceProfile.customConfig,
        )

    fun createHistoryRecorderHandles(
        config: ConfigHandle,
        historyDir: String?,
        copilotHistoryDir: String?,
    ): HistoryRecorderHandles {
        val general = buildHistoryRecorder(historyDir, config)
        val copilot = buildHistoryRecorder(copilotHistoryDir, config)
        val composite = buildCompositeHistoryRecorder(general, copilot)
        return HistoryRecorderHandles(general, copilot, composite)
    }

    internal fun createNavigator(
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        tilesConfig: TilesConfig,
        router: RouterInterface,
    ): NativeComponents {
        val cache = CacheFactory.build(tilesConfig, config, historyRecorderComposite)
        val navigator = Navigator(
            config,
            cache,
            historyRecorderComposite,
            null,
        )
        val graphAccessor = GraphAccessor(cache)
        val roadObjectMatcher = RoadObjectMatcher(cache)

        return NativeComponents(
            navigator,
            graphAccessor,
            cache,
            roadObjectMatcher,
            router,
            navigator.routeAlternativesController,
        )
    }

    fun createNativeRouterInterface(
        config: ConfigHandle,
        tilesConfig: TilesConfig,
        historyRecorder: HistoryRecorderHandle?,
    ): RouterInterface {
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

    private fun buildCompositeHistoryRecorder(
        historyRecorder: HistoryRecorderHandle?,
        copilotHistoryRecorder: HistoryRecorderHandle?,
    ): HistoryRecorderHandle? {
        val filteredHistoryRecorders = listOfNotNull(historyRecorder, copilotHistoryRecorder)
        return if (filteredHistoryRecorders.isNotEmpty()) {
            val compositeHistoryRecorderHandle =
                HistoryRecorderHandle.buildCompositeRecorder(filteredHistoryRecorders)
            if (compositeHistoryRecorderHandle == null) {
                logE(
                    "Could not create composite history recorder to write events",
                    "NavigatorLoader",
                )
            }
            compositeHistoryRecorderHandle
        } else {
            null
        }
    }

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

    data class HistoryRecorderHandles(
        val general: HistoryRecorderHandle?,
        val copilot: HistoryRecorderHandle?,
        val composite: HistoryRecorderHandle?,
    )

    internal data class NativeComponents(
        val navigator: Navigator,
        val graphAccessor: GraphAccessor,
        val cache: CacheHandle,
        val roadObjectMatcher: RoadObjectMatcher,
        val router: RouterInterface,
        val routeAlternativesController: RouteAlternativesControllerInterface,
    )
}
