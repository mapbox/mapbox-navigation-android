package com.mapbox.navigation.navigator.internal

import androidx.annotation.VisibleForTesting
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.SdkInformation
import com.mapbox.common.TileDataDomain
import com.mapbox.navigation.base.BuildConfig
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.base.options.NavigationTileDataDomain
import com.mapbox.navigation.base.options.RoadObjectMatcherOptions
import com.mapbox.navigation.utils.internal.getOrPutJsonObject
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.AdasisFacadeBuilder
import com.mapbox.navigator.AdasisFacadeHandle
import com.mapbox.navigator.AdasisFacadeHandleInterface
import com.mapbox.navigator.BillingProductType
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigFactory
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.GraphAccessor
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.InputsServiceHandle
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.NavigatorHandle
import com.mapbox.navigator.NavigatorHandleBuilder
import com.mapbox.navigator.NavigatorInterface
import com.mapbox.navigator.NavigatorOperationsDelegate
import com.mapbox.navigator.OnVersionChangedCallback
import com.mapbox.navigator.ProfileApplication
import com.mapbox.navigator.ProfilePlatform
import com.mapbox.navigator.RoadObjectMatcher
import com.mapbox.navigator.RoadObjectMatcherConfig
import com.mapbox.navigator.RoadObjectsMatcherOptions
import com.mapbox.navigator.RouterFactory
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterType
import com.mapbox.navigator.SdkHistoryInfo
import com.mapbox.navigator.SettingsProfile
import com.mapbox.navigator.TilesConfig
import com.mapbox.navigator.TilesManagerHandle
import org.json.JSONException
import org.json.JSONObject
import java.io.Closeable

/**
 * This class is expected to gain more responsibility as we define [customConfig].
 * The custom config can be exposed through the [DeviceProfile]
 */
@OptIn(MapboxExperimental::class)
object NavigatorLoader {

    fun createConfig(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
    ): ConfigHandle {
        return ConfigFactory.build(
            settingsProfile(deviceProfile),
            navigatorConfig,
            deviceProfile.customConfig.enrichWithRequiredSdkFeatures(),
        )
    }

    fun createHistoryRecorderHandles(
        config: ConfigHandle,
        historyDir: String?,
        copilotHistoryDir: String?,
        sdkInformation: SdkInformation,
    ): HistoryRecorderHandles {
        val sdkHistoryInfo = SdkHistoryInfo(
            sdkInformation.version,
            sdkInformation.name,
        )
        val general = buildHistoryRecorder(historyDir, config, sdkHistoryInfo)
        val copilot = buildHistoryRecorder(copilotHistoryDir, config, sdkHistoryInfo)
        val composite = buildCompositeHistoryRecorder(general, copilot)
        return HistoryRecorderHandles(general, copilot, composite)
    }

    internal fun createNavigator(
        config: ConfigHandle,
        tilesManagerHandle: TilesManagerHandle,
        historyRecorder: HistoryRecorderHandle?,
        inputsServiceHandle: InputsServiceHandle,
        adasisFacade: AdasisFacadeHandleInterface,
    ): NavigatorInterface {
        return Navigator(
            config,
            tilesManagerHandle,
            historyRecorder,
            RouterType.HYBRID,
            inputsServiceHandle,
            // TODO(NN-4796) make Navigator accept interface types
            adasisFacade as AdasisFacadeHandle,
        )
    }

    fun createTilesManagerHandle(
        config: ConfigHandle,
        tilesConfig: TilesConfig,
        historyRecorder: HistoryRecorderHandle?,
    ): TilesManagerHandle {
        return TilesManagerHandle.build(
            tilesConfig,
            config,
            historyRecorder,
            BillingProductType.CF,
        )
    }

    /**
     * Sets [callback] as the tiles version change callback. Closing the result clears it, which
     * also releases the callback on the native side.
     *
     * A direct native call cannot be mocked, so it goes through here to keep unit tests JVM-only.
     */
    fun setOnVersionChangedCallback(
        tilesManagerHandle: TilesManagerHandle,
        callback: OnVersionChangedCallback,
    ): Closeable {
        tilesManagerHandle.setOnVersionChangedCallback(callback)
        return Closeable { tilesManagerHandle.setOnVersionChangedCallback(null) }
    }

    /**
     * Builds an empty [NavigatorHandle] and sets the [historyRecorder] on it.
     *
     * [NavigatorHandleBuilder.setHistoryRecorder] must be called before the navigator is set,
     * because setting the navigator emits the instance-changed signal and subscribers expect the
     * recorder to be already set.
     */
    fun createNavigatorHandle(historyRecorder: HistoryRecorderHandle?): NavigatorHandle =
        NavigatorHandleBuilder.build().also { handle ->
            historyRecorder?.let { NavigatorHandleBuilder.setHistoryRecorder(handle, it) }
        }

