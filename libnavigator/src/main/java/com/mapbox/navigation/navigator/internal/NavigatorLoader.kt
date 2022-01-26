package com.mapbox.navigation.navigator.internal

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.navigator.internal.NavigatorLoader.customConfig
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
internal object NavigatorLoader {

    fun createNavigator(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig,
        historyDir: String?
    ): NativeComponents {
        val config = ConfigFactory.build(
            settingsProfile(deviceProfile),
            navigatorConfig,
            customConfig(deviceProfile)
        )
        val historyRecorder = buildHistoryRecorder(historyDir, config)
        val cache = CacheFactory.build(tilesConfig, config, historyRecorder)
        val router = RouterFactory.build(
            RouterType.HYBRID,
            cache,
            historyRecorder
        )
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

    private fun buildHistoryRecorder(
        historyDir: String?,
        config: ConfigHandle
    ): HistoryRecorderHandle? {
        return if (historyDir != null) {
            val historyRecorderHandle = HistoryRecorderHandle.build(historyDir, config)
            if (historyRecorderHandle == null) {
                logE(
                    Tag("MbxHistoryRecorder"),
                    Message("Could not create directory directory to write events")
                )
            }
            historyRecorderHandle
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

    // TODO Remove after NN disables it by default
    private fun customConfig(deviceProfile: DeviceProfile): String {
        val useImuJson = """
            {
                "input": {
            	    "stopDetector": {
            		    "mobile": {
            			    "useImu": false
            		    }
            	    }
                }
            }
        """.trimIndent()
        val customConfig = deviceProfile.customConfig
        return if (customConfig.isEmpty()) {
            useImuJson
        } else {
            disableImu(customConfig)
        }
    }

    private fun disableImu(customConfig: String): String {
        val gson = GsonBuilder().create()
        val jsonObject = gson.fromJson(customConfig, JsonObject::class.java)
        val useImu = gson.fromJson(
            """{"useImu": false}""".trimIndent(),
            JsonElement::class.java
        )
        val mobile = gson.fromJson(
            """{"mobile": {"useImu": false}}""".trimIndent(),
            JsonElement::class.java
        )
        val stopDetector = gson.fromJson(
            """{"stopDetector": {"mobile": {"useImu": false}}}""".trimIndent(),
            JsonElement::class.java
        )
        when {
            jsonObject?.getAsJsonObject("input") == null -> {
                jsonObject.add("input", stopDetector)
            }
            jsonObject.getAsJsonObject("input").getAsJsonObject("stopDetector") == null -> {
                jsonObject.getAsJsonObject("input").add("stopDetector", mobile)
            }
            jsonObject.getAsJsonObject("input").getAsJsonObject("stopDetector")
                .getAsJsonObject("mobile") == null -> {
                jsonObject.getAsJsonObject("input").getAsJsonObject("stopDetector")
                    .add("mobile", useImu)
            }
            else -> {
                jsonObject.getAsJsonObject("input")?.getAsJsonObject("stopDetector")
                    ?.getAsJsonObject("mobile")?.addProperty("useImu", false)
            }
        }
        return jsonObject.toString()
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
