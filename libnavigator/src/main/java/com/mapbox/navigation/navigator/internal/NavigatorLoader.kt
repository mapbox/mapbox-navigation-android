package com.mapbox.navigation.navigator.internal

import com.mapbox.navigation.base.options.AutomobileProfile
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.HandheldProfile
import com.mapbox.navigator.Navigator
import com.mapbox.navigator.ProfileApplication
import com.mapbox.navigator.ProfilePlatform
import com.mapbox.navigator.SettingsProfile

/**
 * This class is expected to gain more responsibility as we define [customConfig].
 * The custom config can be exposed through the [DeviceProfile]
 */
internal object NavigatorLoader {

    init {
        System.loadLibrary("navigator-android")
    }

    fun createNavigator(deviceProfile: DeviceProfile): Navigator {
        return Navigator(
            settingsProfile(deviceProfile),
            customConfig(deviceProfile)
        )
    }

    private fun settingsProfile(deviceProfile: DeviceProfile): SettingsProfile {
        return when (deviceProfile) {
            is AutomobileProfile -> {
                SettingsProfile(ProfileApplication.KAUTO, ProfilePlatform.KANDROID)
            }
            is HandheldProfile -> {
                SettingsProfile(ProfileApplication.KMOBILE, ProfilePlatform.KANDROID)
            }
            else -> throw NotImplementedError("Unknown device profile")
        }
    }

    private fun customConfig(deviceProfile: DeviceProfile): String {
        return deviceProfile.customConfig
    }
}
