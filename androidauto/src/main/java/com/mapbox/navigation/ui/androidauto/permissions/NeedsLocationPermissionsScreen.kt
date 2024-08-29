package com.mapbox.navigation.ui.androidauto.permissions

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import com.mapbox.navigation.ui.androidauto.R

/**
 * Provides instructions for accepting location permissions.
 */
internal class NeedsLocationPermissionsScreen(
    carContext: CarContext,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder(
            carContext.getString(R.string.car_message_location_permissions),
        ).setTitle(
            carContext.getString(R.string.car_message_location_permissions_title),
        ).addAction(
            Action.Builder()
                .setTitle(carContext.getString(R.string.car_label_ok))
                .setOnClickListener {
                    carContext.finishCarApp()
                }.build(),
        ).build()
    }
}
