package com.mapbox.androidauto.car.settings

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import com.mapbox.examples.androidauto.R

/**
 * Screen used to display that the settings did not load
 */
class SettingsFailureScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template = MessageTemplate
        .Builder(carContext.getString(R.string.car_settings_preload_failure_message))
        .setTitle(carContext.getString(R.string.car_settings_preload_failure_title))
        .build()
}
