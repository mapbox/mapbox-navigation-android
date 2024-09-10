package com.mapbox.navigation.navigator.internal

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.base.BuildConfig
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.utils.internal.getOrPutJsonObject
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.BillingProductType
import com.mapbox.navigator.CacheFactory
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigFactory
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.EventsMetadataInterface
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.InputsServiceHandle
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
import com.mapbox.navigator.Telemetry
import com.mapbox.navigator.TilesConfig
import org.json.JSONException
import org.json.JSONObject

/**
 * This class is expected to gain more responsibility as we define [customConfig].
 * The custom config can be exposed through the [DeviceProfile]
 */
object NavigatorLoader {

    fun createConfig(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
    ): ConfigHandle {
        return ConfigFactory.build(
            settingsProfile(deviceProfile),
            navigatorConfig,
            enableTelemetryNavigationEvents(deviceProfile.customConfig),
        )
    }

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
        cacheHandle: CacheHandle,
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        offlineCacheHandle: CacheHandle?,
        eventsMetadataProvider: EventsMetadataInterface,
    ): NativeComponents {
        val navigator = Navigator(
            config,
            cacheHandle,
            historyRecorderComposite,
            RouterType.HYBRID,
            null,
            offlineCacheHandle,
        )
        val graphAccessor = GraphAccessor(cacheHandle)
        val roadObjectMatcher = RoadObjectMatcher(cacheHandle)

        return NativeComponents(
            navigator,
            graphAccessor,
            cacheHandle,
            roadObjectMatcher,
            navigator.routeAlternativesController,
            createInputService(config, historyRecorderComposite),
            navigator.getTelemetry(eventsMetadataProvider),
        )
    }

    fun createCacheHandle(
        config: ConfigHandle,
        tilesConfig: TilesConfig,
        historyRecorder: HistoryRecorderHandle?,
    ): CacheHandle {
        return CacheFactory.build(tilesConfig, config, historyRecorder, BillingProductType.CF)
    }

    fun createNativeRouterInterface(
        cacheHandle: CacheHandle,
        config: ConfigHandle,
        historyRecorder: HistoryRecorderHandle?,
    ): RouterInterface {
        return RouterFactory.build(
            RouterType.HYBRID,
            cacheHandle,
            config,
            historyRecorder,
        )
    }

    private fun createInputService(
        config: ConfigHandle,
        historyRecorder: HistoryRecorderHandle?,
    ): InputsServiceHandle {
        return InputsServiceHandle.build(config, historyRecorder)
    }

    private fun buildHistoryRecorder(
        historyDir: String?,
        config: ConfigHandle,
    ): HistoryRecorderHandle? {
        return if (historyDir != null) {
            val historyRecorderHandle = HistoryRecorderHandle.build(historyDir, config)
            if (historyRecorderHandle == null) {
                logE(
                    "Could not create directory directory to write events",
                    "NavigatorLoader",
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

    // TODO should be enabled by default in NN when platforms migrate to native telemetry
    @VisibleForTesting
    internal fun enableTelemetryNavigationEvents(
        config: String,
        sendImmediate: Boolean = BuildConfig.DEBUG,
    ): String {
        val rootJson = if (config.isNotBlank()) {
            try {
                JSONObject(config)
            } catch (e: JSONException) {
                logE("Custom config is not valid: $e, $config")
                JSONObject()
            }
        } else {
            JSONObject()
        }

        rootJson.getOrPutJsonObject("features")
            .put("useTelemetryNavigationEvents", true)

        if (sendImmediate) {
            rootJson.getOrPutJsonObject("telemetry")
                .put("eventsPriority", "Immediate")
        }
        return rootJson.toString()
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
        val routeAlternativesController: RouteAlternativesControllerInterface,
        val inputsService: InputsServiceHandle,
        val telemetry: Telemetry,
    )
}
