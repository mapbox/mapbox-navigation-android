package com.mapbox.navigation.navigator.internal

import androidx.annotation.NonNull
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.navigator.internal.NavigatorLoader.customConfig
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.NavigatorConfig
import com.mapbox.navigator.ProfileApplication
import com.mapbox.navigator.ProfilePlatform
import com.mapbox.navigator.Router
import com.mapbox.navigator.SettingsProfile
import com.mapbox.navigator.TilesConfig

/**
 * This class is expected to gain more responsibility as we define [customConfig].
 * The custom config can be exposed through the [DeviceProfile]
 */
internal object NavigatorLoader {

    fun createRouter(
        return Router(CacheHandle() cache, @NonNull HistoryRecorderHandle historyRecorder)
    ): Router

    fun createNavigator(
        deviceProfile: DeviceProfile,
        navigatorConfig: NavigatorConfig,
        tilesConfig: TilesConfig
    ): Navigator {
        return Navigator(
            settingsProfile(deviceProfile),
            navigatorConfig,
            customConfig(deviceProfile),
            tilesConfig
        )
    }

    private fun settingsProfile(deviceProfile: DeviceProfile): SettingsProfile {
        return when (deviceProfile.deviceType) {
            DeviceType.HANDHELD -> {
                SettingsProfile(ProfileApplication.KMOBILE, ProfilePlatform.KANDROID)
            }
            DeviceType.AUTOMOBILE -> {
                SettingsProfile(ProfileApplication.KAUTO, ProfilePlatform.KANDROID)
            }
            else -> throw NotImplementedError("Unknown device profile")
        }
    }

    private fun customConfig(deviceProfile: DeviceProfile): String {
        return deviceProfile.customConfig
    }
}
