package com.mapbox.navigation.ui.androidauto.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.car.app.CarContext
import androidx.core.content.edit

class MapboxCarStorage internal constructor(
    val carContext: CarContext,
) {
    private val sharedPreferences: SharedPreferences =
        carContext.getSharedPreferences(CAR_SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readSharedPref(key: String, defaultValue: Boolean): Boolean =
        sharedPreferences.getBoolean(key, defaultValue)

    fun writeSharedPref(key: String, value: Boolean) {
        sharedPreferences.edit {
            putBoolean(key, value)
        }
    }

    private companion object {
        private const val CAR_SETTINGS_PREFERENCES_NAME = "mapbox_car_settings"
    }
}
