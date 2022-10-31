package com.mapbox.androidauto.freedrive

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.androidauto.R
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager

class FreeDriveActionStrip(
    private val screen: Screen
) {
    /**
     * Build an action strip for free drive
     */
    fun builder() = ActionStrip.Builder()
        .addAction(buildSettingsAction())
        .addAction(buildFreeDriveFeedbackAction())
        .addAction(buildSearchAction())
        .addAction(buildFavoritesAction())

    private fun buildFreeDriveFeedbackAction() = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(screen.carContext, R.drawable.mapbox_car_ic_feedback)
            ).build()
        )
        .setOnClickListener {
            MapboxScreenManager.push(MapboxScreen.FREE_DRIVE_FEEDBACK)
        }
        .build()

    private fun buildSettingsAction() = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    screen.carContext,
                    R.drawable.ic_settings
                )
            ).build()
        )
        .setOnClickListener {
            MapboxScreenManager.push(MapboxScreen.SETTINGS)
        }
        .build()

    private fun buildSearchAction() = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    screen.carContext,
                    R.drawable.ic_search_black36dp
                )
            ).build()
        )
        .setOnClickListener {
            MapboxScreenManager.push(MapboxScreen.SEARCH)
        }
        .build()

    private fun buildFavoritesAction() = Action.Builder()
        .setTitle(screen.carContext.resources.getString(R.string.car_action_search_favorites))
        .setOnClickListener {
            MapboxScreenManager.push(MapboxScreen.FAVORITES)
        }
        .build()
}
