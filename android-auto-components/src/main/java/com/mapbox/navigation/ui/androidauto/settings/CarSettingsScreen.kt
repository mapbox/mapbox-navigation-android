package com.mapbox.navigation.ui.androidauto.settings

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.internal.setHeaderCompat

/**
 * Handle the android auto car app settings.
 *
 * The default app has no settings yet, so the single toggle below is a sample slot that shows
 * how a setting is stored. Legacy screens open this compatibility screen. On Car API 7 or newer,
 * the unified navigation screen embeds the same settings template alongside its map.
 * Configure [com.mapbox.navigation.ui.androidauto.MapboxCarOptions.Customization.settingsTemplateProvider]
 * to customize inline settings content.
 */
internal class CarSettingsScreen(
    private val mapboxCarContext: MapboxCarContext,
) : Screen(mapboxCarContext.carContext) {

    init {
        addBackPressedHandler {
            mapboxCarContext.mapboxScreenManager.goBack()
        }
    }

    override fun onGetTemplate(): Template = mapboxCarContext.buildCarSettingsTemplate()
}

/**
 * Builds a placeholder settings template with a sample toggle, not functional app settings.
 * Configure [com.mapbox.navigation.ui.androidauto.MapboxCarOptions.Customization.settingsTemplateProvider]
 * to display custom inline settings on the navigation screen.
 */
internal fun MapboxCarContext.buildCarSettingsTemplate(): ListTemplate {
    val label = carContext.getString(R.string.car_settings_toggle_place_holder)
    val key = carContext.getString(R.string.car_settings_toggle_place_holder_key)
    val row = Row.Builder()
        .setTitle(label)
        .setToggle(
            Toggle.Builder { value ->
                mapboxCarStorage.writeSharedPref(key, value)
            }
                .setChecked(mapboxCarStorage.readSharedPref(key, false))
                .build(),
        )
        .build()
    return ListTemplate.Builder()
        .setSingleList(ItemList.Builder().addItem(row).build())
        .setHeaderCompat(
            carContext,
            title = carContext.getString(R.string.car_settings_title),
            startAction = Action.BACK,
        )
        .build()
}