    /**
     * Sets the [TilesManagerHandle] and [Navigator] inside [handle]. Call on navigator
     * (re)creation, keeping the same [handle] instance.
     */
    fun updateNavigatorHandle(
        handle: NavigatorHandle,
        tilesManagerHandle: TilesManagerHandle,
        navigator: NavigatorInterface,
    ) {
        NavigatorHandleBuilder.setTilesManagerHandleAndNavigator(
            handle,
            tilesManagerHandle,
            navigator as Navigator,
        )
    }

    /**
     * Sets the [NavigatorOperationsDelegate] on [handle].
     */
    fun setOperationsDelegate(handle: NavigatorHandle, delegate: NavigatorOperationsDelegate) {
        NavigatorHandleBuilder.setOperationsDelegate(handle, delegate)
    }

    fun createRoadObjectMatcherConfig(
        roadObjectMatcherOptions: RoadObjectMatcherOptions,
    ): RoadObjectMatcherConfig {
        return RoadObjectMatcherConfig(
            roadObjectMatcherOptions.openLRMaxDistanceToNode,
            mapRoadObjectMatcherType(roadObjectMatcherOptions.matchingGraphType),
        )
    }

    fun createRoadObjectMatcherOptions(
        roadObjectMatcherOptions: RoadObjectMatcherOptions,
    ): RoadObjectsMatcherOptions {
        return RoadObjectsMatcherOptions(
            roadObjectMatcherOptions.openLRMaxDistanceToNode,
            mapRoadObjectMatcherType(roadObjectMatcherOptions.matchingGraphType),
        )
    }

    private fun mapRoadObjectMatcherType(
        matchingGraphType: NavigationTileDataDomain,
    ): TileDataDomain = when (matchingGraphType) {
        NavigationTileDataDomain.NAVIGATION -> TileDataDomain.NAVIGATION
        NavigationTileDataDomain.NAVIGATION_HD -> TileDataDomain.NAVIGATION_HD
        NavigationTileDataDomain.MAPS -> TileDataDomain.MAPS
        NavigationTileDataDomain.SEARCH -> TileDataDomain.SEARCH
        NavigationTileDataDomain.ADAS -> TileDataDomain.ADAS
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

    fun createInputService(
        config: ConfigHandle,
        historyRecorder: HistoryRecorderHandle?,
    ): InputsServiceHandle {
        return InputsServiceHandle.build(config, historyRecorder)
    }

    internal fun createAdasisFacade(
        config: ConfigHandle,
        tilesManagerHandle: TilesManagerHandle,
        historyRecorder: HistoryRecorderHandle?,
    ): AdasisFacadeHandleInterface {
        return AdasisFacadeBuilder.build(config, tilesManagerHandle, historyRecorder)
    }

    internal fun createGraphAccessor(tilesManagerHandle: TilesManagerHandle): GraphAccessor {
        return GraphAccessor(tilesManagerHandle)
    }

    internal fun createRoadObjectMatcher(
        tilesManagerHandle: TilesManagerHandle,
        config: RoadObjectMatcherConfig,
    ): RoadObjectMatcher {
        return RoadObjectMatcher(tilesManagerHandle, config)
    }

    private fun buildHistoryRecorder(
        historyDir: String?,
        config: ConfigHandle,
        sdkHistoryInfo: SdkHistoryInfo,
    ): HistoryRecorderHandle? {
        return if (historyDir != null) {
            val historyRecorderHandle = HistoryRecorderHandle.build(
                historyDir,
                sdkHistoryInfo,
                config,
            )
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
        }
    }

    /**
     * The integrator's custom config with the native features the SDK always requires on top.
     */
    @VisibleForTesting
    internal fun String.enrichWithRequiredSdkFeatures(
        sendImmediate: Boolean = BuildConfig.DEBUG,
    ): String {
        val rootJson = parseCustomConfig(this)
        rootJson.enableTelemetryNavigationEvents(sendImmediate)
        rootJson.enableTilesVersionSwap()
        return rootJson.toString()
    }

    private fun parseCustomConfig(config: String): JSONObject {
        if (config.isBlank()) {
            return JSONObject()
        }
        return try {
            JSONObject(config)
        } catch (e: JSONException) {
            logE("Custom config is not valid: $e, $config")
            JSONObject()
        }
    }

    private fun JSONObject.enableTelemetryNavigationEvents(sendImmediate: Boolean) {
        getOrPutJsonObject("features").put("useTelemetryNavigationEvents", true)
        if (sendImmediate) {
            getOrPutJsonObject("telemetry").put("eventsPriority", "Immediate")
        }
    }

    // tiles versions are switched inside the native navigator instead of recreating it
    private fun JSONObject.enableTilesVersionSwap() {
        getOrPutJsonObject("features").put("allowTilesCacheReset", true)
    }

    data class HistoryRecorderHandles(
        val general: HistoryRecorderHandle?,
        val copilot: HistoryRecorderHandle?,
        val composite: HistoryRecorderHandle?,
    )
}
