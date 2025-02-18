package com.mapbox.navigation.mapgpt.core.common

import com.mapbox.common.SettingsServiceFactory
import com.mapbox.common.SettingsServiceStorageType

object PlatformSettingsFactory {
    fun createPersistentSettings(): PlatformSettings {
        val settingsServiceInterface = SettingsServiceFactory
            .getInstance(SettingsServiceStorageType.PERSISTENT)
        return PlatformSettings(settingsServiceInterface)
    }

    fun createNonPersistentSettings(): PlatformSettings {
        val settingsServiceInterface = SettingsServiceFactory
            .getInstance(SettingsServiceStorageType.NON_PERSISTENT)
        return PlatformSettings(settingsServiceInterface)
    }
}
