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
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen

/**
 * Handle the android auto car app settings.
 *
 * The default app has no settings yet, so the single toggle below is a sample slot that shows
 * how a setting is stored. It is kept on purpose: the free drive action strip opens this screen,
 * and integrators replace it by registering their own factory for [MapboxScreen.SETTINGS].
 */
internal class CarSettingsScreen(
    private val mapboxCarContext: MapboxCarContext,
) : Screen(mapboxCarContext.carContext) {

    init {
        addBackPressedHandler {
            mapboxCarContext.mapboxScreenManager.goBack()
        }
    }

    override fun onGetTemplate(): Template {
        val templateBuilder = ListTemplate.Builder().setSingleList(
            ItemList.Builder()
                .addItem(
                    buildRowToggle(
                        R.string.car_settings_toggle_place_holder,
                        R.string.car_settings_toggle_place_holder_key,
                    ),
                )
                .build(),
        )
        return templateBuilder
            .setHeaderAction(Action.BACK)
            .setTitle(carContext.getString(R.string.car_settings_title))
            .build()
    }

    private fun buildRowToggle(labelResource: Int, prefKeyResource: Int): Row {
        val storage = mapboxCarContext.mapboxCarStorage
        val label = carContext.getString(labelResource)
        val key = carContext.getString(prefKeyResource)
        return Row.Builder()
            .setTitle(label)
            .setToggle(
                Toggle.Builder { value ->
                    mapboxCarContext.mapboxCarStorage.writeSharedPref(key, value)
                }
                    .setChecked(storage.readSharedPref(key, false))
                    .build(),
            )
            .build()
    }
}
