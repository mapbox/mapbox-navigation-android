package com.mapbox.androidauto.car.settings

import com.mapbox.androidauto.car.MainCarContext

/**
 * Contains the dependencies for the settings screen.
 */
class SettingsCarContext(
    val mainCarContext: MainCarContext
) {
    val carContext = mainCarContext.carContext
    val carSettingsStorage = mainCarContext.carSettingsStorage
}
