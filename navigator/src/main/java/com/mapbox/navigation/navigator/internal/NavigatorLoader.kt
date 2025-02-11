package com.mapbox.navigation.navigator.internal

import androidx.annotation.VisibleForTesting
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.base.BuildConfig
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.utils.internal.getOrPutJsonObject
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigator.AdasisFacadeHandle
import com.mapbox.navigator.BillingProductType
import com.mapbox.navigator.CacheFactory
import com.mapbox.navigator.CacheHandle
import com.mapbox.navigator.ConfigFactory
import com.mapbox.navigator.ConfigHandle
import com.mapbox.navigator.HistoryRecorderHandle
import com.mapbox.navigator.InputsServiceHandle
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.ProfileApplication
import com.mapbox.navigator.ProfilePlatform
import com.mapbox.navigator.RouterFactory
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterType
import com.mapbox.navigator.SdkHistoryInfo
import com.mapbox.navigator.SettingsProfile
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
        cacheHandle: CacheHandle,
        config: ConfigHandle,
        historyRecorderComposite: HistoryRecorderHandle?,
        offlineCacheHandle: CacheHandle?,
        inputsServiceHandle: InputsServiceHandle,
        adasisFacade: AdasisFacadeHandle,
    ): Navigator {
        return Navigator(
            config,
            cacheHandle,
            historyRecorderComposite,
            RouterType.HYBRID,
            inputsServiceHandle,
            adasisFacade,
            offlineCacheHandle,
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

    fun createInputService(
        config: ConfigHandle,
        historyRecorder: HistoryRecorderHandle?,
    ): InputsServiceHandle {
        return InputsServiceHandle.build(config, historyRecorder)
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
}
