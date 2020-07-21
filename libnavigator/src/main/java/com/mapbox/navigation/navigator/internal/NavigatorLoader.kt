package com.mapbox.navigation.navigator.internal

import com.mapbox.base.common.logger.Logger
import com.mapbox.common.HttpServiceFactory
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.navigator.NavigationOkHttpService
import com.mapbox.navigation.navigator.internal.NavigatorLoader.customConfig
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.ProfileApplication
import com.mapbox.navigator.ProfilePlatform
import com.mapbox.navigator.SettingsProfile
import okhttp3.OkHttpClient

/**
 * This class is expected to gain more responsibility as we define [customConfig].
 * The custom config can be exposed through the [DeviceProfile]
 */
internal object NavigatorLoader {

    init {
        System.loadLibrary("mapbox-common")
        System.loadLibrary("navigator-android")
    }

    fun createNavigator(deviceProfile: DeviceProfile, logger: Logger?): Navigator {
        HttpServiceFactory.setUserDefined(NavigationOkHttpService(OkHttpClient.Builder(), logger))
        return Navigator(
            settingsProfile(deviceProfile),
            customConfig(deviceProfile)
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
