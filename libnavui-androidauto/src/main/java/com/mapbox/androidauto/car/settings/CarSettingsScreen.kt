package com.mapbox.androidauto.car.settings

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import com.mapbox.examples.androidauto.R

/**
 * Handle the android auto car app settings.
 */
class CarSettingsScreen(
    private val settingsCarContext: SettingsCarContext
) : Screen(settingsCarContext.carContext) {

    private val carSettingsStorage = settingsCarContext.carSettingsStorage

    override fun onGetTemplate(): Template {
        val templateBuilder = ListTemplate.Builder().setSingleList(
            ItemList.Builder()
                .addItem(
                    buildRowToggle(
                        R.string.car_settings_toggle_place_holder,
                        R.string.car_settings_toggle_place_holder_key
                    )
                )
                .build(),
        )
        return templateBuilder
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.car_settings_title))
            .build()
    }

    private fun buildRowToggle(labelResource: Int, prefKeyResource: Int): Row {
        val storage = settingsCarContext.carSettingsStorage
        val label = carContext.getString(labelResource)
        val key = carContext.getString(prefKeyResource)
        return Row.Builder()
            .setTitle(label)
            .setToggle(
                Toggle.Builder { value ->
                    carSettingsStorage.writeSharedPref(key, value)
                }
                    .setChecked(storage.readSharedPref(key, false))
                    .build()
            )
            .build()
    }
}
