package com.mapbox.androidauto.car.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.car.app.CarContext

private const val CAR_SETTINGS_PREFERENCES_NAME = "mapbox_car_settings"

class MapboxCarStorage internal constructor(
    val carContext: CarContext
) {
    private val sharedPreferences: SharedPreferences =
        carContext.getSharedPreferences(CAR_SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun readSharedPref(key: String, defaultValue: Boolean): Boolean =
        sharedPreferences.getBoolean(key, defaultValue)

    fun writeSharedPref(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }
}
